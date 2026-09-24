'use strict';

const PAGE = chrome.runtime.getURL('index.html');
const SERVICE_KEY = 'multiAiServiceWindowV23';
const PROVIDERS = {
  'chatgpt-web': { url: 'https://chatgpt.com/', name: 'ChatGPT' },
  'deepseek-web': { url: 'https://chat.deepseek.com/', name: 'DeepSeek' },
};

let serviceState = null;
let serviceLock = Promise.resolve();
let activeOwner = null;

chrome.action.onClicked.addListener(async () => {
  const [existing] = await chrome.tabs.query({ url: PAGE });
  if (existing) {
    await chrome.tabs.update(existing.id, { active: true });
    await chrome.windows.update(existing.windowId, { focused: true });
    return;
  }
  await chrome.tabs.create({ url: PAGE });
});

chrome.runtime.onInstalled.addListener(({ reason }) => {
  if (reason === 'install') chrome.tabs.create({ url: PAGE });
});

function codedError(code, message) {
  return Object.assign(new Error(message), { code });
}

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

async function getServiceState() {
  if (serviceState) return serviceState;
  const stored = await chrome.storage.local.get(SERVICE_KEY);
  const v = stored[SERVICE_KEY];
  serviceState = v && typeof v === 'object'
    ? { windowId: Number.isInteger(v.windowId) ? v.windowId : null, tabId: Number.isInteger(v.tabId) ? v.tabId : null }
    : { windowId: null, tabId: null };
  return serviceState;
}

async function saveServiceState() {
  if (!serviceState) return;
  await chrome.storage.local.set({ [SERVICE_KEY]: serviceState });
}

async function clearServiceState() {
  serviceState = { windowId: null, tabId: null };
  await saveServiceState();
}

async function refocusOwner() {
  const owner = activeOwner;
  if (!owner) return;
  try {
    if (Number.isInteger(owner.tabId)) await chrome.tabs.update(owner.tabId, { active: true });
    if (Number.isInteger(owner.windowId)) await chrome.windows.update(owner.windowId, { focused: true });
  } catch {}
}

async function hideServiceWindow(windowId) {
  if (!Number.isInteger(windowId)) return;
  const win = await chrome.windows.get(windowId).catch(() => null);
  if (!win) return;
  try {
    if (win.state !== 'minimized') await chrome.windows.update(windowId, { state: 'minimized' });
  } catch {}
  // If Chromium accidentally focused the automation window, immediately put the
  // user's Multi AI Chat window back in front.
  if (win.focused) await refocusOwner();
}

chrome.windows.onFocusChanged.addListener((windowId) => {
  getServiceState().then(async (state) => {
    if (windowId !== chrome.windows.WINDOW_ID_NONE && windowId === state.windowId) {
      await hideServiceWindow(state.windowId);
      await refocusOwner();
    }
  }).catch(() => {});
});

chrome.windows.onRemoved.addListener((windowId) => {
  getServiceState().then(async (state) => {
    if (windowId === state.windowId) await clearServiceState();
  }).catch(() => {});
});

async function waitTabComplete(tabId, timeoutMs = 60000) {
  const first = await chrome.tabs.get(tabId).catch(() => null);
  if (!first) throw codedError('tab_closed', 'Служебная вкладка была закрыта.');
  if (first.status === 'complete') return first;

  return new Promise((resolve, reject) => {
    let done = false;
    const cleanup = () => {
      chrome.tabs.onUpdated.removeListener(onUpdated);
      chrome.tabs.onRemoved.removeListener(onRemoved);
      clearTimeout(timer);
    };
    const finish = (fn, value) => {
      if (done) return;
      done = true;
      cleanup();
      fn(value);
    };
    const onUpdated = (id, info, tab) => {
      if (id === tabId && info.status === 'complete') finish(resolve, tab);
    };
    const onRemoved = (id) => {
      if (id === tabId) finish(reject, codedError('tab_closed', 'Служебная вкладка была закрыта.'));
    };
    const timer = setTimeout(() => finish(reject, codedError('provider_timeout', 'Сайт нейросети слишком долго загружается.')), timeoutMs);
    chrome.tabs.onUpdated.addListener(onUpdated);
    chrome.tabs.onRemoved.addListener(onRemoved);
    chrome.tabs.get(tabId).then((tab) => {
      if (tab?.status === 'complete') finish(resolve, tab);
    }).catch(() => finish(reject, codedError('tab_closed', 'Служебная вкладка была закрыта.')));
  });
}

