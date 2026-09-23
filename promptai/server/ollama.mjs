/**
 * Серверный клиент Ollama.
 *
 * Ключ живёт только здесь. Браузер к Ollama не обращается — он стучится
 * в наш /api/prompt, поэтому ключ не попадает на страницу.
 *
 * Промпты и схемы общие с браузерной версией: ../assets/js/ai/ollama-tasks.js
 */

import { createTasks } from '../assets/js/ai/ollama-tasks.js';

const CLOUD_HOST = 'https://ollama.com';
const LOCAL_HOST = 'http://localhost:11434';

export const config = {
  apiKey: process.env.OLLAMA_API_KEY || '',
  host: process.env.OLLAMA_HOST || '',
  model: process.env.OLLAMA_MODEL || '',
  timeoutMs: Number(process.env.OLLAMA_TIMEOUT_MS || 60000),
};

/** Ключ есть — значит, работаем с облаком (или с прокси к нему). */
export const isCloud = () => Boolean(config.apiKey);

export const host = () => config.host || (isCloud() ? CLOUD_HOST : LOCAL_HOST);

export const model = () => config.model || (isCloud() ? 'gpt-oss:120b-cloud' : 'llama3.2');

/** Запрос к Ollama с ответом строго по JSON-схеме. */
async function chat({ system, user, schema }) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), config.timeoutMs);

  try {
    const response = await fetch(`${host()}/api/chat`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        // Ключ уходит только отсюда, с сервера, и только в Ollama.
        ...(config.apiKey ? { Authorization: `Bearer ${config.apiKey}` } : {}),
      },
      body: JSON.stringify({
        model: model(),
        stream: false,
        format: schema,
        options: { temperature: 0.8 },
        messages: [
          { role: 'system', content: system },
          { role: 'user', content: user },
        ],
      }),
      signal: controller.signal,
    });

    const raw = await response.text();

    if (!response.ok) {
      // Ollama возвращает понятный текст ошибки — отдаём его как есть.
      let detail = raw.slice(0, 300);
      try {
        detail = JSON.parse(raw).error || detail;
      } catch { /* не JSON — оставляем как есть */ }
      throw new Error(`Ollama ${response.status}: ${detail}`);
    }

    const content = JSON.parse(raw)?.message?.content;
    if (!content) throw new Error('Ollama вернул пустой ответ');

    return JSON.parse(content);
  } finally {
    clearTimeout(timer);
  }
}

export const TASKS = createTasks(chat);
export const { generate, improve, variants, translate, translateRu, ping } = TASKS;
