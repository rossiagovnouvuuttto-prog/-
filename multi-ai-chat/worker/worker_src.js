/* ==============================================================
   Multi AI Chat - Cloudflare Worker backend.

   Same contract as app.py: the browser only ever talks to this
   Worker, and HF_TOKEN lives in the Worker's secrets. The frontend
   is embedded below by build_worker.py, so the whole site is one
   file that can be pasted into the Cloudflare dashboard.
   ============================================================== */
'use strict';

// __ASSETS__
// __MODELS__

const HF_DEFAULT_BASE = 'https://router.huggingface.co/v1';
const MODEL_ID_RE = /^[A-Za-z0-9._-]+\/[A-Za-z0-9._-]+(:[A-Za-z0-9._-]+)?$/;
const MAX_MESSAGES = 120;
const MAX_CHARS = 120000;
const FEATURED_TTL_MS = 300000;

const MODEL_UNAVAILABLE = 'Модель сейчас недоступна через Hugging Face Inference.';

let featuredCache = { at: 0, data: null };

const cfg = (env) => ({
  token: (env.HF_TOKEN || '').trim(),
  baseUrl: (env.HF_BASE_URL || HF_DEFAULT_BASE).replace(/\/+$/, ''),
  timeout: Number(env.HF_TIMEOUT || 120) * 1000,
});

const json = (body, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store' },
  });

const sse = (event) => `data: ${JSON.stringify(event)}\n\n`;

/* ============== Error translation (mirrors app.py) ============== */
function extractHfMessage(body) {
  try {
    const data = JSON.parse(body);
    if (data && typeof data === 'object') {
      const err = data.error;
      if (err && typeof err === 'object') return String(err.message || '').slice(0, 200);
      if (typeof err === 'string') return err.slice(0, 200);
      if (data.message) return String(data.message).slice(0, 200);
    }
  } catch { /* fall through to the raw body */ }
  return body.trim().slice(0, 200);
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
      message: 'Неверный или просроченный HF_TOKEN. Проверьте секрет на сервере.',
    };
  }
  if (status === 404) return { code: 'model_unavailable', message: MODEL_UNAVAILABLE };
  if (status === 429) {
    return {
      code: 'rate_limit',
      message: 'Превышен лимит запросов Hugging Face. Подождите немного и повторите.',
    };
  }
  if (status === 503 || low.includes('loading') || low.includes('currently loading')) {
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
  return {
    code: 'hf_error',
    message: `Ошибка Hugging Face ${status}${detail ? ` (${detail})` : ''}`,
  };
}

/* ============== Request validation ============== */
const ROLES = new Set(['system', 'user', 'assistant']);