async function createServiceWindow(url) {
  let win;
  try {
    win = await chrome.windows.create({ url, focused: false, state: 'minimized', type: 'normal' });
  } catch {
    // Some Chromium builds reject minimized-at-create. Create unfocused and
    // minimize immediately. The focus guard above prevents it staying on top.
    win = await chrome.windows.create({ url, focused: false, type: 'normal' });
    if (win?.id) await chrome.windows.update(win.id, { state: 'minimized' }).catch(() => {});
  }
  const tab = Array.isArray(win?.tabs) ? win.tabs[0] : null;
  if (!win?.id || !tab?.id) throw codedError('provider_window', 'Не удалось создать скрытую вкладку нейросети.');
  serviceState = { windowId: win.id, tabId: tab.id };
  await saveServiceState();
  await hideServiceWindow(win.id);
  // A second minimize catches Windows builds that briefly restore a newly
  // created browser window while the first external page is loading.
  setTimeout(() => hideServiceWindow(win.id).catch(() => {}), 250);
  setTimeout(() => hideServiceWindow(win.id).catch(() => {}), 900);
  return { windowId: win.id, tabId: tab.id };
}

async function ensureServiceTab(model) {
  const provider = PROVIDERS[model];
  if (!provider) throw codedError('bad_model_id', 'Неизвестная веб-модель.');

  const state = await getServiceState();
  let win = state.windowId ? await chrome.windows.get(state.windowId, { populate: true }).catch(() => null) : null;
  let tab = state.tabId ? await chrome.tabs.get(state.tabId).catch(() => null) : null;
  if (!win || !tab || tab.windowId !== win.id) {
    await clearServiceState();
    ({ windowId: serviceState.windowId, tabId: serviceState.tabId } = await createServiceWindow(provider.url));
    win = await chrome.windows.get(serviceState.windowId).catch(() => null);
    tab = await chrome.tabs.get(serviceState.tabId).catch(() => null);
  }

  if (!win || !tab) throw codedError('provider_window', 'Служебное окно нейросети недоступно.');
  await hideServiceWindow(win.id);

  const currentUrl = String(tab.url || '');
  let onProvider = false;
  try {
    const u = new URL(currentUrl);
    const p = new URL(provider.url);
    onProvider = u.origin === p.origin && u.pathname === '/';
  } catch {}

  if (!onProvider) {
    await chrome.tabs.update(tab.id, { url: provider.url, active: true });
    await hideServiceWindow(win.id);
  } else {
    // There is only one service tab. Keeping it active inside the minimized
    // window avoids the ChatGPT/DeepSeek cross-tab race from v2.2.
    await chrome.tabs.update(tab.id, { active: true }).catch(() => {});
    await hideServiceWindow(win.id);
  }
  await waitTabComplete(tab.id, 60000);
  await hideServiceWindow(win.id);
  await assertOnProvider(tab.id, provider);
  return tab.id;
}

// A finished load is not a successful one: a dead network leaves Chrome's own
// error page, and a logged-out session redirects to the sign-in host. Both
// look "complete", so the landing URL is what decides.
async function assertOnProvider(tabId, provider) {
  const tab = await chrome.tabs.get(tabId).catch(() => null);
  if (!tab) throw codedError('tab_closed', '\u0421\u043B\u0443\u0436\u0435\u0431\u043D\u0430\u044F \u0432\u043A\u043B\u0430\u0434\u043A\u0430 \u0431\u044B\u043B\u0430 \u0437\u0430\u043A\u0440\u044B\u0442\u0430.');

  const here = String(tab.url || '');
  if (!here || /^(chrome-error|about:|chrome:)/i.test(here)) {
    throw codedError('provider_unreachable',
      `\u0421\u0430\u0439\u0442 ${provider.name} \u043D\u0435 \u043E\u0442\u043A\u0440\u044B\u043B\u0441\u044F. \u041F\u0440\u043E\u0432\u0435\u0440\u044C\u0442\u0435 \u0438\u043D\u0442\u0435\u0440\u043D\u0435\u0442 \u0438 \u043F\u043E\u043F\u0440\u043E\u0431\u0443\u0439\u0442\u0435 \u0435\u0449\u0451 \u0440\u0430\u0437.`);
  }

  let sameSite = false;
  try {
    sameSite = new URL(here).origin === new URL(provider.url).origin;
  } catch {}
  if (!sameSite) {
    // The extension may only script the provider's own origin, so a redirect
    // elsewhere is the sign-in flow rather than something to inject into.
    throw codedError('provider_auth',
      `\u041D\u0443\u0436\u043D\u043E \u0432\u043E\u0439\u0442\u0438 \u0432 ${provider.name} \u0432 \u044D\u0442\u043E\u043C \u0431\u0440\u0430\u0443\u0437\u0435\u0440\u0435: \u0441\u0430\u0439\u0442 \u0443\u0432\u0451\u043B \u043D\u0430 \u0441\u0442\u0440\u0430\u043D\u0438\u0446\u0443 \u0432\u0445\u043E\u0434\u0430.`);
  }
}

