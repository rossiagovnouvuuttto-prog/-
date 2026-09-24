/* Multi AI Chat — web-provider backend shim (ChatGPT + DeepSeek, no API keys). */
'use strict';

(() => {
  const realFetch = window.fetch.bind(window);
  let catalogue = null;

  const json = (body, status = 200) => new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json; charset=utf-8' },
  });
  const sse = (event) => `data: ${JSON.stringify(event)}\n\n`;

  async function loadCatalogue() {
    if (catalogue) return catalogue;
    const url = chrome.runtime.getURL('models.json');
    catalogue = await (await realFetch(url)).json();
    return catalogue;
  }

  function parsePayload(payload) {
    if (!payload || typeof payload !== 'object') return { error: 'Некорректный запрос.' };
    if (!['chatgpt-web', 'deepseek-web'].includes(payload.model)) return { error: 'Неизвестная веб-модель.' };
    if (!Array.isArray(payload.messages) || !payload.messages.length) return { error: 'История чата пуста.' };
    return {
      model: payload.model,
      messages: payload.messages
        .filter((m) => m && ['system', 'user', 'assistant'].includes(m.role) && typeof m.content === 'string')
        .map((m) => ({ role: m.role, content: m.content })),
      stream: payload.stream !== false,
    };
  }

  function streamChat(req, signal) {
    const encoder = new TextEncoder();
    let port = null;
    let finished = false;
    const stream = new ReadableStream({
      start(controller) {
        const send = (event) => controller.enqueue(encoder.encode(sse(event)));
        send({ type: 'start', model: req.model });
        port = chrome.runtime.connect({ name: 'webai-chat' });

        const abort = () => {
          try { port.disconnect(); } catch {}
          try { controller.close(); } catch {}
        };
        if (signal) {
          if (signal.aborted) return abort();
          signal.addEventListener('abort', abort, { once: true });
        }

        port.onMessage.addListener((msg) => {
          if (!msg) return;
          if (msg.type === 'done') {
            const text = typeof msg.text === 'string' ? msg.text : '';
            if (!text.trim()) {
              send({ type: 'error', code: 'empty_provider_response', message: 'Веб-модель вернула пустой ответ. Повторите запрос.' });
            } else {
              send({ type: 'delta', content: text });
              send({ type: 'done' });
            }
            finished = true;
            try { controller.close(); } catch {}
            try { port.disconnect(); } catch {}
          } else if (msg.type === 'error') {
            finished = true;
            send({ type: 'error', code: msg.code || 'provider_error', message: msg.message || 'Ошибка веб-нейросети.' });
            try { controller.close(); } catch {}
            try { port.disconnect(); } catch {}
          }
        });
        port.onDisconnect.addListener(() => {
          if (finished) return;
          finished = true;
          try {
            send({ type: 'error', code: 'extension_error', message: 'Связь с фоновой вкладкой прервалась. Попробуйте ещё раз.' });
            controller.close();
          } catch {}
        });
        port.postMessage({ type: 'chat', model: req.model, messages: req.messages });
      },
      cancel() {
        try { port?.disconnect(); } catch {}
      },
    });
    return new Response(stream, {
      headers: { 'Content-Type': 'text/event-stream; charset=utf-8', 'Cache-Control': 'no-cache' },
    });
  }

  window.fetch = async (input, init = {}) => {
    const href = typeof input === 'string' ? input : input.url;
    let url;
    try { url = new URL(href, location.href); } catch { return realFetch(input, init); }
    if (!url.pathname.startsWith('/api/')) return realFetch(input, init);

    if (url.pathname === '/api/health') {
      return json({
        ok: true,
        token_configured: true,
        mode: 'web',
        hint: 'API-ключ не нужен. Войдите на chatgpt.com и chat.deepseek.com в этом браузере.',
      });
    }
    if (url.pathname === '/api/models') return json(await loadCatalogue());
    if (url.pathname === '/api/featured') {
      const cat = await loadCatalogue();
      return json({ featured: (cat.featured || []).map((f) => ({
        key: f.key,
        name: f.name,
        icon: f.icon,
        desc: f.desc,
        type: f.type || 'normal',
        id: (f.candidates || [])[0] || '',
        status: 'online',
      })) });
    }
    if (url.pathname === '/api/chat') {
      let payload;
      try { payload = JSON.parse(init.body || '{}'); }
      catch { return json({ error: { code: 'bad_request', message: 'Некорректный JSON.' } }, 400); }
      const parsed = parsePayload(payload);
      if (parsed.error) return json({ error: { code: 'bad_request', message: parsed.error } }, 400);
      return streamChat(parsed, init.signal);
    }
    return new Response('Not found', { status: 404 });
  };
})();
