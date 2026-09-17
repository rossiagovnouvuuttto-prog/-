/**
 * Сборка автономной версии сайта.
 *
 * Берёт index.html, встраивает в него CSS и все ES-модули и сохраняет
 * результат в promptai.html — один файл, который открывается двойным кликом,
 * без сервера и без интернета.
 *
 * Запуск:  node build.mjs
 */

import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = dirname(fileURLToPath(import.meta.url));
const read = (file) => readFileSync(join(root, file), 'utf8');

/** Модули в порядке зависимостей: каждый следующий опирается на предыдущие. */
const MODULES = [
  'assets/js/config.js',
  'assets/js/utils.js',
  'assets/js/data/dictionary.js',
  'assets/js/data/styles.js',
  'assets/js/data/options.js',
  'assets/js/engine/translator.js',
  'assets/js/engine/prompt-builder.js',
  'assets/js/store.js',
  'assets/js/auth.js',
  'assets/js/quota.js',
  'assets/js/ai/local-provider.js',
  'assets/js/ai/api-provider.js',
  'assets/js/ai/provider.js',
  'assets/js/ui/icons.js',
  'assets/js/ui/toast.js',
  'assets/js/ui/modal.js',
  'assets/js/ui/components.js',
  'assets/js/app.js',
];

/** Снимает import/export: в одном файле модульные границы больше не нужны. */
function flatten(source, file) {
  const body = source
    .replace(/import\s[\s\S]*?from\s*['"][^'"]+['"];\n?/g, '')
    .replace(/^export\s+(?=(const|let|function|async|class)\b)/gm, '')
    .trimEnd();

  return `\n/* ────────── ${file} ────────── */\n\n${body}\n`;
}

const bundle = MODULES.map((file) => flatten(read(file), file)).join('\n');
const css = read('assets/css/styles.css');

let html = read('index.html');

// Внешние ресурсы заменяем встроенными.
// Замена задаётся функцией, а не строкой: иначе `$$` и `$&` внутри кода
// (например, в регулярках и в утилите $$) будут подставлены как спецсимволы.
const insert = (text) => () => text;

html = html
  .replace('<link rel="manifest" href="manifest.webmanifest">\n', '')
  .replace(
    '<link rel="stylesheet" href="assets/css/styles.css">',
    insert(`<style>\n${css}\n</style>`),
  )
  .replace(
    '<script type="module" src="assets/js/app.js"></script>',
    insert(`<script>\n(() => {\n'use strict';\n${bundle}\n})();\n</script>`),
  );

// Иконка в .webmanifest тоже больше не нужна — favicon уже встроен как data-URI.
writeFileSync(join(root, 'promptai.html'), html, 'utf8');

const size = (Buffer.byteLength(html) / 1024).toFixed(0);
console.log(`promptai.html собран: ${MODULES.length} модулей, ${size} КБ`);