async function resetServiceTab(model, tabId) {
  const provider = PROVIDERS[model];
  if (!provider || !tabId) return;
  const tab = await chrome.tabs.get(tabId).catch(() => null);
  if (!tab) return;
  await chrome.tabs.update(tabId, { url: provider.url, active: true }).catch(() => {});
  await hideServiceWindow(tab.windowId);
}

function buildPrompt(messages) {
  const clean = Array.isArray(messages)
    ? messages.filter((m) => m && typeof m.content === 'string' && m.content.trim())
    : [];
  const system = clean.filter((m) => m.role === 'system').map((m) => m.content.trim()).join('\n\n');
  const dialogue = clean.filter((m) => m.role !== 'system').map((m) => {
    const who = m.role === 'assistant' ? 'Ассистент' : 'Пользователь';
    return `${who}: ${m.content.trim()}`;
  }).join('\n\n');
  const parts = ['Продолжи этот диалог как ассистент. Ответь только следующим сообщением ассистента, без служебных пояснений о том, что тебе передан диалог.'];
  if (system) parts.push(`Системные инструкции:\n${system}`);
  parts.push(`Диалог:\n${dialogue}`);
  return parts.join('\n\n');
}

// Chrome raises these as plain Errors with English text meant for developers.
// Anything that reaches the chat has to be a sentence the user can act on, so
// every injection failure is mapped here instead of being passed through.
function injectionError(provider, err) {
  const raw = String(err?.message || err || '');
  console.warn('[multi-ai-chat] injection failed:', raw);

  if (/showing error page|ERR_|net::|No tab with id|No frame with id|Frame with ID|Cannot access a chrome/i.test(raw)) {
    return codedError('provider_unreachable',
      `\u0421\u0430\u0439\u0442 ${provider.name} \u043D\u0435 \u043E\u0442\u043A\u0440\u044B\u043B\u0441\u044F. \u041F\u0440\u043E\u0432\u0435\u0440\u044C\u0442\u0435 \u0438\u043D\u0442\u0435\u0440\u043D\u0435\u0442 \u0438 \u043F\u043E\u043F\u0440\u043E\u0431\u0443\u0439\u0442\u0435 \u0435\u0449\u0451 \u0440\u0430\u0437.`);
  }
  if (/permission|Cannot access contents|host permissions/i.test(raw)) {
    return codedError('provider_blocked',
      `Chrome \u043D\u0435 \u0434\u0430\u043B \u0440\u0430\u0441\u0448\u0438\u0440\u0435\u043D\u0438\u044E \u0434\u043E\u0441\u0442\u0443\u043F \u043A ${provider.name}. \u041E\u0442\u043A\u0440\u043E\u0439\u0442\u0435 chrome://extensions \u0438 \u0440\u0430\u0437\u0440\u0435\u0448\u0438\u0442\u0435 \u0434\u043E\u0441\u0442\u0443\u043F \u043A \u0441\u0430\u0439\u0442\u0443.`);
  }
  return codedError('provider_error',
    `\u041D\u0435 \u0443\u0434\u0430\u043B\u043E\u0441\u044C \u043F\u043E\u0440\u0430\u0431\u043E\u0442\u0430\u0442\u044C \u0441\u043E \u0441\u0442\u0440\u0430\u043D\u0438\u0446\u0435\u0439 ${provider.name}. \u041F\u043E\u043F\u0440\u043E\u0431\u0443\u0439\u0442\u0435 \u0435\u0449\u0451 \u0440\u0430\u0437.`);
}

