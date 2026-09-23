/**
 * Content script для foxford.ru.
 * Считывает задание, собирает промпт на русском, копирует его в буфер обмена
 * и просит фоновый скрипт открыть https://chatgpt.com/.
 * Никаких запросов к API, cookies или токенов. Кнопку сдачи задания не трогает.
 */
(function () {
  'use strict';

  if (window.__fxgHelperLoaded) return;
  window.__fxgHelperLoaded = true;

  const ROOT_ID = 'fxg-root';
  const OPTION_MARK = 'data-fxg-option';
  const NOTICE_TEXT = 'Задание скопировано — вставьте его в ChatGPT';

  const settings = {
    theme: 'auto',
    detail: 'normal',
    extraInstruction: ''
  };

  /* ---------- разбор задания ---------- */

  const TASK_SELECTORS = [
    '[data-testid*="task"]',
    '[class*="task-content"]',
    '[class*="TaskContent"]',
    '[class*="question"]',
    '[class*="Question"]',
    '[class*="exercise"]',
    '[class*="Exercise"]',
    '[class*="problem"]',
    '.lesson-task',
    'main article',
    'main'
  ];

  const OPTION_SELECTORS = [
    'label:has(input[type="radio"])',
    'label:has(input[type="checkbox"])',
    '[role="radio"]',
    '[role="checkbox"]',
    '[class*="option"]',
    '[class*="Option"]',
    '[class*="answer-variant"]',
    '[class*="variant"]',
    'li'
  ];

  // Всегда лишнее: служебные узлы и наш собственный интерфейс
  const NOISE_SELECTORS = ['#' + ROOT_ID, 'script', 'style', 'noscript', 'template', 'svg'].join(', ');
  // Лишнее для условия задания: кнопки «Ответить»/«Проверить», навигация и т. п.
  const CONTROL_SELECTORS = ['button', '[role="button"]', 'select', 'nav', 'header', 'footer', 'aside'].join(', ');
  const BLANK_SELECTORS = 'input[type="text"], input[type="number"], input:not([type]), textarea';

  function isVisible(el) {
    if (!el || !el.getBoundingClientRect) return false;
    const rect = el.getBoundingClientRect();
    if (rect.width < 2 || rect.height < 2) return false;
    const style = getComputedStyle(el);
    return style.visibility !== 'hidden' && style.display !== 'none' && style.opacity !== '0';
  }

  function normalize(text) {
    return String(text || '')
      .replace(/[ \t ]+/g, ' ')
      .replace(/ *\n */g, '\n')
      .replace(/\n{3,}/g, '\n\n')
      .trim();
  }

  function textOf(node, stripControls) {
    if (!node) return '';
    const clone = node.cloneNode(true);
    clone.querySelectorAll(NOISE_SELECTORS).forEach((n) => n.remove());
    if (stripControls) {
      // поле для ответа в условии показываем как пропуск
      clone.querySelectorAll(BLANK_SELECTORS).forEach((n) => n.replaceWith(' ____ '));
      clone.querySelectorAll(CONTROL_SELECTORS).forEach((n) => n.remove());
    }
    clone.querySelectorAll('input').forEach((n) => n.remove());
    // innerText недоступен у отсоединённого клона, поэтому переносы строк
    // восстанавливаем по блочным элементам
    clone.querySelectorAll('br').forEach((br) => br.replaceWith('\n'));
    clone.querySelectorAll('p, div, li, tr, h1, h2, h3, h4, h5, h6').forEach((b) => b.append('\n'));
    return normalize(clone.textContent);
  }

  function safeQueryAll(root, selector) {
    try {
      return Array.from(root.querySelectorAll(selector));
    } catch (e) {
      return []; // например, :has() в старых движках
    }
  }

  function findTaskContainer() {
    for (const selector of TASK_SELECTORS) {
      // самый компактный видимый узел, в котором есть осмысленный текст
      const scored = safeQueryAll(document, selector)
        .filter((el) => isVisible(el) && !el.closest('#' + ROOT_ID))
        .map((el) => ({ el, len: textOf(el, true).length }))
        .filter((c) => c.len > 60)
        .sort((a, b) => a.len - b.len);
      if (scored.length) return scored[0].el;
    }
    return document.body;
  }

  function findOptionElements(container) {
    for (const selector of OPTION_SELECTORS) {
      const seen = new Set();
      const found = [];
      for (const el of safeQueryAll(container, selector)) {
        if (!isVisible(el)) continue;
        // вложенный вариант уже учтён через родителя
        if (found.some((f) => f.contains(el) || el.contains(f))) continue;
        const text = textOf(el);
        if (!text || text.length > 400) continue;
        const key = text.toLowerCase();
        if (seen.has(key)) continue;
        seen.add(key);
        found.push(el);
      }
      if (found.length >= 2) return found.slice(0, 12);
    }
    return [];
  }

  function hasImages(container) {
    return safeQueryAll(container, 'img, canvas, svg, picture, [class*="image"]').some((el) => {
      if (!isVisible(el)) return false;
      const rect = el.getBoundingClientRect();
      return rect.width > 60 && rect.height > 40;
    });
  }

  function extractTask() {
    const selection = normalize(window.getSelection ? String(window.getSelection()) : '');
    if (selection.length > 20) {
      return { question: selection, options: [], hasImage: false, fromSelection: true, pageTitle: document.title };
    }

    const container = findTaskContainer();
    const optionEls = findOptionElements(container);
    const options = optionEls.map((el) => textOf(el));

    // условие = текст контейнера без вариантов ответа, чтобы они не дублировались
    optionEls.forEach((el) => el.setAttribute(OPTION_MARK, ''));
    const clone = container.cloneNode(true);
    optionEls.forEach((el) => el.removeAttribute(OPTION_MARK));
    clone.querySelectorAll('[' + OPTION_MARK + ']').forEach((n) => n.remove());

    let question = textOf(clone, true);
    if (question.length > 6000) question = question.slice(0, 6000) + '…';

    return {
      question,
      options,
      hasImage: hasImages(container),
      fromSelection: false,
      pageTitle: normalize(document.title.replace(/\s*[|—–-]\s*Фоксфорд.*$/i, ''))
    };
  }

  /* ---------- промпт ---------- */

  function buildPrompt(task) {
    const lines = [];
    lines.push('Помоги мне разобраться с заданием с образовательной платформы Фоксфорд.');
    if (task.pageTitle) lines.push('', 'Тема / страница: ' + task.pageTitle);

    lines.push('', 'Условие задания:', '"""', task.question || '(текст не распознан)', '"""');

    if (task.options.length) {
      lines.push('', 'Варианты ответа:');
      task.options.forEach((opt, i) => lines.push(`${i + 1}) ${opt.replace(/\n+/g, ' ')}`));
    }

    if (task.hasImage) {
      lines.push('', 'В задании есть рисунок, график или формула, которые могли не попасть в текст. Если без них решить нельзя — скажи, и я пришлю скриншот.');
    }

    lines.push('', 'Что нужно сделать:');
    if (settings.detail === 'short') {
      lines.push(
        '1. Коротко объясни, как решать задание.',
        '2. Назови предполагаемый правильный ответ' + (task.options.length ? ' (номер и текст варианта).' : '.'),
        '3. Оцени уверенность: высокая, средняя или низкая.'
      );
    } else {
      lines.push(
        '1. Сформулируй своими словами, что спрашивается в задании.',
        '2. Реши задание пошагово и объясни каждый шаг простыми словами, как школьнику.',
        '3. Назови предполагаемый правильный ответ' + (task.options.length ? ' (номер и текст варианта).' : '.'),
        '4. Оцени уверенность в ответе (высокая, средняя или низкая) и объясни почему.'
      );
      if (settings.detail === 'detailed') {
        lines.push(
          '5. Укажи типичные ошибки в таком задании и какое правило или формулу стоит запомнить.',
          '6. Предложи похожее задание для самопроверки (без ответа).'
        );
      }
    }

    const extra = normalize(settings.extraInstruction);
    if (extra) lines.push('', 'Дополнительно: ' + extra);

    lines.push('', 'Отвечай на русском языке.');
    return lines.join('\n');
  }

  /* ---------- буфер обмена ---------- */

  async function copyText(text) {
    try {
      if (navigator.clipboard && window.isSecureContext) {
        await navigator.clipboard.writeText(text);
        return true;
      }
    } catch (e) {
      // упадём в запасной вариант ниже
    }
    const area = document.createElement('textarea');
    area.value = text;
    area.setAttribute('readonly', '');
    area.style.cssText = 'position:fixed;top:-1000px;left:-1000px;opacity:0;';
    document.body.appendChild(area);
    area.select();
    let ok = false;
    try {
      ok = document.execCommand('copy');
    } catch (e) {
      ok = false;
    }
    area.remove();
    return ok;
  }

  /* ---------- интерфейс ---------- */

  const root = document.createElement('div');
  root.id = ROOT_ID;
  root.innerHTML = `
    <div class="fxg-dock" role="group" aria-label="Разбор задания с ChatGPT">
      <button class="fxg-btn fxg-btn--primary fxg-send" type="button" title="Скопировать задание и открыть ChatGPT">
        <span class="fxg-btn__icon" aria-hidden="true">💬</span>
        <span class="fxg-btn__label">Разобрать с ChatGPT</span>
      </button>
      <button class="fxg-btn fxg-btn--secondary fxg-copy" type="button" title="Только скопировать промпт в буфер обмена">
        <span class="fxg-btn__icon" aria-hidden="true">📋</span>
        <span class="fxg-btn__label">Скопировать задание</span>
      </button>
    </div>
    <div class="fxg-toasts" aria-live="polite"></div>
  `;

  function mount() {
    (document.body || document.documentElement).appendChild(root);
  }
  if (document.body) mount(); else document.addEventListener('DOMContentLoaded', mount, { once: true });

  const toasts = root.querySelector('.fxg-toasts');
  const sendBtn = root.querySelector('.fxg-send');
  const copyBtn = root.querySelector('.fxg-copy');

  function applyTheme(theme) {
    settings.theme = theme || 'auto';
    root.dataset.theme = settings.theme;
  }

  function showToast(title, text, kind, timeout) {
    const toast = document.createElement('div');
    toast.className = 'fxg-toast fxg-toast--' + (kind || 'ok');
    toast.setAttribute('role', kind === 'error' ? 'alert' : 'status');

    const head = document.createElement('strong');
    head.textContent = title;
    toast.appendChild(head);
    if (text) {
      const body = document.createElement('span');
      body.textContent = text;
      toast.appendChild(body);
    }
    const close = document.createElement('button');
    close.type = 'button';
    close.className = 'fxg-toast__close';
    close.setAttribute('aria-label', 'Закрыть');
    close.textContent = '✕';
    close.addEventListener('click', () => toast.remove());
    toast.appendChild(close);

    toasts.appendChild(toast);
    while (toasts.children.length > 3) toasts.firstElementChild.remove();
    setTimeout(() => toast.classList.add('fxg-toast--leaving'), (timeout || 6000) - 300);
    setTimeout(() => toast.remove(), timeout || 6000);
  }

  function sendMessage(message) {
    return new Promise((resolve) => {
      try {
        chrome.runtime.sendMessage(message, (response) => {
          if (chrome.runtime.lastError) {
            resolve({ ok: false, message: chrome.runtime.lastError.message });
            return;
          }
          resolve(response || { ok: false, message: 'Пустой ответ фонового скрипта.' });
        });
      } catch (e) {
        // например, расширение перезагрузили, а страницу — нет
        resolve({ ok: false, message: 'Обновите страницу (F5): ' + e.message });
      }
    });
  }

  function prepare() {
    const task = extractTask();
    if (!task.question || task.question.length < 10) {
      showToast('Задание не найдено', 'Выделите текст задания мышью и нажмите кнопку ещё раз.', 'error');
      return null;
    }
    return { task, prompt: buildPrompt(task) };
  }

  let busy = false;

  async function handle(openChatGpt) {
    if (busy) return;
    busy = true;
    try {
      const prepared = prepare();
      if (!prepared) return;

      // копируем ДО открытия вкладки: после переключения фокуса запись в буфер запрещена
      const copied = await copyText(prepared.prompt);
      if (!copied) {
        showToast('Не удалось скопировать', 'Браузер запретил доступ к буферу обмена. Разрешите его для сайта и попробуйте снова.', 'error', 8000);
        return;
      }

      const hint = prepared.task.options.length
        ? `Найдено вариантов ответа: ${prepared.task.options.length}.`
        : prepared.task.fromSelection ? 'Использован выделенный текст.' : 'Варианты ответа не найдены — проверьте текст перед отправкой.';

      if (openChatGpt) {
        showToast('Открываю ChatGPT', 'Расширение вставит задание в чат. ' + hint + ' Если хотите — вставьте вручную (Ctrl+V).', 'ok', 8000);
        // промпт передаём фоновому скрипту: он сохранит его и откроет вкладку,
        // где content-script вставит текст в поле чата
        const response = await sendMessage({ type: 'OPEN_CHATGPT', prompt: prepared.prompt });
        if (!response.ok) {
          showToast('Не удалось открыть ChatGPT', (response.message || '') + ' Откройте chatgpt.com вручную — задание уже в буфере.', 'error', 8000);
        }
      } else {
        showToast('Задание скопировано', hint, 'ok');
      }
    } catch (e) {
      showToast('Сбой расширения', e.message, 'error');
    } finally {
      busy = false;
    }
  }

  sendBtn.addEventListener('click', () => handle(true));
  copyBtn.addEventListener('click', () => handle(false));

  /* ---------- настройки и связь с popup ---------- */

  chrome.storage.local.get({ theme: 'auto', detail: 'normal', extraInstruction: '' }, (data) => {
    settings.detail = data.detail;
    settings.extraInstruction = data.extraInstruction;
    applyTheme(data.theme);
  });

  chrome.storage.onChanged.addListener((changes, area) => {
    if (area !== 'local') return;
    if (changes.theme) applyTheme(changes.theme.newValue);
    if (changes.detail) settings.detail = changes.detail.newValue;
    if (changes.extraInstruction) settings.extraInstruction = changes.extraInstruction.newValue;
  });

  chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
    if (message?.type === 'GET_PROMPT') {
      const task = extractTask();
      const found = Boolean(task.question && task.question.length >= 10);
      sendResponse({ ok: found, prompt: found ? buildPrompt(task) : '', optionsCount: task.options.length });
    } else if (message?.type === 'SHOW_NOTICE') {
      showToast(message.title || NOTICE_TEXT, message.text || '', message.kind || 'ok');
      sendResponse({ ok: true });
    }
    // синхронный ответ — канал держать открытым не нужно
    return false;
  });

  applyTheme('auto');
})();
