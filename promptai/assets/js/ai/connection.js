/**
 * Настройки подключения, которые пользователь вводит сам.
 *
 * Ключ хранится только в localStorage этого браузера: он не попадает ни в
 * код сайта, ни в репозиторий, ни на чужие устройства. Отправляется он
 * единственному адресату — выбранному сервису.
 */

import { APP } from '../config.js';
import { DEFAULT_SERVICE, getService } from './services.js';

const KEY = `${APP.storagePrefix}connection`;

export const DEFAULTS = {
  /** 'off' — встроенный движок; иначе идентификатор сервиса. */
  service: 'off',
  apiKey: '',
  model: '',
  host: '',
};

export function readConnection() {
  try {
    const stored = JSON.parse(localStorage.getItem(KEY) || '{}');
    return { ...DEFAULTS, ...stored };
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

export const isConnected = (connection = readConnection()) => connection.service !== 'off';

/** Итоговые параметры запроса: что выбрал пользователь, дополненное умолчаниями сервиса. */
export function resolve(connection = readConnection()) {
  const service = getService(connection.service === 'off' ? DEFAULT_SERVICE : connection.service);

  return {
    id: connection.service,
    api: service.api,
    label: service.label,
    host: (connection.host || service.host).replace(/\/+$/, ''),
    model: connection.model || service.model,
    apiKey: connection.apiKey,
  };
}