async function runProvider(tabId, model, prompt) {
  const provider = PROVIDERS[model] || { name: '\u0441\u0430\u0439\u0442 \u043D\u0435\u0439\u0440\u043E\u0441\u0435\u0442\u0438' };
  let frames;
  try {
    frames = await chrome.scripting.executeScript({
      target: { tabId },
      world: 'MAIN',
      args: [model, prompt],
      func: async (providerModel, text) => {
        const isChatGPT = providerModel === 'chatgpt-web';
        const providerName = isChatGPT ? 'ChatGPT' : 'DeepSeek';
        const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
        const visible = (el) => !!el && !!(el.offsetWidth || el.offsetHeight || el.getClientRects().length);
        const labelOf = (el) => `${el?.getAttribute?.('aria-label') || ''} ${el?.getAttribute?.('title') || ''} ${el?.textContent || ''}`.trim();
        const norm = (s) => String(s || '').replace(/\u00a0/g, ' ').replace(/\s+/g, ' ').trim();

        function loginScreen() {
          const candidates = [...document.querySelectorAll('button,a')].filter(visible);
          return candidates.some((el) => /^(log in|sign in|войти|登录)$/i.test(norm(labelOf(el))));
        }

        function composerCandidates() {
          const selectors = isChatGPT
            ? ['#prompt-textarea', '[contenteditable="true"][data-lexical-editor="true"]', '[contenteditable="true"][role="textbox"]', 'textarea[data-testid*="prompt"]', 'textarea']
            : ['textarea', '[contenteditable="true"][role="textbox"]', '[contenteditable="true"]'];
          const set = new Set();
          for (const selector of selectors) {
            for (const el of document.querySelectorAll(selector)) {
              if (visible(el) && !el.disabled && el.getAttribute('aria-disabled') !== 'true') set.add(el);
            }
          }
          return [...set];
        }

        function findComposer() {
          const list = composerCandidates();
          if (!list.length) return null;
          const scored = list.map((el) => {
            const r = el.getBoundingClientRect();
            const meta = `${el.id || ''} ${el.getAttribute('placeholder') || ''} ${el.getAttribute('aria-label') || ''} ${el.getAttribute('data-placeholder') || ''}`;
            let score = Math.max(0, r.width) * Math.max(0, r.height) + Math.max(0, r.top);
            if (el.id === 'prompt-textarea') score += 1000000;
            if (/message|ask|chatgpt|deepseek|сообщ|вопрос|输入|发送/i.test(meta)) score += 200000;
            if (/search|поиск/i.test(meta)) score -= 500000;
            return { el, score };
          }).sort((a, b) => b.score - a.score);
          return scored[0]?.el || null;
        }

        async function waitComposer(timeout = 45000) {
          const started = Date.now();
          while (Date.now() - started < timeout) {
            const c = findComposer();
            if (c) return c;
            if (loginScreen()) return null;
            await sleep(250);
          }
          return null;
        }

        function composerText(el) {
          return norm(('value' in el ? el.value : el.innerText || el.textContent || ''));
        }

        function nativeSetValue(el, value) {
          const proto = el instanceof HTMLTextAreaElement ? HTMLTextAreaElement.prototype : HTMLInputElement.prototype;
          const setter = Object.getOwnPropertyDescriptor(proto, 'value')?.set;
          if (setter) setter.call(el, value); else el.value = value;
        }

        async function setComposer(editor, value) {
          editor.focus();
          if (editor instanceof HTMLTextAreaElement || editor instanceof HTMLInputElement) {
            nativeSetValue(editor, value);
            editor.dispatchEvent(new InputEvent('input', { bubbles: true, inputType: 'insertText', data: value }));
            editor.dispatchEvent(new Event('change', { bubbles: true }));
          } else {
            try {
              const range = document.createRange();
              range.selectNodeContents(editor);
              const sel = window.getSelection();
              sel.removeAllRanges();
              sel.addRange(range);
              document.execCommand('delete', false);
              document.execCommand('insertText', false, value);
              sel.removeAllRanges();
            } catch {}
            if (composerText(editor).length < Math.min(8, value.length)) {
              // Lexical/ProseMirror fallback used by current ChatGPT/DeepSeek.
              const p = editor.querySelector('p');
              if (p) p.textContent = value;
              else editor.textContent = value;
            }
            editor.dispatchEvent(new InputEvent('beforeinput', { bubbles: true, cancelable: true, inputType: 'insertText', data: value }));
            editor.dispatchEvent(new InputEvent('input', { bubbles: true, inputType: 'insertText', data: value }));
            editor.dispatchEvent(new Event('change', { bubbles: true }));
          }
          await sleep(80);
          return composerText(editor);
        }

        function findSendButton(editor) {
          const exactSelectors = isChatGPT
            ? ['button[data-testid="send-button"]', 'button[aria-label="Send prompt"]', 'button[aria-label*="Send" i]', 'button[aria-label*="Отправ" i]']
            : ['button[aria-label*="send" i]', 'button[aria-label*="发送" i]', 'button[title*="send" i]', 'button[title*="发送" i]'];
          const scopes = [];
          const form = editor?.closest?.('form');
          if (form) scopes.push(form);
          let p = editor?.parentElement;
          for (let i = 0; p && i < 5; i++, p = p.parentElement) scopes.push(p);
          scopes.push(document);
          for (const selector of exactSelectors) {
            for (const scope of scopes) {
              const b = [...scope.querySelectorAll(selector)].find((x) => visible(x) && !x.disabled && x.getAttribute('aria-disabled') !== 'true');
              if (b) return b;
            }
          }
          // DeepSeek frequently uses an icon-only submit button. Restrict the
          // fallback to the composer/form area so we never click sidebar buttons.
          const scope = form || editor?.parentElement?.parentElement || document;
          let best = null;
          let bestScore = -Infinity;
          for (const b of scope.querySelectorAll('button')) {
            if (!visible(b) || b.disabled || b.getAttribute('aria-disabled') === 'true') continue;
            const label = labelOf(b);
            if (/attach|upload|file|image|voice|microphone|mic|скреп|файл|изображ|голос|语音|上传|stop|останов/i.test(label)) continue;
            let score = 0;
            if (/send|submit|отправ|发送|提交/i.test(label)) score += 1000;
            if (b.type === 'submit') score += 500;
            const r = b.getBoundingClientRect();
            const e = editor.getBoundingClientRect();
            if (r.left >= e.left && Math.abs(r.top - e.top) < 180) score += 120;
            if (b.querySelector('svg')) score += 20;
            if (score > bestScore) { best = b; bestScore = score; }
          }
          return bestScore >= 120 ? best : null;
        }

        function responseNodes() {
          const set = new Set();
          const add = (n) => { if (n && visible(n)) set.add(n); };
          if (isChatGPT) {
            for (const n of document.querySelectorAll('[data-message-author-role="assistant"]')) add(n.closest('article') || n.closest('[data-testid^="conversation-turn-"]') || n);
            for (const n of document.querySelectorAll('article[data-testid^="conversation-turn-"], [data-testid^="conversation-turn-"]')) {
              if (n.querySelector('[data-message-author-role="assistant"]')) add(n);
            }
            for (const n of document.querySelectorAll('main .markdown, main [class*="markdown"], main [class*="prose"]')) add(n.closest('article') || n);
          } else {
            for (const n of document.querySelectorAll('.ds-markdown, [class*="ds-markdown"], [class*="markdown"], [class*="prose"]')) add(n);
            for (const n of document.querySelectorAll('[class*="message"] [class*="content"], main [class*="message"], main [class*="answer"]')) add(n);
          }
          return [...set];
        }

        function extractText(node) {
          if (!node) return '';
          let source = node;
          if (isChatGPT) source = node.querySelector?.('[data-message-author-role="assistant"] .markdown, [data-message-author-role="assistant"] [class*="prose"], [data-message-author-role="assistant"], .markdown, [class*="prose"]') || node;
          const clone = source.cloneNode(true);
          for (const bad of clone.querySelectorAll('button,svg,script,style,textarea,input,[aria-hidden="true"],[class*="toolbar"],[class*="actions"],[class*="sr-only"]')) bad.remove();
          for (const pre of [...clone.querySelectorAll('pre')]) {
            const code = pre.querySelector('code');
            const raw = (((code || pre).textContent) || '').replace(/\n$/, '');
            if (!raw.trim()) { pre.remove(); continue; }
            const langClass = code ? [...code.classList].find((x) => x.startsWith('language-')) : '';
            const lang = (langClass || '').replace('language-', '');
            pre.replaceWith(document.createTextNode(`\n\`\`\`${lang}\n${raw}\n\`\`\`\n`));
          }
          return String(clone.innerText || clone.textContent || '').replace(/\u00a0/g, ' ').trim();
        }

        function snapshot() {
          return responseNodes().map((node) => ({ node, text: extractText(node) })).filter((x) => x.text);
        }

        function generating() {
          return [...document.querySelectorAll('button')].filter(visible).some((b) => {
            const l = labelOf(b);
            return b.matches('[data-testid="stop-button"], [data-testid*="stop" i]') || /stop generating|stop response|останов|停止生成|停止|中止|终止/i.test(l);
          });
        }

        const editor = await waitComposer();
        if (!editor) {
          if (loginScreen()) return { ok: false, code: 'provider_auth', message: `Нужно войти в ${providerName} в этом профиле Chrome.` };
          return { ok: false, code: 'composer_not_found', message: `Не найдено поле ввода ${providerName}. Возможно, интерфейс сайта изменился.` };
        }

        const baseline = new Set(snapshot().map((x) => norm(x.text)));
        const promptNorm = norm(text);
        let inserted = await setComposer(editor, text);
        if (!inserted || inserted.length < Math.min(8, promptNorm.length)) {
          // Retry after a click; some editors initialise only on pointer focus.
          try { editor.click(); } catch {}
          inserted = await setComposer(editor, text);
        }
        if (!inserted || inserted.length < Math.min(8, promptNorm.length)) {
          return { ok: false, code: 'composer_fill_failed', message: `${providerName}: не удалось вставить сообщение в поле ввода.` };
        }

        let send = findSendButton(editor);
        if (send) send.click();
        else {
          editor.focus();
          editor.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', code: 'Enter', keyCode: 13, which: 13, bubbles: true, cancelable: true }));
          editor.dispatchEvent(new KeyboardEvent('keyup', { key: 'Enter', code: 'Enter', keyCode: 13, which: 13, bubbles: true, cancelable: true }));
        }

        const sendDeadline = Date.now() + 8000;
        let confirmedSent = false;
        while (Date.now() < sendDeadline) {
          const current = composerText(editor);
          const hasNewResponse = snapshot().some((x) => !baseline.has(norm(x.text)) && norm(x.text) !== promptNorm);
          if (!current || generating() || hasNewResponse) { confirmedSent = true; break; }
          await sleep(160);
        }
        if (!confirmedSent) {
          // Last safe retry using the nearest form.
          const form = editor.closest?.('form');
          if (form?.requestSubmit) {
            try { form.requestSubmit(); } catch {}
            await sleep(700);
            confirmedSent = !composerText(editor) || generating() || snapshot().some((x) => !baseline.has(norm(x.text)) && norm(x.text) !== promptNorm);
          }
        }
        if (!confirmedSent) {
          return { ok: false, code: 'composer_send_failed', message: `${providerName} не принял сообщение. Запрос не был отправлен.` };
        }

        return await new Promise((resolve) => {
          let done = false;
          let last = '';
          let lastChange = 0;
          let sawBusy = false;
          const started = Date.now();
          const timeoutMs = 180000;

          const finish = (v) => {
            if (done) return;
            done = true;
            observer.disconnect();
            clearInterval(poll);
            clearTimeout(deadline);
            resolve(v);
          };

          const check = () => {
            if (done) return;
            const items = snapshot();
            let candidate = '';
            for (let i = items.length - 1; i >= 0; i--) {
              const t = norm(items[i].text);
              if (!t || baseline.has(t)) continue;
              if (t === promptNorm || promptNorm.includes(t) || t.includes(promptNorm)) continue;
              candidate = items[i].text;
              break;
            }
            const busy = generating();
            if (busy) sawBusy = true;
            if (candidate && candidate !== last) {
              last = candidate;
              lastChange = Date.now();
            }
            if (!last) return;
            const stableFor = Date.now() - lastChange;
            if (!busy && (sawBusy ? stableFor >= 300 : stableFor >= (isChatGPT ? 1100 : 1600))) {
              finish({ ok: true, text: last });
            }
          };

          const observer = new MutationObserver(check);
          observer.observe(document.documentElement || document.body, { childList: true, subtree: true, characterData: true });
          const poll = setInterval(check, 350);
          const deadline = setTimeout(() => {
            if (last) finish({ ok: true, text: last });
            else finish({ ok: false, code: 'provider_timeout', message: `${providerName} не вернул новый ответ за ${Math.round(timeoutMs / 60000)} минуты.` });
          }, timeoutMs);
          check();
        });
      },
    });
  } catch (err) {
    throw injectionError(provider, err);
  }

  const result = frames?.[0]?.result;
  if (!result) {
    throw codedError('provider_unreachable',
      `\u0421\u0442\u0440\u0430\u043D\u0438\u0446\u0430 ${provider.name} \u043D\u0435 \u043E\u0442\u0432\u0435\u0442\u0438\u043B\u0430. \u041F\u043E\u043F\u0440\u043E\u0431\u0443\u0439\u0442\u0435 \u0435\u0449\u0451 \u0440\u0430\u0437.`);
  }
  return result;
}

