/** Мелкие утилиты, общие для всего приложения. */

/** Быстрый querySelector. */
export const $ = (sel, root = document) => root.querySelector(sel);
export const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));

/** Экранирование пользовательского текста перед вставкой в HTML. */
export function escapeHtml(value = '') {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

/** Уникальный идентификатор записи. */
export function uid() {
  if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID();
  return `id-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 9)}`;
}

/** Пауза. */
export const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

/** Случайное целое в диапазоне [min, max]. */
export const randomInt = (min, max) => Math.floor(Math.random() * (max - min + 1)) + min;

/** Случайный элемент массива. */
export const pick = (list) => list[Math.floor(Math.random() * list.length)];

/** Псевдослучайный, но стабильный выбор — чтобы варианты не повторялись. */
export function pickUnique(list, used = new Set()) {
  const free = list.filter((item) => !used.has(item));
  const chosen = free.length ? pick(free) : pick(list);
  used.add(chosen);
  return chosen;
}

/** Debounce. */
export function debounce(fn, wait = 200) {
  let timer;
  return (...args) => {
    clearTimeout(timer);
    timer = setTimeout(() => fn(...args), wait);
  };
}

/** Человекочитаемая дата. */
export function formatDate(timestamp) {
  const date = new Date(timestamp);
  const now = new Date();
  const diffMin = Math.round((now - date) / 60000);

  if (diffMin < 1) return 'только что';
  if (diffMin < 60) return `${diffMin} мин назад`;
  if (diffMin < 60 * 24 && now.getDate() === date.getDate()) {
    return `сегодня, ${date.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })}`;
  }
  return date.toLocaleDateString('ru-RU', { day: '2-digit', month: 'short', year: '2-digit' });
}

/** Русская плюрализация: plural(5, ['вариант','варианта','вариантов']). */
export function plural(count, forms) {
  const n = Math.abs(count) % 100;
  const n1 = n % 10;
  if (n > 10 && n < 20) return forms[2];
  if (n1 > 1 && n1 < 5) return forms[1];
  if (n1 === 1) return forms[0];
  return forms[2];
}

/** Ключ текущих суток — для дневных лимитов. */
export function todayKey(date = new Date()) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

/** Есть ли в строке кириллица. */
export const hasCyrillic = (text = '') => /[а-яё]/i.test(text);

/** Копирование в буфер обмена с запасным вариантом для старых мобильных браузеров. */
export async function copyText(text) {
  try {
    if (navigator.clipboard && globalThis.isSecureContext) {
      await navigator.clipboard.writeText(text);
      return true;
    }
  } catch {
    /* падаем в запасной путь ниже */
  }

  try {
    const area = document.createElement('textarea');
    area.value = text;
    area.setAttribute('readonly', '');
    area.style.cssText = 'position:fixed;top:-1000px;opacity:0;';
    document.body.appendChild(area);
    area.select();
    const ok = document.execCommand('copy');
    area.remove();
    return ok;
  } catch {
    return false;
  }
}

/** Лёгкая тактильная отдача на мобильных. */
export function haptic(pattern = 12) {
  if (navigator.vibrate) navigator.vibrate(pattern);
}

/** Нормализация пробелов и запятых в промпте. */
export function tidyPrompt(text = '') {
  return text
    .replace(/\s+/g, ' ')
    .replace(/\s*,\s*/g, ', ')
    .replace(/(,\s*){2,}/g, ', ')
    .replace(/^[,\s]+|[,\s]+$/g, '')
    .trim();
}

/** Удаление повторяющихся частей промпта (без учёта регистра). */
export function dedupeParts(parts) {
  const seen = new Set();
  const out = [];
  for (const part of parts) {
    const value = String(part || '').trim();
    if (!value) continue;
    const key = value.toLowerCase();
    if (seen.has(key)) continue;
    seen.add(key);
    out.push(value);
  }
  return out;
}
