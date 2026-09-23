/**
 * Прямое обращение к Ollama из браузера.
 *
 * Работает без сервера: страница сама отправляет запрос в Ollama с ключом,
 * который пользователь ввёл в настройках. Ключ остаётся в его браузере.
 *
 * Ограничение, которое здесь не обойти: браузер выполнит запрос, только если
 * Ollama разрешает обращения с адреса этой страницы (CORS). Если не разрешает,
 * запрос не уйдёт, и мы сообщаем об этом понятным текстом — гадать
 * пользователю не придётся.
 */

import { AI } from '../config.js';
import { createTasks } from './ollama-tasks.js';
import { hostFor, modelFor, readConnection } from './connection.js';

/** Человеческое объяснение вместо «Failed to fetch». */
export function explain(error, connection) {
  const message = String(error?.message || error);

  if (/Failed to fetch|NetworkError|load failed/i.test(message)) {
    return connection.mode === 'local'
      ? 'Браузер не смог обратиться к Ollama на этом компьютере. Проверьте, что приложение Ollama запущено, и что ему разрешены обращения с этой страницы (переменная OLLAMA_ORIGINS).'
      : 'Браузер не смог обратиться к ollama.com напрямую — сервис не разрешает запросы со страниц сайтов. Ключ здесь ни при чём: нужен запуск сервера PromptAI, он обращается к Ollama сам.';
  }
  if (/\b401\b|\b403\b/.test(message)) return 'Ollama не приняла ключ. Проверьте, что он скопирован полностью.';
  if (/\b404\b/.test(message)) return 'Такой модели нет. Список облачных моделей — на ollama.com/search?c=cloud';
  if (/aborted|timeout/i.test(message)) return 'Ollama не ответила вовремя. Попробуйте модель полегче.';
  return message;
}

/** Запрос к Ollama с ответом строго по JSON-схеме. */
async function chat({ system, user, schema }) {
  const connection = readConnection();

  const response = await fetch(`${hostFor(connection)}/api/chat`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(connection.apiKey ? { Authorization: `Bearer ${connection.apiKey}` } : {}),
    },
    body: JSON.stringify({
      model: modelFor(connection),
      stream: false,
      format: schema,
      options: { temperature: 0.8 },
      messages: [
        { role: 'system', content: system },
        { role: 'user', content: user },
      ],
    }),
    signal: AbortSignal.timeout(AI.timeoutMs),
  });

  const raw = await response.text();

  if (!response.ok) {
    let detail = raw.slice(0, 300);
    try {
      detail = JSON.parse(raw).error || detail;
    } catch { /* не JSON */ }
    throw new Error(`Ollama ${response.status}: ${detail}`);
  }

  const content = JSON.parse(raw)?.message?.content;
  if (!content) throw new Error('Ollama вернула пустой ответ');

  return JSON.parse(content);
}

const tasks = createTasks(chat);

export const directProvider = {
  id: 'direct',
  generate: tasks.generate,
  improve: tasks.improve,
  variants: tasks.variants,
  translate: tasks.translate,
  translateRu: tasks.translateRu,
  translateIdea: ({ idea }) => tasks.translate({ text: idea }),
  ping: tasks.ping,
};
