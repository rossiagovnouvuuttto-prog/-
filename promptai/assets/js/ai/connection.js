/**
 * Настройки подключения к Ollama, которые пользователь вводит сам.
 *
 * Ключ хранится только в localStorage этого браузера: он не попадает ни в
 * код сайта, ни в репозиторий, ни на чужие устройства. Отправляется он
 * единственному адресату — самой Ollama.
 */

import { APP } from '../config.js';

const KEY = `${APP.storagePrefix}connection`;

export const CLOUD_HOST = 'https://ollama.com';
export const LOCAL_HOST = 'http://localhost:11434';

export const DEFAULTS = {
  /** 'off' — встроенный движок, 'cloud' — облако по ключу, 'local' — Ollama на этом компьютере. */
  mode: 'off',
  apiKey: '',
  model: '',
  host: '',
};

export function readConnection() {
  try {
    return { ...DEFAULTS, ...JSON.parse(localStorage.getItem(KEY) || '{}') };
  } catch {
    return { ...DEFAULTS };
  }
}

export function saveConnection(patch) {
  const next = { ...readConnection(), ...patch };
  try {
    localStorage.setItem(KEY, JSON.stringify(next));
  } catch {
    /* приватный режим — настройки живут до перезагрузки */
  }
  return next;
}

export function clearConnection() {
  try {
    localStorage.removeItem(KEY);
  } catch { /* ничего не делаем */ }
  return { ...DEFAULTS };
}

/** Адрес Ollama для текущего режима. */
export function hostFor(connection) {
  if (connection.host) return connection.host.replace(/\/+$/, '');
  return connection.mode === 'local' ? LOCAL_HOST : CLOUD_HOST;
}

/** Модель по умолчанию для текущего режима. */
export function modelFor(connection) {
  if (connection.model) return connection.model;
  return connection.mode === 'local' ? 'llama3.2' : 'gpt-oss:120b-cloud';
}
