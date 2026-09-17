/** Шаблоны разметки. Чистые функции: данные на входе — HTML на выходе. */

import { PLANS } from '../config.js';
import { SETTING_GROUPS } from '../data/options.js';
import { STYLES } from '../data/styles.js';
import { escapeHtml, formatDate, plural } from '../utils.js';
import { icon } from './icons.js';

/* ──────────────────────────  стили  ────────────────────────── */

export function styleCards(activeId) {
  return STYLES.map((style) => `
    <button
      class="style-card ${style.id === activeId ? 'is-active' : ''}"
      type="button"
      role="radio"
      aria-checked="${style.id === activeId}"
      data-style="${style.id}"
      style="--card-grad: ${style.gradient}">
      <span class="style-card__check">${icon('check')}</span>
      <span class="style-card__icon" aria-hidden="true">${style.icon}</span>
      <span class="style-card__name">${style.name}</span>
      <span class="style-card__desc">${style.desc}</span>
    </button>`).join('');
}

/* ──────────────────────────  настройки  ────────────────────────── */

export function settingGroups(settings) {
  return SETTING_GROUPS.map((group) => `
    <div class="setting">
      <p class="setting__label">${icon(group.icon)} ${group.label}</p>
      <div class="setting__options" role="radiogroup" aria-label="${group.label}">
        ${group.options.map((option) => `
          <button
            class="chip ${settings[group.id] === option.id ? 'is-active' : ''}"
            type="button"
            role="radio"
            aria-checked="${settings[group.id] === option.id}"
            data-group="${group.id}"
            data-option="${option.id}">${option.label}</button>`).join('')}
      </div>
    </div>`).join('');
}

/* ──────────────────────────  результат  ────────────────────────── */

export function skeleton() {
  return `
    <div class="skeleton">
      <p class="skeleton__label">AI собирает промпт...</p>
      <span class="skeleton__line"></span>
      <span class="skeleton__line"></span>
      <span class="skeleton__line"></span>
      <span class="skeleton__line"></span>
    </div>`;
}

export function resultCard(entry, { isFavorite = false } = {}) {
  const words = entry.prompt.split(/\s+/).filter(Boolean).length;
  const chars = entry.prompt.length;
  const tags = [
    entry.styleName,
    entry.settingLabels?.camera,
    entry.settingLabels?.lighting,
    entry.settingLabels?.quality,
    entry.settingLabels?.aspect,
  ].filter(Boolean);

  return `
    <article class="result" data-entry="${entry.id}">
      <header class="result__head">
        <h3 class="result__title">${icon('sparkles')} Готовый промпт</h3>
        <div class="result__tags">
          ${tags.map((tag, index) => `<span class="tag ${index === 0 ? 'tag--accent' : ''}">${escapeHtml(tag)}</span>`).join('')}
        </div>
      </header>

      <pre class="result__body" id="promptText">${escapeHtml(entry.multiline || entry.prompt)}</pre>

      <div class="result__meta">
        <span><b>${words}</b> ${plural(words, ['слово', 'слова', 'слов'])}</span>
        <span><b>${chars}</b> ${plural(chars, ['символ', 'символа', 'символов'])}</span>
        ${entry.ratio ? `<span>Midjourney: <b>${escapeHtml(entry.ratio)}</b></span>` : ''}
        ${entry.degraded ? '<span>офлайн-режим</span>' : ''}
      </div>

      ${entry.negative ? `<p class="result__negative"><b>Negative prompt:</b> ${escapeHtml(entry.negative)}</p>` : ''}

      <div class="result__actions">
        <button class="btn btn--primary btn--copy" type="button" data-action="copy">${icon('copy')} Копировать</button>
        <button class="btn" type="button" data-action="improve">
          <span class="btn__label">${icon('wand')} Улучшить промпт</span><span class="btn__spinner"></span>
        </button>
        <button class="btn" type="button" data-action="variants">
          <span class="btn__label">${icon('layers')} Создать варианты</span><span class="btn__spinner"></span>
        </button>
        <button class="btn" type="button" data-action="translate">
          <span class="btn__label">${icon('globe')} Перевести на английский</span><span class="btn__spinner"></span>
        </button>
      </div>

      <div class="result__actions" style="grid-template-columns: 1fr 1fr; margin-top: 8px;">
        <button class="btn btn--sm" type="button" data-action="favorite">
          ${icon('star')} ${isFavorite ? 'В избранном' : 'В избранное'}
        </button>
        <button class="btn btn--sm" type="button" data-action="regenerate">${icon('refresh')} Ещё раз</button>
      </div>

      <div class="variants" id="variantList"></div>
    </article>`;
}

export function variantCard(variant) {
  return `
    <div class="variant" data-variant="${escapeHtml(variant.id)}">
      <div class="variant__head">
        <span class="variant__name">${escapeHtml(variant.name)}</span>
        <button class="icon-btn" type="button" data-action="copy-variant" aria-label="Копировать вариант">${icon('copy')}</button>
      </div>
      <p class="variant__text">${escapeHtml(variant.prompt)}</p>
    </div>`;
}

/* ──────────────────────────  библиотека  ────────────────────────── */

