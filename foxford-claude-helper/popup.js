'use strict';

const statusEl = document.getElementById('status');
const themeEl = document.getElementById('theme');

function setStatus(text, kind) {
  statusEl.textContent = text;
  statusEl.className = 'fxc-status' + (kind ? ' fxc-status--' + kind : '');
}

function applyTheme(theme) {
  document.documentElement.dataset.theme = theme || 'auto';
}

async function activeTab() {
  const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
  return tab;
}

async function init() {
  const stored = await chrome.storage.local.get({ apiKey: '', theme: 'auto' });
  themeEl.value = stored.theme;
  applyTheme(stored.theme);

  const tab = await activeTab();
  const onFoxford = Boolean(tab?.url && /^https:\/\/([a-z0-9-]+\.)?foxford\.ru\//i.test(tab.url));

  if (!stored.apiKey) {
    setStatus('API-ключ не задан — откройте настройки.', 'warn');
  } else if (!onFoxford) {
    setStatus('Откройте страницу задания на foxford.ru.', 'warn');
  } else {
    setStatus('Готово к работе.', 'ok');
  }

  document.getElementById('analyze').disabled = !onFoxford;
}

themeEl.addEventListener('change', async () => {
  await chrome.storage.local.set({ theme: themeEl.value });
  applyTheme(themeEl.value);
});

document.getElementById('options').addEventListener('click', () => {
  chrome.runtime.openOptionsPage();
  window.close();
});

document.getElementById('analyze').addEventListener('click', async () => {
  const tab = await activeTab();
  if (!tab) return;
  chrome.tabs.sendMessage(tab.id, { type: 'RUN_ANALYSIS' }, () => {
    if (chrome.runtime.lastError) {
      setStatus('Страница не готова — обновите её (F5) и попробуйте снова.', 'warn');
      return;
    }
    window.close();
  });
});

init().catch((e) => setStatus('Ошибка: ' + e.message, 'error'));
