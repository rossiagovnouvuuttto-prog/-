/**
 * Сервер PromptAI.
 *
 * Делает две вещи:
 *   1. отдаёт статические файлы сайта;
 *   2. принимает POST /api/prompt и ходит в Ollama, держа ключ у себя.
 *
 * Зависимостей нет — нужен только Node 18 или новее.
 *
 * Запуск:
 *   OLLAMA_API_KEY=ваш_ключ node server/server.mjs
 *   node server/server.mjs                # локальная Ollama, ключ не нужен
 */

import { createReadStream, readFileSync } from 'node:fs';
import { stat } from 'node:fs/promises';
import { createServer } from 'node:http';
import { dirname, extname, join, normalize, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..');

/**
 * Читает ключ из файла .env рядом с сайтом.
 *
 * Так проще, чем возиться с переменными окружения, особенно в Windows:
 * достаточно положить рядом файл с одной строкой OLLAMA_API_KEY=...
 * Переменные, заданные в терминале, имеют приоритет и не перезаписываются.
 */
function loadEnv() {
  try {
    for (const line of readFileSync(join(ROOT, '.env'), 'utf8').split('\n')) {
      const match = line.match(/^\s*([A-Z_][A-Z0-9_]*)\s*=\s*(.*)\s*$/i);
      if (!match) continue;

      const [, name, rawValue] = match;
      if (process.env[name]) continue;

      process.env[name] = rawValue.trim().replace(/^["']|["']$/g, '');
    }
  } catch {
    // Файла нет — это нормально: ключ можно задать и переменной окружения.
  }
}

loadEnv();

// Импорт после loadEnv: модуль читает настройки при загрузке.
const { TASKS, host, isCloud, model, ping } = await import('./ollama.mjs');

const PORT = Number(process.env.PORT || 4173);

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.mjs': 'text/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.webmanifest': 'application/manifest+json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.ico': 'image/x-icon',
};

/* ────────────────────────  утилиты  ──────────────────────── */

function sendJson(response, status, payload) {
  const body = JSON.stringify(payload);
  response.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
    'Content-Length': Buffer.byteLength(body),
    'Cache-Control': 'no-store',
  });
  response.end(body);
}

async function readBody(request, limit = 64 * 1024) {
  const chunks = [];
  let size = 0;

  for await (const chunk of request) {
    size += chunk.length;
    if (size > limit) throw new Error('Слишком большой запрос');
    chunks.push(chunk);
  }

  return chunks.length ? JSON.parse(Buffer.concat(chunks).toString('utf8')) : {};
}

/* ────────────────────────  статика  ──────────────────────── */

async function serveStatic(request, response) {
  const url = new URL(request.url, 'http://localhost');
  const relative = url.pathname === '/' ? 'index.html' : decodeURIComponent(url.pathname).slice(1);

  // normalize + проверка префикса: наружу из папки сайта не выпускаем.
  const target = join(ROOT, normalize(relative));
  if (!target.startsWith(ROOT)) {
    response.writeHead(403).end('Forbidden');
    return;
  }

  try {
    const info = await stat(target);
    if (!info.isFile()) throw new Error('not a file');

    response.writeHead(200, {
      'Content-Type': MIME[extname(target)] || 'application/octet-stream',
      'Content-Length': info.size,
      'Cache-Control': 'no-cache',
    });
    createReadStream(target).pipe(response);
  } catch {
    response.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' }).end('Не найдено');
  }
}

/* ────────────────────────  сервер  ──────────────────────── */

const server = createServer(async (request, response) => {
  const url = new URL(request.url, 'http://localhost');

  if (url.pathname === '/api/health') {
    sendJson(response, 200, {
      ok: true,
      mode: isCloud() ? 'cloud' : 'local',
      model: model(),
      host: host(),
    });
    return;
  }

  if (url.pathname === '/api/prompt') {
    if (request.method !== 'POST') {
      sendJson(response, 405, { error: 'Нужен POST' });
      return;
    }

    let payload;
    try {
      payload = await readBody(request);
    } catch (error) {
      sendJson(response, 400, { error: error.message });
      return;
    }

    const task = TASKS[payload.task];
    if (!task) {
      sendJson(response, 400, { error: `Неизвестная задача: ${payload.task}` });
      return;
    }

    try {
      const started = Date.now();
      const result = await task(payload);
      console.log(`  ${payload.task} — ${Date.now() - started} мс`);
      sendJson(response, 200, result);
    } catch (error) {
      // 502 + текст ошибки: фронтенд по этому сигналу переходит
      // на встроенный движок, а в логах видно настоящую причину.
      console.error(`  ${payload.task} — ошибка: ${error.message}`);
      sendJson(response, 502, { error: error.message });
    }
    return;
  }

  if (request.method === 'GET' || request.method === 'HEAD') {
    await serveStatic(request, response);
    return;
  }

  response.writeHead(405).end();
});

server.listen(PORT, async () => {
  console.log(`\n  PromptAI — http://localhost:${PORT}\n`);
  console.log(`  Режим:  ${isCloud() ? 'облако Ollama' : 'локальная Ollama'}`);
  console.log(`  Адрес:  ${host()}`);
  console.log(`  Модель: ${model()}`);

  if (isCloud()) console.log('  Ключ:   задан через OLLAMA_API_KEY');
  else if (!process.env.OLLAMA_HOST) console.log('  Ключ:   не задан — используется Ollama на этом компьютере');

  process.stdout.write('\n  Проверяю связь с моделью... ');

  try {
    await ping();
    console.log('готово. AI подключён.\n');
  } catch (error) {
    console.log('не удалось.');
    console.log(`  Причина: ${error.message}`);
    console.log('\n  Сайт продолжит работать на встроенном движке.');
    console.log('  Что проверить:');
    console.log('    • ключ со страницы ollama.com/settings/keys передан в OLLAMA_API_KEY;');
    console.log('    • модель существует — список облачных моделей на ollama.com/search?c=cloud;');
    console.log('    • другую модель можно задать через OLLAMA_MODEL.\n');
  }
});