export function libraryEntry(entry, { isFavorite }) {
  return `
    <article class="entry" data-entry="${entry.id}">
      <div class="entry__top">
        <p class="entry__idea">${escapeHtml(entry.idea)}</p>
        <span class="entry__date">${formatDate(entry.createdAt)}</span>
      </div>
      <p class="entry__prompt">${escapeHtml(entry.prompt)}</p>
      <div class="entry__bottom">
        <span class="tag tag--accent">${escapeHtml(entry.styleName || '')}</span>
        ${entry.settingLabels?.quality ? `<span class="tag">${escapeHtml(entry.settingLabels.quality)}</span>` : ''}
        ${entry.settingLabels?.aspect ? `<span class="tag">${escapeHtml(entry.settingLabels.aspect)}</span>` : ''}
        <span class="entry__tools">
          <button class="icon-btn" type="button" data-action="copy-entry" aria-label="Копировать">${icon('copy')}</button>
          <button class="icon-btn ${isFavorite ? 'is-on' : ''}" type="button" data-action="fav-entry" aria-label="В избранное">${icon('star')}</button>
          <button class="icon-btn" type="button" data-action="reuse-entry" aria-label="Открыть в генераторе">${icon('refresh')}</button>
          <button class="icon-btn" type="button" data-action="delete-entry" aria-label="Удалить">${icon('trash')}</button>
        </span>
      </div>
    </article>`;
}

export function emptyState(title, text, symbol = '✨') {
  return `
    <div class="empty">
      <span class="empty__icon" aria-hidden="true">${symbol}</span>
      <p class="empty__title">${title}</p>
      <p class="empty__text">${text}</p>
    </div>`;
}

/* ──────────────────────────  статические блоки  ────────────────────────── */

export const FEATURES = [
  { icon: '🧠', title: 'Профессиональная структура', text: 'Объект, стиль, окружение, камера, свет, качество и детализация — промпт собирается по той же схеме, что используют профи.' },
  { icon: '🌐', title: 'Перевод с русского', text: 'Пишите как думаете. AI переведёт описание в английские ключевые слова, которые понимают все генераторы изображений.' },
  { icon: '🎛️', title: 'Тонкая настройка', text: 'Семь стилей, четыре типа камеры, три схемы освещения, качество до 8K и три формата кадра.' },
  { icon: '🔀', title: 'Несколько вариантов', text: 'Один клик — и вы получаете драматичный, мягкий, эпичный и другие варианты одной и той же идеи.' },
  { icon: '⭐', title: 'История и избранное', text: 'Все промпты сохраняются автоматически. Лучшие — отмечайте звездой и возвращайтесь к ним в любой момент.' },
  { icon: '🚫', title: 'Negative prompt', text: 'К каждому стилю прилагается список того, что модели лучше не рисовать. Меньше артефактов — чище результат.' },
];

export function featureCards() {
  return FEATURES.map((feature) => `
    <article class="feature reveal" data-reveal>
      <div class="feature__icon" aria-hidden="true">${feature.icon}</div>
      <h3 class="feature__title">${feature.title}</h3>
      <p class="feature__text">${feature.text}</p>
    </article>`).join('');
}

export function pricingCards(currentPlanId) {
  const plans = [PLANS.free, PLANS.premium];

  return plans.map((plan) => {
    const isPro = plan.id === 'premium';
    const isCurrent = plan.id === currentPlanId;

    return `
      <article class="plan ${isPro ? 'plan--pro' : ''} reveal" data-reveal>
        ${isPro ? '<span class="plan__ribbon">Популярный</span>' : ''}
        <p class="plan__name">${plan.label}</p>
        <div class="plan__price">
          <span class="plan__amount">${plan.price}</span>
          <span class="plan__period">${plan.period}</span>
        </div>
        <p class="plan__desc">${plan.desc}</p>
        <ul class="plan__list">
          ${plan.features.map((feature) => `<li>${icon('check')}<span>${feature}</span></li>`).join('')}
        </ul>
        <button
          class="btn ${isPro ? 'btn--primary' : ''} btn--block"
          type="button"
          data-plan="${plan.id}"
          ${isCurrent ? 'disabled' : ''}>
          ${isCurrent ? 'Ваш текущий тариф' : isPro ? 'Перейти на Premium' : 'Создать аккаунт'}
        </button>
      </article>`;
  }).join('');
}

export const FAQ = [
  {
    q: 'Нужно ли знать английский?',
    a: 'Нет. Опишите идею по-русски — PromptAI переведёт описание в английские ключевые слова и соберёт из них промпт. Кнопка «Перевести на английский» дополнительно прогоняет готовый результат через перевод.',
  },
  {
    q: 'Для каких генераторов подходят промпты?',
    a: 'Для Midjourney, Stable Diffusion, DALL·E, Flux, Kandinsky и любых других моделей, которые принимают текстовое описание. Для Midjourney мы дополнительно показываем параметр соотношения сторон (--ar).',
  },
  {
    q: 'Что такое negative prompt?',
    a: 'Это список того, чего в картинке быть не должно: лишние пальцы, водяные знаки, искажённая анатомия. Большинство генераторов поддерживают отдельное поле для негативного промпта — просто скопируйте туда нашу строку.',
  },
  {
    q: 'Где хранятся мои промпты?',
    a: 'В памяти вашего браузера на этом устройстве. Мы ничего не отправляем на сервер, пока вы не подключите собственный AI-бэкенд. Очистка данных браузера удалит историю.',
  },
  {
    q: 'Как работает бесплатный лимит?',
    a: 'Без аккаунта доступно 5 генераций в день, с бесплатным аккаунтом — 20. Счётчик обнуляется каждые сутки. Premium снимает ограничение полностью и открывает до 5 вариантов за раз.',
  },
  {
    q: 'Можно ли подключить свою AI-модель?',
    a: 'Да, проект к этому подготовлен. В assets/js/config.js переключите provider на «api» и укажите адрес своего эндпоинта — контракт описан в assets/js/ai/api-provider.js и в README.',
  },
];

export function faqItems() {
  return FAQ.map((item) => `
    <details class="faq__item">
      <summary class="faq__q">${item.q}</summary>
      <p class="faq__a">${item.a}</p>
    </details>`).join('');
}
