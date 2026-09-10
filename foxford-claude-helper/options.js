'use strict';

const DEFAULTS = {
  apiKey: '',
  model: 'claude-sonnet-5',
  maxTokens: 1200,
  theme: 'auto'
};

const els = {
  apiKey: document.getElementById('apiKey'),
  model: document.getElementById('model'),
  maxTokens: document.getElementById('maxTokens'),
  theme: document.getElementById('theme'),
  status: document.getElementById('status')
};

function setStatus(text, kind) {
  els.status.textContent = text;
  els.status.className = 'fxc-status' + (kind ? ' fxc-status--' + kind : '');
}

function applyTheme(theme) {
  document.documentElement.dataset.theme = theme || 'auto';
}

async function load() {
  const stored = await chrome.storage.local.get(DEFAULTS);
  els.apiKey.value = stored.apiKey || '';
  els.model.value = stored.model || DEFAULTS.model;
  els.maxTokens.value = stored.maxTokens || DEFAULTS.maxTokens;
  els.theme.value = stored.theme || DEFAULTS.theme;
  applyTheme(els.theme.value);
  if (!stored.apiKey) setStatus('Ключ ещё не сохранён.', 'warn');
}

function readMaxTokens() {
  const value = parseInt(els.maxTokens.value, 10);
  if (!isFinite(value)) return DEFAULTS.maxTokens;
  return Math.max(256, Math.min(8000, value));
}

async function save() {
  const apiKey = els.apiKey.value.trim();
  if (apiKey && !/^sk-ant-[\w-]{10,}$/.test(apiKey)) {
    setStatus('Ключ выглядит некорректно: обычно он начинается с «sk-ant-».', 'warn');
  }
  const maxTokens = readMaxTokens();
  els.maxTokens.value = maxTokens;
  await chrome.storage.local.set({
    apiKey,
    model: els.model.value,
    maxTokens,
    theme: els.theme.value
  });
  applyTheme(els.theme.value);
  if (apiKey) setStatus('Настройки сохранены.', 'ok');
}

document.getElementById('save').addEventListener('click', save);

document.getElementById('toggleKey').addEventListener('click', (event) => {
  const hidden = els.apiKey.type === 'password';
  els.apiKey.type = hidden ? 'text' : 'password';
  event.currentTarget.textContent = hidden ? 'Скрыть' : 'Показать';
});

document.getElementById('clear').addEventListener('click', async () => {
  els.apiKey.value = '';
  await chrome.storage.local.set({ apiKey: '' });
  setStatus('Ключ удалён из хранилища браузера.', 'warn');
});

document.getElementById('test').addEventListener('click', async () => {
  const apiKey = els.apiKey.value.trim();
  if (!apiKey) {
    setStatus('Сначала введите ключ.', 'warn');
    return;
  }
  setStatus('Проверяю ключ…');
  chrome.runtime.sendMessage({ type: 'TEST_KEY', apiKey, model: els.model.value }, (response) => {
    if (chrome.runtime.lastError) {
      setStatus('Ошибка связи с фоновым скриптом: ' + chrome.runtime.lastError.message, 'error');
      return;
    }
    setStatus(response.message, response.ok ? 'ok' : 'error');
  });
});

[els.model, els.maxTokens, els.theme].forEach((el) => el.addEventListener('change', save));

load().catch((e) => setStatus('Не удалось загрузить настройки: ' + e.message, 'error'));
