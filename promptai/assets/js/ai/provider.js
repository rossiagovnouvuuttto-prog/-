/**
 * Точка входа для всех AI-операций.
 *
 * UI обращается только сюда и ничего не знает о том, считается промпт
 * локально или на сервере. В режиме 'auto' сайт один раз спрашивает бэкенд,
 * жив ли он: если сервер с моделью не запущен (например, страницу открыли
 * файлом с диска), всё продолжает работать на встроенном движке.
 */

import { AI } from '../config.js';
import { apiProvider } from './api-provider.js';
import { localProvider } from './local-provider.js';

const PROVIDERS = {
  local: localProvider,
  api: apiProvider,
};

/** Что известно о бэкенде: null — ещё не проверяли. */
let detected = null;
let detection = null;

/** Разовая проверка доступности бэкенда. */
async function detect() {
  if (AI.provider !== 'auto') {
    return { provider: PROVIDERS[AI.provider] || localProvider, info: null };
  }
  if (detected) return detected;

  detection ||= (async () => {
    // На file:// запрос к /api/health невозможен в принципе, и браузер пишет
    // в консоль ошибку. Проверяем только там, где бэкенд вообще может быть.
    if (!/^https?:$/.test(globalThis.location?.protocol || '')) {
      detected = { provider: localProvider, info: null };
      return detected;
    }

    try {
      const response = await fetch(AI.healthEndpoint, {
        signal: AbortSignal.timeout(AI.healthTimeoutMs),
      });
      if (!response.ok) throw new Error(String(response.status));

      const info = await response.json();
      detected = { provider: apiProvider, info };
    } catch {
      // Бэкенда нет — это нормальный режим работы, не ошибка.
      detected = { provider: localProvider, info: null };
    }
    return detected;
  })();

  return detection;
}

/** Выполняет операцию на выбранном провайдере, при сбое падая на локальный движок. */
async function run(task, payload) {
  const { provider } = await detect();

  try {
    return await provider[task](payload);
  } catch (error) {
    console.warn(`[ai] ${provider.id}.${task} не сработал:`, error.message);

    if (provider.id !== 'local' && AI.fallbackToLocal) {
      const result = await localProvider[task](payload);
      return { ...result, degraded: true };
    }
    throw error;
  }
}

export const ai = {
  generate:      (payload) => run('generate', payload),
  improve:       (payload) => run('improve', payload),
  variants:      (payload) => run('variants', payload),
  translate:     (payload) => run('translate', payload),
  translateRu:   (payload) => run('translateRu', payload),
  /** Какой движок работает сейчас: { provider, info } — info есть только у бэкенда. */
  status:        () => detect(),
};