async function executeWebChat(model, messages, job) {
  const provider = PROVIDERS[model];
  if (!provider) throw codedError('bad_model_id', 'Неизвестная веб-модель.');
  const prompt = buildPrompt(messages);
  if (prompt.length > 120000) throw codedError('too_long', 'История чата слишком длинная для веб-режима. Создайте новый чат.');

  const previous = serviceLock;
  let release;
  serviceLock = new Promise((r) => { release = r; });
  await previous.catch(() => {});
  try {
    if (job.cancelled) throw codedError('cancelled', 'Генерация остановлена.');
    const tabId = await ensureServiceTab(model);
    job.tabId = tabId;
    job.model = model;
    if (job.cancelled) throw codedError('cancelled', 'Генерация остановлена.');

    // The only service tab is already active in its minimized window. Keep the
    // window minimized without repeatedly toggling tabs, which was what caused
    // v2.2 to visibly restore the service window on some Windows builds.
    const tab = await chrome.tabs.get(tabId).catch(() => null);
    if (tab?.windowId) await hideServiceWindow(tab.windowId);

    const result = await runProvider(tabId, model, prompt);
    if (!result?.ok) throw codedError(result?.code || 'provider_error', result?.message || `${provider.name} не вернул ответ.`);
    const out = typeof result.text === 'string' ? result.text.trim() : '';
    if (!out) throw codedError('empty_provider_response', `${provider.name} вернул пустой ответ.`);
    return out;
  } finally {
    if (job.tabId && job.model) resetServiceTab(job.model, job.tabId).catch(() => {});
    release();
  }
}

