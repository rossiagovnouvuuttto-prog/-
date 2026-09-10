/**
 * Service worker: единственное место, где происходит обращение к Claude API.
 * Ключ хранится в chrome.storage.local и никогда не попадает в исходный код.
 */

const API_URL = 'https://api.anthropic.com/v1/messages';
const API_VERSION = '2023-06-01';

const DEFAULTS = {
  apiKey: '',
  model: 'claude-sonnet-5',
  maxTokens: 1200,
  theme: 'auto',
  enabled: true
};

async function getSettings() {
  const stored = await chrome.storage.local.get(DEFAULTS);
  return { ...DEFAULTS, ...stored };
}

const SYSTEM_PROMPT = [
  'Ты — помощник школьника на образовательной платформе Foxford.',
  'Тебе дают текст задания (иногда со скриншотом страницы).',
  'Определи вопрос и варианты ответа, реши задание и объясни решение простыми словами.',
  'Отвечай только на русском языке.',
  'Верни СТРОГО один JSON-объект без markdown-обрамления и без текста вокруг, по схеме:',
  '{"question": "краткая формулировка вопроса",',
  ' "answer": "предполагаемый правильный ответ (если есть варианты — процитируй нужный вариант)",',
  ' "explanation": "объяснение простыми словами, 2-5 предложений",',
  ' "confidence": 0.0-1.0,',
  ' "confidence_reason": "почему такая уверенность"}',
  'Если данных не хватает — всё равно верни JSON, поставь низкую уверенность и объясни, чего не хватает.'
].join(' ');

function buildUserContent(payload) {
  const content = [];

  if (payload.screenshot) {
    const match = /^data:(image\/(?:png|jpeg|webp));base64,(.+)$/.exec(payload.screenshot);
    if (match) {
      content.push({
        type: 'image',
        source: { type: 'base64', media_type: match[1], data: match[2] }
      });
    }
  }

  const parts = [];
  if (payload.pageTitle) parts.push(`Страница: ${payload.pageTitle}`);
  if (payload.question) parts.push(`Текст задания:\n${payload.question}`);
  if (Array.isArray(payload.options) && payload.options.length) {
    parts.push('Варианты ответа:\n' + payload.options.map((o, i) => `${i + 1}) ${o}`).join('\n'));
  }
  if (payload.followUp) {
    parts.push('Дополнительно: дай более подробное пошаговое объяснение того же задания.');
  }
  if (payload.screenshot) {
    parts.push('К заданию приложен снимок видимой области страницы — используй его, если в тексте не хватает данных (график, картинка, формула).');
  }
  content.push({ type: 'text', text: parts.join('\n\n') || 'Задание не распознано, опиши, что видно на изображении, и реши его.' });

  return content;
}

function extractJson(text) {
  const cleaned = String(text || '').replace(/^\s*```(?:json)?/i, '').replace(/```\s*$/, '').trim();
  const start = cleaned.indexOf('{');
  const end = cleaned.lastIndexOf('}');
  if (start === -1 || end === -1 || end < start) return null;
  try {
    return JSON.parse(cleaned.slice(start, end + 1));
  } catch (e) {
    return null;
  }
}

async function askClaude(payload) {
  const settings = await getSettings();
  if (!settings.apiKey) {
    return { ok: false, error: 'NO_KEY', message: 'API-ключ не задан. Откройте настройки расширения и введите ключ Claude.' };
  }

  const body = {
    model: settings.model,
    max_tokens: Number(settings.maxTokens) || 1200,
    system: SYSTEM_PROMPT,
    messages: [{ role: 'user', content: buildUserContent(payload) }]
  };

  let response;
  try {
    response = await fetch(API_URL, {
      method: 'POST',
      headers: {
        'content-type': 'application/json',
        'x-api-key': settings.apiKey,
        'anthropic-version': API_VERSION,
        'anthropic-dangerous-direct-browser-access': 'true'
      },
      body: JSON.stringify(body)
    });
  } catch (e) {
    return { ok: false, error: 'NETWORK', message: 'Не удалось связаться с Claude API: ' + e.message };
  }

  const raw = await response.text();
  if (!response.ok) {
    let detail = raw;
    try {
      detail = JSON.parse(raw)?.error?.message || raw;
    } catch (e) { /* оставляем сырой текст */ }
    const hint = response.status === 401 ? ' Проверьте API-ключ в настройках.' : '';
    return { ok: false, error: 'HTTP_' + response.status, message: `Ошибка API (${response.status}): ${detail}${hint}` };
  }

  let data;
  try {
    data = JSON.parse(raw);
  } catch (e) {
    return { ok: false, error: 'BAD_JSON', message: 'Некорректный ответ API.' };
  }

  const text = (data.content || []).filter((b) => b.type === 'text').map((b) => b.text).join('\n').trim();
  const parsed = extractJson(text);

  return {
    ok: true,
    result: parsed || { question: payload.question || '', answer: '', explanation: text, confidence: null },
    rawText: text,
    usage: data.usage || null
  };
}

async function captureVisibleTab() {
  const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
  if (!tab) throw new Error('Активная вкладка не найдена.');
  return chrome.tabs.captureVisibleTab(tab.windowId, { format: 'png' });
}

async function testKey(apiKey, model) {
  try {
    const response = await fetch(API_URL, {
      method: 'POST',
      headers: {
        'content-type': 'application/json',
        'x-api-key': apiKey,
        'anthropic-version': API_VERSION,
        'anthropic-dangerous-direct-browser-access': 'true'
      },
      body: JSON.stringify({
        model: model || DEFAULTS.model,
        max_tokens: 16,
        messages: [{ role: 'user', content: 'ping' }]
      })
    });
    if (response.ok) return { ok: true, message: 'Ключ работает.' };
    const raw = await response.text();
    let detail = raw;
    try { detail = JSON.parse(raw)?.error?.message || raw; } catch (e) { /* noop */ }
    return { ok: false, message: `Ошибка ${response.status}: ${detail}` };
  } catch (e) {
    return { ok: false, message: 'Сеть недоступна: ' + e.message };
  }
}

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  (async () => {
    try {
      switch (message?.type) {
        case 'ASK_CLAUDE':
          sendResponse(await askClaude(message.payload || {}));
          break;
        case 'CAPTURE_TAB':
          sendResponse({ ok: true, dataUrl: await captureVisibleTab() });
          break;
        case 'GET_SETTINGS':
          sendResponse({ ok: true, settings: await getSettings() });
          break;
        case 'OPEN_OPTIONS':
          chrome.runtime.openOptionsPage();
          sendResponse({ ok: true });
          break;
        case 'TEST_KEY':
          sendResponse(await testKey(message.apiKey, message.model));
          break;
        default:
          sendResponse({ ok: false, message: 'Неизвестный запрос.' });
      }
    } catch (e) {
      sendResponse({ ok: false, error: 'EXCEPTION', message: e?.message || String(e) });
    }
  })();
  return true; // ответ асинхронный
});

chrome.runtime.onInstalled.addListener(async (details) => {
  if (details.reason === 'install') {
    await chrome.storage.local.set({ ...DEFAULTS });
    chrome.runtime.openOptionsPage();
  }
});
