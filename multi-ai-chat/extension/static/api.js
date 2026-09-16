/* ==============================================================
   Multi AI Chat - in-extension backend.

   The hosted site talks to a server at /api/*. Inside the extension
   there is no server, so this file intercepts those same paths and
   answers them locally, talking to Hugging Face directly.

   app.js is byte-identical to the hosted build: it still calls
   fetch('/api/chat') and never sees the token. The token lives in
   the browser's extension storage and is only ever sent to
   huggingface.co in an Authorization header.

   Loaded BEFORE app.js so the shim is in place on first call.
   ============================================================== */
'use strict';

(() => {
  const realFetch = window.fetch.bind(window);

  const HF_DEFAULT_BASE = 'https://router.huggingface.co/v1';
  const MODEL_ID_RE = /^[A-Za-z0-9._-]+\/[A-Za-z0-9._-]+(:[A-Za-z0-9._-]+)?$/;
  const MAX_MESSAGES = 120;
  const MAX_CHARS = 120000;
  const FEATURED_TTL_MS = 300000;

  const MODEL_UNAVAILABLE = 'Модель сейчас недоступна через Hugging Face Inference.';
  const NO_TOKEN_HINT = 'Откройте «Настройки» слева и вставьте туда токен Hugging Face.';

  let featuredCache = { at: 0, data: null };
  let catalogue = null;

  /* ---------- storage: extension when available, localStorage otherwise ---------- */
  const hasChrome = typeof chrome !== 'undefined' && chrome.storage && chrome.storage.local;

  const store = {
    async get(keys) {
      if (hasChrome) return chrome.storage.local.get(keys);
      const out = {};
      for (const k of keys) {
        const v = localStorage.getItem('mac.ext.' + k);
        if (v !== null) out[k] = v;
      }
      return out;
    },
    async set(obj) {
      if (hasChrome) return chrome.storage.local.set(obj);
      for (const [k, v] of Object.entries(obj)) localStorage.setItem('mac.ext.' + k, v);
    },
  };

  async function settings() {
    const got = await store.get(['hfToken', 'hfBaseUrl']);
    return {
      token: (got.hfToken || '').trim(),
      baseUrl: (got.hfBaseUrl || HF_DEFAULT_BASE).replace(/\/+$/, ''),
      timeout: 120000,
    };
  }

  /* ---------- helpers ---------- */
  const json = (body, status = 200) =>
    new Response(JSON.stringify(body), {
      status,
      headers: { 'Content-Type': 'application/json; charset=utf-8' },
    });

  const sse = (event) => `data: ${JSON.stringify(event)}\n\n`;

  function extractHfMessage(body) {
    try {
      const data = JSON.parse(body);
      if (data && typeof data === 'object') {
        const err = data.error;
        if (err && typeof err === 'object') return String(err.message || '').slice(0, 200);
        if (typeof err === 'string') return err.slice(0, 200);
        if (data.message) return String(data.message).slice(0, 200);
      }
    } catch { /* fall through */ }
    return String(body).trim().slice(0, 200);
  }

  function classifyHttpError(status, body) {
    const low = (body || '').toLowerCase();
    if (status === 401 || status === 403) {
      if (low.includes('gated') || low.includes('awaiting approval') ||
          (low.includes('accept') && low.includes('license'))) {
        return {
          code: 'model_gated',
          message: 'Эта модель закрыта (gated). Откройте её страницу на Hugging Face и примите условия доступа.',
        };
      }
      return {
        code: 'bad_token',
        message: 'Неверный или просроченный токен Hugging Face. Проверьте его в «Настройках».',
      };
    }
    if (status === 404) return { code: 'model_unavailable', message: MODEL_UNAVAILABLE };
    if (status === 429) {
      return {
        code: 'rate_limit',
        message: 'Превышен лимит запросов Hugging Face. Подождите немного и повторите.',
      };
    }
    if (status === 503 || low.includes('loading')) {
      return {
        code: 'model_loading',
        message: 'Модель загружается на стороне Hugging Face. Попробуйте ещё раз через полминуты.',
      };
    }
    if (status === 400 && (low.includes('not supported') || low.includes('no provider') ||
                           low.includes('unsupported'))) {
      return { code: 'model_unavailable', message: MODEL_UNAVAILABLE };
    }
    const detail = extractHfMessage(body);
    return { code: 'hf_error', message: `Ошибка Hugging Face ${status}${detail ? ` (${detail})` : ''}` };
  }

  const ROLES = new Set(['system', 'user', 'assistant']);

  function parseRequest(payload) {
    const bad = (where, msg) =>
      ({ error: { code: 'bad_request', message: `Некорректный запрос: ${where} - ${msg}` } });

    if (!payload || typeof payload !== 'object') return { err: bad('body', 'ожидается объект') };
    if (typeof payload.model !== 'string' || !payload.model) return { err: bad('model', 'обязательное поле') };

    const messages = payload.messages;
    if (!Array.isArray(messages) || messages.length < 1) {
      return { err: bad('messages', 'нужно хотя бы одно сообщение') };
    }
    for (const m of messages) {
      if (!m || typeof m !== 'object' || !ROLES.has(m.role) || typeof m.content !== 'string') {
        return { err: bad('messages', 'каждое сообщение должно иметь role и content') };
      }
    }

    const num = (value, fallback, min, max, exclusiveMin = false) => {
      if (value === undefined || value === null) return fallback;
      const v = Number(value);
      if (!Number.isFinite(v)) return NaN;
      if (exclusiveMin ? v <= min : v < min) return NaN;
      return v > max ? NaN : v;
    };

    const temperature = num(payload.temperature, 0.7, 0, 2);
    if (Number.isNaN(temperature)) return { err: bad('temperature', 'допустимо от 0 до 2') };
    const maxTokens = num(payload.max_tokens, 2048, 1, 32000);
    if (Number.isNaN(maxTokens)) return { err: bad('max_tokens', 'допустимо от 1 до 32000') };
    const topP = num(payload.top_p, 0.95, 0, 1, true);
    if (Number.isNaN(topP)) return { err: bad('top_p', 'допустимо от 0 до 1') };

    return {
      req: {
        model: payload.model,
        messages: messages.map((m) => ({ role: m.role, content: m.content })),
        temperature,
        max_tokens: Math.trunc(maxTokens),
        top_p: topP,
        stream: payload.stream !== false,
      },
    };
  }

  function businessError(req, conf) {
    if (!conf.token) return { code: 'no_token', message: `На сервере не задан токен. ${NO_TOKEN_HINT}` };
    if (!MODEL_ID_RE.test(req.model)) {
      return { code: 'bad_model_id', message: 'Некорректный Model ID. Формат: owner/name' };
    }
    if (req.messages.length > MAX_MESSAGES) {
      return { code: 'too_many_messages', message: 'Слишком длинная история чата.' };
    }
    if (req.messages.reduce((n, m) => n + m.content.length, 0) > MAX_CHARS) {
      return { code: 'too_long', message: 'Слишком длинный запрос.' };
    }
    return null;
  }

  const payloadFor = (req, stream) => ({
    model: req.model,
    messages: req.messages,
    temperature: req.temperature,
    max_tokens: req.max_tokens,
    top_p: req.top_p,
    stream,
  });

  /* ---------- /api/chat ---------- */
  function streamChat(req, conf, signal) {
    const encoder = new TextEncoder();
    const { readable, writable } = new TransformStream();
    const writer = writable.getWriter();
    const send = (event) => writer.write(encoder.encode(sse(event)));

    (async () => {
      try {
        const bizErr = businessError(req, conf);
        if (bizErr) { await send({ type: 'error', ...bizErr }); return; }

        let resp;
        try {
          resp = await realFetch(`${conf.baseUrl}/chat/completions`, {
            method: 'POST',
            headers: {
              Authorization: `Bearer ${conf.token}`,
              'Content-Type': 'application/json',
              Accept: 'text/event-stream',
            },
            body: JSON.stringify(payloadFor(req, true)),
            signal,
          });
        } catch (err) {
          if (err && err.name === 'AbortError') return;
          await send({
            type: 'error', code: 'network',
            message: 'Ошибка сети при обращении к Hugging Face. Проверьте интернет.',
          });
          return;
        }

        if (resp.status >= 400) {
          await send({ type: 'error', ...classifyHttpError(resp.status, await resp.text()) });
          return;
        }

        await send({ type: 'start', model: req.model });

        const reader = resp.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';
        let produced = false;

        for (;;) {
          const { value, done } = await reader.read();
          if (done) break;
          buffer += decoder.decode(value, { stream: true });

          const lines = buffer.split('\n');
          buffer = lines.pop() || '';

          for (const raw of lines) {
            const line = raw.trim();
            if (!line.startsWith('data:')) continue;
            const data = line.slice(5).trim();
            if (data === '[DONE]') { buffer = ''; break; }

            let chunk;
            try { chunk = JSON.parse(data); } catch { continue; }

            if (chunk && chunk.error) {
              await send({ type: 'error', code: 'hf_error', message: extractHfMessage(data) || MODEL_UNAVAILABLE });
              return;
            }
            for (const choice of chunk.choices || []) {
              const delta = choice.delta || {};
              const reasoning = delta.reasoning_content || delta.reasoning;
              if (reasoning) await send({ type: 'reasoning', content: reasoning });
              if (delta.content) { produced = true; await send({ type: 'delta', content: delta.content }); }
            }
            if (chunk.usage) await send({ type: 'usage', usage: chunk.usage });
          }
        }

        if (!produced) {
          await send({
            type: 'error', code: 'empty_response',
            message: 'Модель вернула пустой ответ. Попробуйте ещё раз или выберите другую модель.',
          });
          return;
        }
        await send({ type: 'done' });
      } catch (err) {
        if (!(err && err.name === 'AbortError')) {
          try { await send({ type: 'error', code: 'internal', message: `Внутренняя ошибка: ${err && err.name}` }); }
          catch { /* stream already gone */ }
        }
      } finally {
        try { await writer.close(); } catch { /* already closed */ }
      }
    })();

    return new Response(readable, {
      headers: { 'Content-Type': 'text/event-stream; charset=utf-8', 'Cache-Control': 'no-cache' },
    });
  }

  async function completeOnce(req, conf, signal) {
    const bizErr = businessError(req, conf);
    if (bizErr) return json({ error: bizErr }, bizErr.code === 'no_token' ? 503 : 400);

    let resp;
    try {
      resp = await realFetch(`${conf.baseUrl}/chat/completions`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${conf.token}`, 'Content-Type': 'application/json' },
        body: JSON.stringify(payloadFor(req, false)),
        signal,
      });
    } catch {
      return json({ error: { code: 'network', message: 'Ошибка сети при обращении к Hugging Face.' } }, 502);
    }
    if (resp.status >= 400) return json({ error: classifyHttpError(resp.status, await resp.text()) }, resp.status);

    const data = await resp.json();
    const content = ((data.choices || [])[0]?.message || {}).content || '';
    if (!content) return json({ error: { code: 'empty_response', message: 'Модель вернула пустой ответ.' } }, 502);
    return json({ content, usage: data.usage, model: req.model });
  }

  /* ---------- catalogue + featured ---------- */
  async function loadCatalogue() {
    if (catalogue) return catalogue;
    const url = hasChrome ? chrome.runtime.getURL('models.json') : '/models.json';
    catalogue = await (await realFetch(url)).json();
    return catalogue;
  }

  const baseId = (id) => String(id).split(':', 1)[0].trim().toLowerCase();

  async function fetchServedIds(conf) {
    try {
      const resp = await realFetch(`${conf.baseUrl}/models`, {
        headers: { Authorization: `Bearer ${conf.token}` },
      });
      if (resp.status >= 400) return null;
      const payload = await resp.json();
      const rows = Array.isArray(payload) ? payload : payload.data;
      if (!Array.isArray(rows)) return null;
      const served = new Set();
      for (const row of rows) {
        const id = row && typeof row === 'object' ? row.id : row;
        if (typeof id === 'string' && id) served.add(baseId(id));
      }
      return served.size ? served : null;
    } catch { return null; }
  }

  async function probeModel(conf, modelId) {
    try {
      const resp = await realFetch(`${conf.baseUrl}/chat/completions`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${conf.token}`, 'Content-Type': 'application/json' },
        body: JSON.stringify({
          model: modelId, messages: [{ role: 'user', content: 'ping' }], max_tokens: 1, stream: false,
        }),
      });
      return resp.status < 400;
    } catch { return false; }
  }

  async function resolveFeatured(conf, force) {
    const list = (await loadCatalogue()).featured || [];
    const now = Date.now();
    if (featuredCache.data && !force && now - featuredCache.at < FEATURED_TTL_MS) return featuredCache.data;

    const card = (entry, id, status) => ({
      key: entry.key || '', name: entry.name || '', icon: entry.icon || '',
      desc: entry.desc || '', type: entry.type || 'normal',
      id, status, candidates: entry.candidates || [],
    });

    let result;
    if (!conf.token) {
      result = list.map((e) => card(e, (e.candidates || [])[0] || '', 'offline'));
    } else {
      const served = await fetchServedIds(conf);
      if (served) {
        result = list.map((entry) => {
          const cands = entry.candidates || [];
          const chosen = cands.find((c) => served.has(baseId(c)));
          return card(entry, chosen || cands[0] || '', chosen ? 'online' : 'offline');
        });
      } else {
        result = await Promise.all(list.map(async (entry) => {
          for (const cand of (entry.candidates || []).slice(0, 2)) {
            if (await probeModel(conf, cand)) return card(entry, cand, 'online');
          }
          return card(entry, (entry.candidates || [])[0] || '', 'offline');
        }));
      }
    }

    featuredCache = { at: now, data: result };
    return result;
  }

  /* ---------- the shim ---------- */
  window.fetch = async (input, init = {}) => {
    const href = typeof input === 'string' ? input : input.url;
    let url;
    try { url = new URL(href, location.href); } catch { return realFetch(input, init); }
    if (!url.pathname.startsWith('/api/')) return realFetch(input, init);

    const conf = await settings();
    const route = url.pathname;

    if (route === '/api/health') {
      return json({ ok: true, token_configured: Boolean(conf.token), hint: NO_TOKEN_HINT });
    }
    if (route === '/api/models') return json(await loadCatalogue());
    if (route === '/api/featured') {
      const refresh = ['1', 'true', 'yes'].includes((url.searchParams.get('refresh') || '').toLowerCase());
      return json({ featured: await resolveFeatured(conf, refresh) });
    }
    if (route === '/api/chat') {
      let payload;
      try { payload = JSON.parse(init.body); }
      catch { return json({ error: { code: 'bad_request', message: 'Некорректный JSON' } }, 400); }
      const { req, err } = parseRequest(payload);
      if (err) return json(err, 400);
      return req.stream ? streamChat(req, conf, init.signal) : completeOnce(req, conf, init.signal);
    }
    return new Response('Not found', { status: 404 });
  };

  /* ---------- token field inside Settings ---------- */
  document.addEventListener('DOMContentLoaded', async () => {
    const field = document.getElementById('hfToken');
    if (!field) return;

    const got = await store.get(['hfToken']);
    field.value = got.hfToken || '';

    let timer = null;
    field.addEventListener('input', () => {
      clearTimeout(timer);
      timer = setTimeout(async () => {
        await store.set({ hfToken: field.value.trim() });
        featuredCache = { at: 0, data: null };   // re-check which models are live
      }, 400);
    });
  });
})();