chrome.runtime.onConnect.addListener((port) => {
  if (port.name !== 'webai-chat') return;
  const senderTab = port.sender?.tab;
  const job = {
    cancelled: false,
    tabId: null,
    model: null,
    ownerTabId: senderTab?.id ?? null,
    ownerWindowId: senderTab?.windowId ?? null,
  };
  activeOwner = { tabId: job.ownerTabId, windowId: job.ownerWindowId };

  port.onDisconnect.addListener(() => {
    job.cancelled = true;
    if (activeOwner?.tabId === job.ownerTabId) activeOwner = null;
  });

  port.onMessage.addListener(async (msg) => {
    if (!msg || msg.type !== 'chat' || job.cancelled) return;
    activeOwner = { tabId: job.ownerTabId, windowId: job.ownerWindowId };
    try {
      const text = await executeWebChat(msg.model, msg.messages, job);
      if (!job.cancelled) port.postMessage({ type: 'done', text });
    } catch (err) {
      if (!job.cancelled && err?.code !== 'cancelled') {
        if (!err?.code) console.warn('[multi-ai-chat] unhandled failure:', err);
        port.postMessage({
          type: 'error',
          code: err?.code || 'provider_error',
          message: err?.code && err?.message
            ? err.message
            : 'Не удалось получить ответ от веб-нейросети. Попробуйте ещё раз.',
        });
      }
    } finally {
      if (activeOwner?.tabId === job.ownerTabId) activeOwner = null;
      refocusOwner().catch(() => {});
    }
  });
});
