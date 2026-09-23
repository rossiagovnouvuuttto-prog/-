'use strict';

const FOXFORD_RE = /^https:\/\/([a-z0-9-]+\.)*foxford\.ru\//i;

const statusEl = document.getElementById('status');
const themeEl = document.getElementById('theme');
const sendBtn = document.getElementById('send');
const copyBtn = document.getElementById('copy');

function setStatus(text, kind) {
  statusEl.textContent = text;
  statusEl.className = 'fxg-status' + (kind ? ' fxg-status--' + kind : '');
}

function applyTheme(theme) {
  document.documentElement.dataset.theme = theme || 'auto';
}

async function activeTab() {
  const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
  return tab;
}

function sendToTab(tabId, message) {
  return new Promise((resolve) => {
    chrome.tabs.sendMessage(tabId, message, (response) => {
      if (chrome.runtime.lastError) {
        resolve({ ok: false, message: chrome.runtime.lastError.message, unreachable: true });
        return;
      }
      resolve(response || { ok: false });
    });
  });
}

async function run(openChatGpt) {
  const tab = await activeTab();
  if (!tab) return;

  sendBtn.disabled = copyBtn.disabled = true;
  try {
    const response = await sendToTab(tab.id, { type: 'GET_PROMPT' });
    if (response.unreachable) {
      setStatus('Страница не готова — обновите её (F5) и попробуйте снова.', 'warn');
      return;
    }
    if (!response.ok || !response.prompt) {
      setStatus('Задание не найдено. Выделите его текст мышью и повторите.', 'warn');
      return;
    }

    // popup сейчас в фокусе, поэтому копируем именно отсюда
    await navigator.clipboard.writeText(response.prompt);

    if (!openChatGpt) {
      setStatus('Задание скопировано в буфер обмена.', 'ok');
      await sendToTab(tab.id, { type: 'SHOW_NOTICE', title: 'Задание скопировано' });
      return;
    }

    await sendToTab(tab.id, {
      type: 'SHOW_NOTICE',
      title: 'Открываю ChatGPT',
      text: 'Расширение вставит задание в чат.'
    });
    // вкладку открывает фоновый скрипт; промпт он передаст content-script'у ChatGPT
    await chrome.runtime.sendMessage({
      type: 'OPEN_CHATGPT',
      prompt: response.prompt,
      sourceTab: { index: tab.index, windowId: tab.windowId }
    });
    window.close();
  } catch (e) {
    setStatus('Не удалось скопировать: ' + e.message, 'error');
  } finally {
    sendBtn.disabled = copyBtn.disabled = false;
  }
}

async function init() {
  const stored = await chrome.storage.local.get({ theme: 'auto' });
  themeEl.value = stored.theme;
  applyTheme(stored.theme);

  const tab = await activeTab();
  const onFoxford = Boolean(tab?.url && FOXFORD_RE.test(tab.url));
  sendBtn.disabled = copyBtn.disabled = !onFoxford;
  if (onFoxford) {
    setStatus('Откройте задание и нажмите кнопку ниже.', 'ok');
  } else {
    setStatus('Расширение работает только на foxford.ru. Откройте страницу с заданием.', 'warn');
  }
}

themeEl.addEventListener('change', async () => {
  await chrome.storage.local.set({ theme: themeEl.value });
  applyTheme(themeEl.value);
});

sendBtn.addEventListener('click', () => run(true));
copyBtn.addEventListener('click', () => run(false));

document.getElementById('options').addEventListener('click', () => {
  chrome.runtime.openOptionsPage();
  window.close();
});

init().catch((e) => setStatus('Ошибка: ' + e.message, 'error'));
