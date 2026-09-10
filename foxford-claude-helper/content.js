/**
 * Content script: плавающая кнопка, разбор задания и панель с ответом.
 * Работает только на foxford.ru (см. manifest.json → content_scripts.matches).
 */
(function () {
  'use strict';

  if (window.__foxfordClaudeHelperLoaded) return;
  window.__foxfordClaudeHelperLoaded = true;

  const ROOT_ID = 'fxc-root';
  const state = {
    lastPayload: null,
    lastResult: null,
    busy: false,
    theme: 'auto'
  };

  /* ---------- разбор задания ---------- */

  const QUESTION_SELECTORS = [
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
    '[class*="option"]',
    '[class*="Option"]',
    '[class*="answer-variant"]',
    '[class*="variant"]',
    'li'
  ];

  function isVisible(el) {
    if (!el || !el.getBoundingClientRect) return false;
    const rect = el.getBoundingClientRect();
    if (rect.width < 2 || rect.height < 2) return false;
    const style = getComputedStyle(el);
    return style.visibility !== 'hidden' && style.display !== 'none' && style.opacity !== '0';
  }

  function cleanText(node) {
    if (!node) return '';
    const clone = node.cloneNode(true);
    clone.querySelectorAll('#' + ROOT_ID + ', script, style, noscript, svg').forEach((n) => n.remove());
    return clone.textContent.replace(/[ \t ]+/g, ' ').replace(/\n{3,}/g, '\n\n').trim();
  }

  function safeQueryAll(root, selector) {
    try {
      return Array.from(root.querySelectorAll(selector));
    } catch (e) {
      return []; // например, :has() в старых движках
    }
  }

  function findTaskContainer() {
    for (const selector of QUESTION_SELECTORS) {
      const candidates = safeQueryAll(document, selector).filter(isVisible);
      // берём самый "содержательный", но не гигантский узел
      // самый компактный узел, в котором всё ещё есть осмысленный текст
      const scored = candidates
        .map((el) => ({ el, len: cleanText(el).length }))
        .filter((c) => c.len > 60)
        .sort((a, b) => a.len - b.len);
      if (scored.length) return scored[0].el;
    }
    return document.body;
  }

  function collectOptions(container) {
    const found = [];
    const seen = new Set();
    for (const selector of OPTION_SELECTORS) {
      for (const el of safeQueryAll(container, selector)) {
        if (!isVisible(el)) continue;
        const text = cleanText(el);
        if (!text || text.length > 400) continue;
        const key = text.toLowerCase();
        if (seen.has(key)) continue;
        seen.add(key);
        found.push(text);
      }
      if (found.length >= 2) break;
    }
    return found.slice(0, 12);
  }

  function hasImages(container) {
    const imgs = safeQueryAll(container, 'img, canvas, svg, [class*="image"]').filter(isVisible);
    return imgs.some((el) => {
      const rect = el.getBoundingClientRect();
      return rect.width > 60 && rect.height > 40;
    });
  }

  function extractTask() {
    const container = findTaskContainer();
    const options = collectOptions(container);
    let text = cleanText(container);
    if (text.length > 6000) text = text.slice(0, 6000) + '…';

    const selection = String(window.getSelection ? window.getSelection() : '').trim();
    if (selection.length > 20) text = selection;

    return {
      question: text,
      options,
      pageTitle: document.title,
      url: location.href,
      hasImage: hasImages(container)
    };
  }

  /* ---------- интерфейс ---------- */

  const root = document.createElement('div');
  root.id = ROOT_ID;
  root.innerHTML = `
    <button class="fxc-fab" type="button" title="Разобрать задание с помощью Claude AI">
      <span class="fxc-fab__spark">✦</span>
      <span class="fxc-fab__label">Claude AI</span>
    </button>
    <aside class="fxc-panel" hidden aria-live="polite">
      <header class="fxc-panel__head">
        <span class="fxc-panel__title">Claude AI · разбор задания</span>
        <div class="fxc-panel__tools">
          <button class="fxc-icon-btn fxc-theme" type="button" title="Тема оформления">◐</button>
          <button class="fxc-icon-btn fxc-close" type="button" title="Закрыть">✕</button>
        </div>
      </header>
      <div class="fxc-panel__body"></div>
      <footer class="fxc-panel__foot">
        <label class="fxc-check"><input type="checkbox" class="fxc-shot"> Прикрепить снимок экрана</label>
        <button class="fxc-btn fxc-btn--ghost fxc-more" type="button" disabled>Объяснить подробнее</button>
        <button class="fxc-btn fxc-analyze" type="button">Анализировать</button>
      </footer>
      <p class="fxc-note">Расширение никогда не нажимает кнопку отправки задания — решение остаётся за вами.</p>
    </aside>
  `;

  function mount() {
    (document.body || document.documentElement).appendChild(root);
  }
  if (document.body) mount(); else document.addEventListener('DOMContentLoaded', mount, { once: true });

  const fab = root.querySelector('.fxc-fab');
  const panel = root.querySelector('.fxc-panel');
  const body = root.querySelector('.fxc-panel__body');
  const shotBox = root.querySelector('.fxc-shot');
  const moreBtn = root.querySelector('.fxc-more');
  const analyzeBtn = root.querySelector('.fxc-analyze');

  function applyTheme(theme) {
    state.theme = theme || 'auto';
    root.dataset.theme = state.theme;
  }

  function escapeHtml(value) {
    return String(value == null ? '' : value)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }

  function renderMessage(html, kind) {
    body.innerHTML = `<div class="fxc-msg fxc-msg--${kind || 'info'}">${html}</div>`;
  }

  function renderLoading(text) {
    body.innerHTML = `<div class="fxc-loading"><span class="fxc-spinner"></span>${escapeHtml(text)}</div>`;
  }

  function confidenceView(value) {
    const num = typeof value === 'number' ? value : parseFloat(value);
    if (!isFinite(num)) return { percent: null, label: 'неизвестна', level: 'low' };
    const percent = Math.round(Math.max(0, Math.min(1, num > 1 ? num / 100 : num)) * 100);
    const level = percent >= 80 ? 'high' : percent >= 55 ? 'mid' : 'low';
    const label = level === 'high' ? 'высокая' : level === 'mid' ? 'средняя' : 'низкая';
    return { percent, label, level };
  }

  function renderResult(result, extra) {
    const conf = confidenceView(result.confidence);
    body.innerHTML = `
      ${result.question ? `<section class="fxc-block"><h3>Вопрос</h3><p>${escapeHtml(result.question)}</p></section>` : ''}
      <section class="fxc-block fxc-block--answer">
        <h3>Предполагаемый ответ</h3>
        <p class="fxc-answer">${escapeHtml(result.answer) || '—'}</p>
      </section>
      <section class="fxc-block">
        <h3>Объяснение простыми словами</h3>
        <p>${escapeHtml(result.explanation).replace(/\n/g, '<br>') || '—'}</p>
      </section>
      <section class="fxc-block">
        <h3>Уверенность</h3>
        <div class="fxc-conf fxc-conf--${conf.level}">
          <div class="fxc-conf__bar"><span style="width:${conf.percent == null ? 0 : conf.percent}%"></span></div>
          <span class="fxc-conf__text">${conf.percent == null ? 'неизвестна' : conf.percent + '% · ' + conf.label}</span>
        </div>
        ${result.confidence_reason ? `<p class="fxc-conf__reason">${escapeHtml(result.confidence_reason)}</p>` : ''}
      </section>
      ${extra ? `<section class="fxc-block fxc-block--extra"><h3>Подробное объяснение</h3><p>${escapeHtml(extra).replace(/\n/g, '<br>')}</p></section>` : ''}
      <p class="fxc-disclaimer">Ответ сгенерирован ИИ и может быть неверным. Проверьте его перед отправкой.</p>
    `;
    moreBtn.disabled = false;
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
        resolve({ ok: false, message: e.message });
      }
    });
  }

  async function captureScreenshot() {
    root.classList.add('fxc-hidden-for-shot');
    await new Promise((r) => requestAnimationFrame(() => requestAnimationFrame(r)));
    const response = await sendMessage({ type: 'CAPTURE_TAB' });
    root.classList.remove('fxc-hidden-for-shot');
    if (!response.ok) throw new Error(response.message || 'Не удалось сделать снимок.');
    return response.dataUrl;
  }

  async function run(followUp) {
    if (state.busy) return;
    state.busy = true;
    analyzeBtn.disabled = true;
    moreBtn.disabled = true;

    try {
      const task = followUp && state.lastPayload ? state.lastPayload : extractTask();
      const payload = { ...task, followUp: Boolean(followUp) };

      if (shotBox.checked) {
        renderLoading('Делаю снимок видимой области…');
        try {
          payload.screenshot = await captureScreenshot();
        } catch (e) {
          renderMessage('Снимок не удался: ' + escapeHtml(e.message) + '. Отправляю только текст.', 'warn');
        }
      }

      state.lastPayload = { ...payload, screenshot: undefined };
      renderLoading(followUp ? 'Готовлю подробное объяснение…' : 'Анализирую задание…');

      const response = await sendMessage({ type: 'ASK_CLAUDE', payload });
      if (!response.ok) {
        const action = response.error === 'NO_KEY'
          ? '<button class="fxc-btn fxc-open-options" type="button">Открыть настройки</button>'
          : '';
        renderMessage(escapeHtml(response.message || 'Неизвестная ошибка.') + action, 'error');
        const openBtn = body.querySelector('.fxc-open-options');
        if (openBtn) openBtn.addEventListener('click', () => sendMessage({ type: 'OPEN_OPTIONS' }));
        return;
      }

      if (followUp && state.lastResult) {
        renderResult(state.lastResult, response.result.explanation || response.rawText);
      } else {
        state.lastResult = response.result;
        renderResult(response.result, null);
      }
    } catch (e) {
      renderMessage('Сбой расширения: ' + escapeHtml(e.message), 'error');
    } finally {
      state.busy = false;
      analyzeBtn.disabled = false;
    }
  }

  function openPanel() {
    panel.hidden = false;
    root.classList.add('fxc-open');
  }

  fab.addEventListener('click', () => {
    if (panel.hidden) {
      openPanel();
      const task = extractTask();
      shotBox.checked = task.hasImage;
      run(false);
    } else {
      panel.hidden = true;
      root.classList.remove('fxc-open');
    }
  });

  root.querySelector('.fxc-close').addEventListener('click', () => {
    panel.hidden = true;
    root.classList.remove('fxc-open');
  });

  root.querySelector('.fxc-theme').addEventListener('click', () => {
    const order = ['auto', 'light', 'dark'];
    const next = order[(order.indexOf(state.theme) + 1) % order.length];
    applyTheme(next);
    chrome.storage.local.set({ theme: next });
  });

  analyzeBtn.addEventListener('click', () => run(false));
  moreBtn.addEventListener('click', () => run(true));

  chrome.storage.local.get({ theme: 'auto' }, (data) => applyTheme(data.theme));
  chrome.storage.onChanged.addListener((changes, area) => {
    if (area === 'local' && changes.theme) applyTheme(changes.theme.newValue);
  });

  chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
    if (message?.type === 'RUN_ANALYSIS') {
      openPanel();
      run(false);
      sendResponse({ ok: true });
    }
    if (message?.type === 'PING_CONTENT') sendResponse({ ok: true });
    return true;
  });

  applyTheme('auto');
})();
