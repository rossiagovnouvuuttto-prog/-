/**
 * Content script для chatgpt.com / chat.openai.com.
 * Забирает одноразовый промпт, который расширение сохранило в chrome.storage.local,
 * вставляет его в поле ввода уже открытого чата и (по настройке) нажимает «Отправить».
 *
 * Работает только в текущей, уже авторизованной вкладке пользователя.
 * НЕ читает cookies или токены, НЕ выполняет вход в аккаунт, НЕ обращается к API —
 * делает ровно то, что сделал бы человек: печатает текст и жмёт кнопку отправки.
 */
(function () {
  'use strict';

  if (window.__fxgChatgptLoaded) return;
  window.__fxgChatgptLoaded = true;

  const KEY = 'pendingPrompt';
  const MAX_WAIT_MS = 25000; // сколько ждём загрузки поля ввода чата
  const FRESH_MS = 120000;   // промпт старше 2 минут не используем

  function findInput() {
    // Основное поле ChatGPT — редактируемый div ProseMirror; textarea — запасной вариант
    return (
      document.querySelector('#prompt-textarea') ||
      document.querySelector('div[contenteditable="true"].ProseMirror') ||
      document.querySelector('div[contenteditable="true"][id^="prompt"]') ||
      document.querySelector('form textarea')
    );
  }

  function findSendButton() {
    return (
      document.querySelector('button[data-testid="send-button"]') ||
      document.querySelector('button[aria-label*="Send"]') ||
      document.querySelector('button[aria-label*="Отправить"]') ||
      document.querySelector('form button[type="submit"]')
    );
  }

  function waitFor(getter, timeout) {
    return new Promise((resolve) => {
      const found = getter();
      if (found) return resolve(found);
      const started = Date.now();
      const observer = new MutationObserver(() => {
        const el = getter();
        if (el) {
          observer.disconnect();
          resolve(el);
        } else if (Date.now() - started > timeout) {
          observer.disconnect();
          resolve(null);
        }
      });
      observer.observe(document.documentElement, { childList: true, subtree: true });
      setTimeout(() => {
        observer.disconnect();
        resolve(getter());
      }, timeout);
    });
  }

  function setTextareaValue(el, text) {
    const setter = Object.getOwnPropertyDescriptor(window.HTMLTextAreaElement.prototype, 'value');
    if (setter && setter.set) setter.set.call(el, text);
    else el.value = text;
    el.dispatchEvent(new Event('input', { bubbles: true }));
    el.dispatchEvent(new Event('change', { bubbles: true }));
  }

  function setProseMirrorValue(el, text) {
    el.focus();
    // Пытаемся вставить как обычный ввод, чтобы редактор корректно обновил состояние
    let inserted = false;
    try {
      const dt = new DataTransfer();
      dt.setData('text/plain', text);
      const pasteEvent = new ClipboardEvent('paste', { bubbles: true, cancelable: true, clipboardData: dt });
      inserted = !el.dispatchEvent(pasteEvent) || el.textContent.length > 0;
    } catch (e) {
      inserted = false;
    }
    if (!el.textContent) {
      // Запасной способ: строим абзацы вручную (ProseMirror хранит строки как <p>)
      el.innerHTML = '';
      for (const line of text.split('\n')) {
        const p = document.createElement('p');
        if (line) p.textContent = line;
        else p.appendChild(document.createElement('br'));
        el.appendChild(p);
      }
      el.dispatchEvent(new InputEvent('input', { bubbles: true }));
    }
  }

  function fillInput(el, text) {
    if (el.tagName === 'TEXTAREA') setTextareaValue(el, text);
    else setProseMirrorValue(el, text);
  }

  function toast(text) {
    const box = document.createElement('div');
    box.textContent = text;
    box.setAttribute('role', 'status');
    box.style.cssText = [
      'position:fixed', 'z-index:2147483000', 'left:50%', 'bottom:24px',
      'transform:translateX(-50%)', 'max-width:90vw', 'padding:12px 18px',
      'background:#1f8a70', 'color:#fff', 'font:600 14px/1.4 -apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,Arial,sans-serif',
      'border-radius:12px', 'box-shadow:0 12px 32px rgba(0,0,0,0.3)'
    ].join(';');
    document.documentElement.appendChild(box);
    setTimeout(() => { box.style.transition = 'opacity .4s'; box.style.opacity = '0'; }, 4200);
    setTimeout(() => box.remove(), 4800);
  }

  async function trySubmit() {
    const btn = await waitFor(findSendButton, 4000);
    if (btn && !btn.disabled) {
      btn.click();
      return true;
    }
    // Если кнопка недоступна — эмулируем Enter в поле ввода
    const input = findInput();
    if (input) {
      const opts = { bubbles: true, cancelable: true, key: 'Enter', code: 'Enter', keyCode: 13, which: 13 };
      input.dispatchEvent(new KeyboardEvent('keydown', opts));
      input.dispatchEvent(new KeyboardEvent('keyup', opts));
      return true;
    }
    return false;
  }

  async function run() {
    const store = await chrome.storage.local.get({ [KEY]: null, autoSend: true });
    const pending = store[KEY];
    if (!pending || !pending.text) return;
    if (Date.now() - (pending.ts || 0) > FRESH_MS) {
      await chrome.storage.local.remove(KEY);
      return;
    }
    // Промпт одноразовый — сразу удаляем, чтобы не вставить повторно при F5
    await chrome.storage.local.remove(KEY);

    const input = await waitFor(findInput, MAX_WAIT_MS);
    if (!input) {
      toast('Не нашёл поле ввода ChatGPT — вставьте задание вручную (Ctrl+V).');
      return;
    }

    fillInput(input, pending.text);
    await new Promise((r) => setTimeout(r, 400));

    if (store.autoSend) {
      const sent = await trySubmit();
      toast(sent ? 'Задание отправлено в ChatGPT' : 'Задание вставлено — нажмите «Отправить».');
    } else {
      toast('Задание вставлено в поле — проверьте и нажмите «Отправить».');
    }
  }

  // Промпт мог прийти уже после загрузки страницы (мы сохранили его почти одновременно
  // с открытием вкладки) — реагируем и на появление ключа в хранилище.
  chrome.storage.onChanged.addListener((changes, area) => {
    if (area === 'local' && changes[KEY] && changes[KEY].newValue) run();
  });

  run();
})();
