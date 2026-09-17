/**
 * Хранилище состояния.
 *
 * Данные лежат в localStorage, а при его недоступности (приватный режим,
 * заблокированные куки) — в памяти вкладки. Наружу торчит один объект `store`
 * с подпиской на изменения, поэтому источник данных можно заменить на API,
 * не трогая UI.
 */

import { APP, DEFAULT_SETTINGS, LIMITS } from './config.js';
import { todayKey, uid } from './utils.js';

const KEYS = {
  settings:  `${APP.storagePrefix}settings`,
  history:   `${APP.storagePrefix}history`,
  favorites: `${APP.storagePrefix}favorites`,
  usage:     `${APP.storagePrefix}usage`,
  session:   `${APP.storagePrefix}session`,
  accounts:  `${APP.storagePrefix}accounts`,
};

const memory = new Map();

function storage() {
  try {
    const probe = `${APP.storagePrefix}probe`;
    localStorage.setItem(probe, '1');
    localStorage.removeItem(probe);
    return localStorage;
  } catch {
    return null;
  }
}

const backend = storage();

function read(key, fallback) {
  try {
    const raw = backend ? backend.getItem(key) : memory.get(key);
    if (raw == null) return structuredCloneSafe(fallback);
    return JSON.parse(raw);
  } catch {
    return structuredCloneSafe(fallback);
  }
}

function write(key, value) {
  const raw = JSON.stringify(value);
  try {
    if (backend) backend.setItem(key, raw);
    else memory.set(key, raw);
  } catch {
    memory.set(key, raw);
  }
  return value;
}

function structuredCloneSafe(value) {
  return value == null ? value : JSON.parse(JSON.stringify(value));
}

/* ──────────────────────────  подписки  ────────────────────────── */

const listeners = new Set();

function emit(event) {
  for (const listener of listeners) {
    try {
      listener(event);
    } catch (error) {
      console.error('[store] listener failed', error);
    }
  }
}

/* ──────────────────────────  публичный API  ────────────────────────── */

export const store = {
  subscribe(listener) {
    listeners.add(listener);
    return () => listeners.delete(listener);
  },

  /* — настройки генератора — */

  getSettings() {
    return { ...DEFAULT_SETTINGS, ...read(KEYS.settings, {}) };
  },

  patchSettings(patch) {
    const next = { ...this.getSettings(), ...patch };
    write(KEYS.settings, next);
    emit({ type: 'settings', payload: next });
    return next;
  },

  resetSettings() {
    write(KEYS.settings, { ...DEFAULT_SETTINGS });
    emit({ type: 'settings', payload: { ...DEFAULT_SETTINGS } });
    return { ...DEFAULT_SETTINGS };
  },

  /* — история — */

  getHistory() {
    return read(KEYS.history, []);
  },

  addHistory(entry) {
    const record = { id: entry.id || uid(), createdAt: Date.now(), ...entry };
    const next = [record, ...this.getHistory().filter((item) => item.id !== record.id)]
      .slice(0, LIMITS.historySize);
    write(KEYS.history, next);
    emit({ type: 'history', payload: next });
    return record;
  },

  updateHistory(id, patch) {
    const next = this.getHistory().map((item) => (item.id === id ? { ...item, ...patch } : item));
    write(KEYS.history, next);
    emit({ type: 'history', payload: next });
    return next;
  },

  removeHistory(id) {
    const next = this.getHistory().filter((item) => item.id !== id);
    write(KEYS.history, next);
    emit({ type: 'history', payload: next });
    return next;
  },

  clearHistory() {
    write(KEYS.history, []);
    emit({ type: 'history', payload: [] });
  },

  /* — избранное — */

  getFavorites() {
    return read(KEYS.favorites, []);
  },

  isFavorite(id) {
    return this.getFavorites().some((item) => item.id === id);
  },

  toggleFavorite(entry) {
    const favorites = this.getFavorites();
    const exists = favorites.some((item) => item.id === entry.id);
    const next = exists
      ? favorites.filter((item) => item.id !== entry.id)
      : [{ ...entry, favoritedAt: Date.now() }, ...favorites].slice(0, LIMITS.favoritesSize);

    write(KEYS.favorites, next);
    emit({ type: 'favorites', payload: next });
    return !exists;
  },

  removeFavorite(id) {
    const next = this.getFavorites().filter((item) => item.id !== id);
    write(KEYS.favorites, next);
    emit({ type: 'favorites', payload: next });
    return next;
  },

  /* — дневной расход генераций —
     Счётчик свой у каждого аккаунта (и отдельный у гостя), поэтому после
     регистрации пользователь действительно получает новый лимит. */

  getUsageMap() {
    const usage = read(KEYS.usage, { day: todayKey(), byScope: {} });
    if (usage.day !== todayKey()) {
      const reset = { day: todayKey(), byScope: {} };
      write(KEYS.usage, reset);
      return reset;
    }
    return { day: usage.day, byScope: usage.byScope || {} };
  },

  getUsage(scope = 'guest') {
    const usage = this.getUsageMap();
    return { day: usage.day, scope, used: usage.byScope[scope] || 0 };
  },

  bumpUsage(scope = 'guest', amount = 1) {
    const usage = this.getUsageMap();
    const next = {
      day: usage.day,
      byScope: { ...usage.byScope, [scope]: (usage.byScope[scope] || 0) + amount },
    };
    write(KEYS.usage, next);
    emit({ type: 'usage', payload: next });
    return next;
  },

  /* — аккаунты (демо-реализация, заменяется на серверную) — */

  getAccounts() {
    return read(KEYS.accounts, []);
  },

  saveAccounts(accounts) {
    write(KEYS.accounts, accounts);
    return accounts;
  },

  getSession() {
    return read(KEYS.session, null);
  },

  setSession(session) {
    write(KEYS.session, session);
    emit({ type: 'session', payload: session });
    return session;
  },
};
