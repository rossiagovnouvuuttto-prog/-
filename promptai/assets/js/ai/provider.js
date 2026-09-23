/**
 * Точка входа для всех AI-операций.
 *
 * UI обращается только сюда и не знает, кто считает промпт. Порядок выбора:
 *
 *   1. настройки пользователя — если он подключил Ollama своим ключом;
 *   2. сервер PromptAI — если сайт открыт с запущенного сервера;
 *   3. встроенный движок — всегда доступен, работает без сети.
 */

import { AI } from '../config.js';
import { isConnected, resolve } from './connection.js';
import { apiProvider } from './api-provider.js';
import { directProvider } from './direct-provider.js';
import { localProvider } from './local-provider.js';

const PROVIDERS = {
  local: localProvider,
  api: apiProvider,
  direct: directProvider,
};

/** Что известно о текущем движке: null — ещё не определяли. */
let detected = null;
let detection = null;

/** Сбрасывает выбор — вызывается после изменения настроек подключения. */
export function resetProvider() {
  detected = null;
  detection = null;
}

async function probeBackend() {
  if (!/^https?:$/.test(globalThis.location?.protocol || '')) return null;

  try {
    const response = await fetch(AI.healthEndpoint, {
      signal: AbortSignal.timeout(AI.healthTimeoutMs),
    });
    if (!response.ok) throw new Error(String(response.status));
    return await response.json();
  } catch {
    // Сервера нет — это штатный режим, не ошибка.
    return null;
  }
}

async function detect() {
  if (AI.provider !== 'auto') {
    return { provider: PROVIDERS[AI.provider] || localProvider, info: null };
  }
  if (detected) return detected;

  detection ||= (async () => {
    if (isConnected()) {
      const connection = resolve();
      detected = {
        provider: directProvider,
        info: { model: connection.model, label: connection.label, source: 'direct' },
      };
      return detected;
    }

    const health = await probeBackend();
    detected = health
      ? { provider: apiProvider, info: { ...health, source: 'server' } }
      : { provider: localProvider, info: null };

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
      return { ...result, degraded: true, reason: error };
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
  /** Какой движок работает сейчас: { provider, info }. */
  status:        () => detect(),
};
