'use strict';

const DEFAULTS = {
  theme: 'auto',
  detail: 'normal',
  extraInstruction: '',
  systemNotification: true
};

const els = {
  theme: document.getElementById('theme'),
  detail: document.getElementById('detail'),
  extraInstruction: document.getElementById('extraInstruction'),
  systemNotification: document.getElementById('systemNotification'),
  preview: document.getElementById('preview'),
  status: document.getElementById('status')
};

let statusTimer = null;

function setStatus(text, kind) {
  els.status.textContent = text;
  els.status.className = 'fxg-status' + (kind ? ' fxg-status--' + kind : '');
  clearTimeout(statusTimer);
  if (kind === 'ok') statusTimer = setTimeout(() => setStatus(''), 2000);
}

function applyTheme(theme) {
  document.documentElement.dataset.theme = theme || 'auto';
}

// Упрощённый пример — фактический промпт собирает content.js по тем же правилам.
function renderPreview() {
  const detail = els.detail.value;
  const lines = [
    'Помоги мне разобраться с заданием с образовательной платформы Фоксфорд.',
    '',
    'Тема / страница: Алгебра, 8 класс',
    '',
    'Условие задания:',
    '"""',
    'Решите уравнение x² − 5x + 6 = 0.',
    '"""',
    '',
    'Варианты ответа:',
    '1) 2 и 3',
    '2) −2 и −3',
    '3) 1 и 6',
    '',
    'Что нужно сделать:'
  ];
  if (detail === 'short') {
    lines.push('1. Коротко объясни, как решать задание.', '2. Назови предполагаемый правильный ответ (номер и текст варианта).', '3. Оцени уверенность: высокая, средняя или низкая.');
  } else {
    lines.push(
      '1. Сформулируй своими словами, что спрашивается в задании.',
      '2. Реши задание пошагово и объясни каждый шаг простыми словами, как школьнику.',
      '3. Назови предполагаемый правильный ответ (номер и текст варианта).',
      '4. Оцени уверенность в ответе (высокая, средняя или низкая) и объясни почему.'
    );
    if (detail === 'detailed') {
      lines.push('5. Укажи типичные ошибки в таком задании и какое правило или формулу стоит запомнить.', '6. Предложи похожее задание для самопроверки (без ответа).');
    }
  }
  const extra = els.extraInstruction.value.trim();
  if (extra) lines.push('', 'Дополнительно: ' + extra);
  lines.push('', 'Отвечай на русском языке.');
  els.preview.textContent = lines.join('\n');
}

async function load() {
  const stored = await chrome.storage.local.get(DEFAULTS);
  els.theme.value = stored.theme;
  els.detail.value = stored.detail;
  els.extraInstruction.value = stored.extraInstruction;
  els.systemNotification.checked = Boolean(stored.systemNotification);
  applyTheme(stored.theme);
  renderPreview();
}

async function save() {
  await chrome.storage.local.set({
    theme: els.theme.value,
    detail: els.detail.value,
    extraInstruction: els.extraInstruction.value.trim().slice(0, 500),
    systemNotification: els.systemNotification.checked
  });
  applyTheme(els.theme.value);
  renderPreview();
  setStatus('Настройки сохранены.', 'ok');
}

[els.theme, els.detail, els.systemNotification].forEach((el) => el.addEventListener('change', save));

let typingTimer = null;
els.extraInstruction.addEventListener('input', () => {
  renderPreview();
  clearTimeout(typingTimer);
  typingTimer = setTimeout(save, 500);
});

load().catch((e) => setStatus('Не удалось загрузить настройки: ' + e.message, 'error'));
