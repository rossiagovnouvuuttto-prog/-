/**
 * Локальный провайдер: работает офлайн на встроенном движке.
 * Используется по умолчанию и как запасной вариант, если API недоступно.
 */

import { AI } from '../config.js';
import { buildPrompt, buildVariants, enhancePrompt } from '../engine/prompt-builder.js';
import { translatePrompt, translateToEnglish } from '../engine/translator.js';
import { randomInt, sleep } from '../utils.js';

/** Небольшая задержка, чтобы интерфейс вёл себя так же, как с реальным API. */
async function think() {
  const [min, max] = AI.localLatencyMs;
  await sleep(randomInt(min, max));
}

export const localProvider = {
  id: 'local',

  async generate({ idea, settings }) {
    await think();
    const result = buildPrompt({ idea, settings });
    return { ...result, source: 'local' };
  },

  async improve({ prompt, multiline }) {
    await think();
    const result = enhancePrompt(prompt, multiline);
    return { ...result, source: 'local' };
  },

  async variants({ idea, settings, count }) {
    await think();
    return { variants: buildVariants({ idea, settings, count }), source: 'local' };
  },

  async translate({ text }) {
    await think();
    return { text: translatePrompt(text), source: 'local' };
  },

  /** Перевод описания пользователя — используется до сборки промпта. */
  async translateIdea({ idea }) {
    await think();
    return { text: translateToEnglish(idea), source: 'local' };
  },
};
