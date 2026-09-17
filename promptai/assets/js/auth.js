/**
 * Система аккаунтов.
 *
 * Здесь лежит ДЕМО-провайдер: пользователи хранятся в localStorage, пароль —
 * в виде SHA-256 с солью. Этого достаточно, чтобы показать сценарий входа,
 * но это не настоящая безопасность: боевую авторизацию делает сервер.
 *
 * Чтобы подключить реальный бэкенд, реализуйте те же четыре метода
 * (register / login / logout / current) и подставьте свой объект в `auth`.
 */

import { PLANS } from './config.js';
import { store } from './store.js';
import { uid } from './utils.js';

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

async function hash(password, salt) {
  const data = new TextEncoder().encode(`${salt}:${password}`);
  if (globalThis.crypto?.subtle) {
    const digest = await globalThis.crypto.subtle.digest('SHA-256', data);
    return Array.from(new Uint8Array(digest))
      .map((byte) => byte.toString(16).padStart(2, '0'))
      .join('');
  }
  // Запасной путь для окружений без WebCrypto — только для демо.
  let acc = 0;
  for (const byte of data) acc = (acc * 31 + byte) >>> 0;
  return `fallback-${acc.toString(16)}`;
}

function publicUser(account) {
  if (!account) return null;
  return {
    id: account.id,
    email: account.email,
    plan: account.plan,
    createdAt: account.createdAt,
  };
}

export const localAuthProvider = {
  async register(email, password) {
    const normalized = String(email).trim().toLowerCase();

    if (!EMAIL_RE.test(normalized)) throw new Error('Введите корректный e-mail');
    if (String(password).length < 6) throw new Error('Пароль должен быть от 6 символов');

    const accounts = store.getAccounts();
    if (accounts.some((item) => item.email === normalized)) {
      throw new Error('Аккаунт с таким e-mail уже существует');
    }

    const salt = uid();
    const account = {
      id: uid(),
      email: normalized,
      salt,
      passwordHash: await hash(password, salt),
      plan: PLANS.free.id,
      createdAt: Date.now(),
    };

    store.saveAccounts([...accounts, account]);
    store.setSession(publicUser(account));
    return publicUser(account);
  },

  async login(email, password) {
    const normalized = String(email).trim().toLowerCase();
    const account = store.getAccounts().find((item) => item.email === normalized);

    if (!account) throw new Error('Аккаунт не найден');
    if ((await hash(password, account.salt)) !== account.passwordHash) {
      throw new Error('Неверный пароль');
    }

    store.setSession(publicUser(account));
    return publicUser(account);
  },

  logout() {
    store.setSession(null);
    return null;
  },

  current() {
    return store.getSession();
  },

  /** Демо-апгрейд до Premium: на проде сюда приходит вебхук платёжной системы. */
  setPlan(planId) {
    const session = store.getSession();
    if (!session) throw new Error('Сначала войдите в аккаунт');

    const accounts = store.getAccounts().map((item) =>
      item.id === session.id ? { ...item, plan: planId } : item,
    );
    store.saveAccounts(accounts);

    const next = { ...session, plan: planId };
    store.setSession(next);
    return next;
  },
};

export const auth = localAuthProvider;

/** Текущий тариф: гость → guest, вошедший пользователь → free/premium. */
export function currentPlan() {
  const user = auth.current();
  if (!user) return PLANS.guest;
  return PLANS[user.plan] || PLANS.free;
}
