/** Модальные окна: на мобильных — шторка снизу, на десктопе — карточка по центру. */

import { $ } from '../utils.js';
import { icon } from './icons.js';

let lastFocused = null;

export function openModal({ title, subtitle = '', body, onMount }) {
  const root = $('#modalRoot');
  if (!root) return;

  // Если окно уже открыто (например, из пейволла открывают регистрацию),
  // запоминаем исходный фокус только один раз и не плодим обработчики.
  if (root.hidden) lastFocused = document.activeElement;

  root.innerHTML = `
    <div class="modal" role="dialog" aria-modal="true" aria-label="${title}">
      <div class="modal__grip" aria-hidden="true"></div>
      <div class="modal__head">
        <div>
          <h2 class="modal__title">${title}</h2>
          ${subtitle ? `<p class="modal__sub">${subtitle}</p>` : ''}
        </div>
        <button class="icon-btn modal__close" type="button" aria-label="Закрыть" data-modal-close>${icon('close')}</button>
      </div>
      <div class="modal__body">${body}</div>
    </div>`;

  root.hidden = false;
  document.body.style.overflow = 'hidden';

  root.querySelector('[data-modal-close]')?.addEventListener('click', closeModal);
  root.addEventListener('click', onBackdrop);
  document.addEventListener('keydown', onKeydown);

  onMount?.(root.querySelector('.modal'));
  root.querySelector('input, button:not([data-modal-close])')?.focus({ preventScroll: true });
}

function onBackdrop(event) {
  if (event.target.id === 'modalRoot') closeModal();
}

function onKeydown(event) {
  if (event.key === 'Escape') closeModal();
}

export function closeModal() {
  const root = $('#modalRoot');
  if (!root || root.hidden) return;

  root.hidden = true;
  root.innerHTML = '';
  root.removeEventListener('click', onBackdrop);
  document.removeEventListener('keydown', onKeydown);
  document.body.style.overflow = '';
  lastFocused?.focus?.({ preventScroll: true });
}
