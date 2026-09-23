/**
 * Прямое обращение к AI-сервису из браузера.
 *
 * Работает без сервера: страница сама отправляет запрос с ключом, который
 * пользователь ввёл в настройках. Ключ остаётся в его браузере.
 *
 * Ограничение, которое здесь не обойти: браузер выполнит запрос, только если
 * сервис разрешает обращения со страниц сайтов (CORS). Если не разрешает,
 * запрос не уйдёт, и мы сообщаем об этом понятным текстом — гадать
 * пользователю не придётся.
 */

import { AI } from '../config.js';
import { createTasks } from './ollama-tasks.js';
import { createOpenAiChat } from './openai-chat.js';
import { resolve } from './connection.js';

/** Человеческое объяснение вместо «Failed to fetch». */
export function explain(error, connection = resolve()) {
  const message = String(error?.message || error);
  const local = /localhost|127\.0\.0\.1/.test(connection.host || '');

  if (/Failed to fetch|NetworkError|load failed/i.test(message)) {
    return local
      ? 'Браузер не смог обратиться к сервису на этом компьютере. Проверьте, что приложение запущено и что ему разрешены обращения с этой страницы (для Ollama — переменная OLLAMA_ORIGINS).'
      : `Браузер не пустил запрос к ${connection.host || 'сервису'}: тот не разрешает обращения со страниц сайтов. Ключ здесь ни при чём — поможет запуск сервера PromptAI, он обращается к сервису сам.`;
  }
  if (/\b401\b|\b403\b/.test(message)) return 'Сервис не принял ключ. Проверьте, что он скопирован полностью и не отозван.';
  if (/\b404\b/.test(message)) return 'Не найдено: проверьте название модели и адрес сервиса.';
  if (/\b429\b/.test(message)) return 'Слишком много запросов или закончился лимит на счёте.';
  if (/aborted|timeout/i.test(message)) return 'Сервис не ответил вовремя. Попробуйте модель полегче.';
  return message;
}

/** Запрос к Ollama с ответом строго по JSON-схеме. */
async function ollamaChat({ system, user, schema }) {
  const connection = resolve();

  const response = await fetch(`${connection.host}/api/chat`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(connection.apiKey ? { Authorization: `Bearer ${connection.apiKey}` } : {}),
    },
    body: JSON.stringify({
      model: connection.model,
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
    throw new Error(`${response.status}: ${detail}`);
  }

  const content = JSON.parse(raw)?.message?.content;
  if (!content) throw new Error('Сервис вернул пустой ответ');

  return JSON.parse(content);
}

const openAiChat = createOpenAiChat({
  settings: resolve,
  timeoutMs: AI.timeoutMs,
});

/** Формат запроса выбирается по сервису. */
const chat = (request) => (resolve().api === 'openai' ? openAiChat(request) : ollamaChat(request));

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
