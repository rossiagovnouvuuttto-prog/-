/** Всплывающие уведомления. */

import { $ } from '../utils.js';
import { icon } from './icons.js';

const ICONS = { success: 'check', error: 'alert', info: 'info' };

export function toast(message, type = 'info', duration = 2600) {
  const root = $('#toastRoot');
  if (!root) return;

  const node = document.createElement('div');
  node.className = `toast toast--${type}`;
  node.innerHTML = `<span class="toast__icon">${icon(ICONS[type] || 'info')}</span><span>${message}</span>`;
  root.appendChild(node);

  setTimeout(() => {
    node.classList.add('is-out');
    node.addEventListener('animationend', () => node.remove(), { once: true });
  }, duration);
}
