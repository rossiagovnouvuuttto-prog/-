/**
 * Точка входа для всех AI-операций.
 *
 * UI обращается только сюда и ничего не знает о том, считается промпт
 * локально или на сервере. Переключение — через AI.provider в config.js.
 */

import { AI } from '../config.js';
import { apiProvider } from './api-provider.js';
import { localProvider } from './local-provider.js';

const PROVIDERS = {
  local: localProvider,
  api: apiProvider,
};

function active() {
  return PROVIDERS[AI.provider] || localProvider;
}

/** Выполняет операцию на выбранном провайдере, при сбое падая на локальный движок. */
async function run(task, payload) {
  const provider = active();

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
  translateIdea: (payload) => run('translateIdea', payload),
  get providerId() {
    return active().id;
  },
};