/** Schema-level checks -> HTTP 400, matching the pydantic model in app.py. */
function parseRequest(payload) {
  const bad = (where, msg) => ({ error: { code: 'bad_request', message: `Некорректный запрос: ${where} - ${msg}` } });

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

  const num = (name, value, fallback, min, max, exclusiveMin = false) => {
    if (value === undefined || value === null) return fallback;
    const v = Number(value);
    if (!Number.isFinite(v)) return NaN;
    if (exclusiveMin ? v <= min : v < min) return NaN;
    if (v > max) return NaN;
    return v;
  };

  const temperature = num('temperature', payload.temperature, 0.7, 0, 2);
  if (Number.isNaN(temperature)) return { err: bad('temperature', 'допустимо от 0 до 2') };
  const maxTokens = num('max_tokens', payload.max_tokens, 2048, 1, 32000);
  if (Number.isNaN(maxTokens)) return { err: bad('max_tokens', 'допустимо от 1 до 32000') };
  const topP = num('top_p', payload.top_p, 0.95, 0, 1, true);
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

/** Business-level checks -> reported as an SSE error event, like app.py. */
function businessError(req, conf) {
  if (!conf.token) {
    return {
      code: 'no_token',
      message: 'На сервере не задан HF_TOKEN. Добавьте его в секреты хостинга и перезапустите приложение.',
    };
  }
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

function withTimeout(ms) {
  const ctrl = new AbortController();
  const id = setTimeout(() => ctrl.abort(), ms);
  return { signal: ctrl.signal, done: () => clearTimeout(id) };
}

/* ============== Streaming chat ============== */
function streamChat(req, conf, clientSignal) {
  const encoder = new TextEncoder();
  const { readable, writable } = new TransformStream();
  const writer = writable.getWriter();
  const send = (event) => writer.write(encoder.encode(sse(event)));

  (async () => {
    const guard = withTimeout(conf.timeout);
    const onAbort = () => guard.signal.dispatchEvent?.(new Event('abort'));
    try {
      const bizErr = businessError(req, conf);
      if (bizErr) { await send({ type: 'error', ...bizErr }); return; }

      let resp;
      try {
        resp = await fetch(`${conf.baseUrl}/chat/completions`, {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${conf.token}`,
            'Content-Type': 'application/json',
            Accept: 'text/event-stream',
          },
          body: JSON.stringify(payloadFor(req, true)),
          signal: guard.signal,
        });
      } catch (err) {
        const timedOut = err && (err.name === 'AbortError' || err.name === 'TimeoutError');
        await send(timedOut
          ? { type: 'error', code: 'timeout', message: 'Время ожидания ответа истекло. Попробуйте ещё раз.' }
          : { type: 'error', code: 'network', message: 'Ошибка сети при обращении к Hugging Face.' });
        return;
      }

      if (resp.status >= 400) {
        const body = await resp.text();
        await send({ type: 'error', ...classifyHttpError(resp.status, body) });
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
          type: 'error',
          code: 'empty_response',
          message: 'Модель вернула пустой ответ. Попробуйте ещё раз или выберите другую модель.',
        });
        return;
      }
      await send({ type: 'done' });
    } catch (err) {
      try {
        await send({ type: 'error', code: 'internal', message: `Внутренняя ошибка сервера: ${err && err.name}` });
      } catch { /* the client already went away */ }
    } finally {
      guard.done();
      try { await writer.close(); } catch { /* already closed */ }
      void onAbort;
      void clientSignal;
    }
  })();

  return new Response(readable, {
    headers: {
      'Content-Type': 'text/event-stream; charset=utf-8',
      'Cache-Control': 'no-cache, no-transform',
      Connection: 'keep-alive',
      'X-Accel-Buffering': 'no',
    },
  });
}

async function completeOnce(req, conf) {
  const bizErr = businessError(req, conf);
  if (bizErr) return json({ error: bizErr }, bizErr.code === 'no_token' ? 503 : 400);

  const guard = withTimeout(conf.timeout);
  let resp;
  try {
    resp = await fetch(`${conf.baseUrl}/chat/completions`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${conf.token}`, 'Content-Type': 'application/json' },
      body: JSON.stringify(payloadFor(req, false)),
      signal: guard.signal,
    });
  } catch (err) {
    guard.done();
    const timedOut = err && (err.name === 'AbortError' || err.name === 'TimeoutError');
    return json({
      error: timedOut
        ? { code: 'timeout', message: 'Время ожидания ответа истекло.' }
        : { code: 'network', message: 'Ошибка сети при обращении к Hugging Face.' },
    }, timedOut ? 504 : 502);
  }
  guard.done();

  if (resp.status >= 400) {
    return json({ error: classifyHttpError(resp.status, await resp.text()) }, resp.status);
  }

  const data = await resp.json();
  const content = ((data.choices || [])[0]?.message || {}).content || '';
  if (!content) {
    return json({ error: { code: 'empty_response', message: 'Модель вернула пустой ответ.' } }, 502);
  }
  return json({ content, usage: data.usage, model: req.model });
}

/* ============== Featured models ============== */
const baseId = (id) => String(id).split(':', 1)[0].trim().toLowerCase();

async function fetchServedIds(conf) {
  try {
    const guard = withTimeout(20000);
    const resp = await fetch(`${conf.baseUrl}/models`, {
      headers: { Authorization: `Bearer ${conf.token}` },
      signal: guard.signal,
    });
    guard.done();
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
    const guard = withTimeout(20000);
    const resp = await fetch(`${conf.baseUrl}/chat/completions`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${conf.token}`, 'Content-Type': 'application/json' },
      body: JSON.stringify({
        model: modelId,
        messages: [{ role: 'user', content: 'ping' }],
        max_tokens: 1,
        stream: false,
      }),
      signal: guard.signal,
    });
    guard.done();
    return resp.status < 400;
  } catch { return false; }
}

async function resolveFeatured(conf, force) {
  const list = MODELS.featured || [];
  const now = Date.now();
  if (featuredCache.data && !force && now - featuredCache.at < FEATURED_TTL_MS) {
    return featuredCache.data;
  }

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
        const cands = (entry.candidates || []).slice(0, 2);
        for (const cand of cands) {
          if (await probeModel(conf, cand)) return card(entry, cand, 'online');
        }
        return card(entry, cands[0] || '', 'offline');
      }));
    }
  }

  featuredCache = { at: now, data: result };
  return result;
}

/* ============== Router ============== */
const TYPES = {
  'index.html': 'text/html; charset=utf-8',
  'styles.css': 'text/css; charset=utf-8',
  'app.js': 'application/javascript; charset=utf-8',
};

const asset = (name) =>
  new Response(ASSETS[name], {
    headers: { 'Content-Type': TYPES[name], 'Cache-Control': 'public, max-age=300' },
  });

export default {
  async fetch(request, env) {
    const conf = cfg(env);
    const url = new URL(request.url);
    const path = url.pathname;

    if (path === '/' || path === '/index.html') return asset('index.html');
    if (path === '/static/styles.css') return asset('styles.css');
    if (path === '/static/app.js') return asset('app.js');

    if (path === '/api/health') {
      return json({ ok: true, token_configured: Boolean(conf.token), base_url: conf.baseUrl });
    }
    if (path === '/api/models') return json(MODELS);
    if (path === '/api/featured') {
      const refresh = ['1', 'true', 'yes'].includes((url.searchParams.get('refresh') || '').toLowerCase());
      return json({ featured: await resolveFeatured(conf, refresh) });
    }

    if (path === '/api/chat') {
      if (request.method !== 'POST') return json({ error: { code: 'bad_method', message: 'Нужен POST' } }, 405);
      let payload;
      try { payload = await request.json(); }
      catch { return json({ error: { code: 'bad_request', message: 'Некорректный JSON' } }, 400); }

      const { req, err } = parseRequest(payload);
      if (err) return json(err, 400);
      return req.stream ? streamChat(req, conf, request.signal) : completeOnce(req, conf);
    }

    return new Response('Not found', { status: 404 });
  },
};
