/**
 * Сборка автономной версии сайта.
 *
 * Берёт index.html, встраивает в него CSS и все ES-модули и сохраняет
 * результат в promptai.html — один файл, который открывается двойным кликом,
 * без сервера и без интернета.
 *
 * Порядок модулей вычисляется из их импортов, поэтому новый файл
 * достаточно просто импортировать — правки сборщика не нужны.
 *
 * Запуск:  node build.mjs
 */

import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = dirname(fileURLToPath(import.meta.url));
const read = (file) => readFileSync(join(root, file), 'utf8');

const ENTRY = 'assets/js/app.js';
const IMPORT_RE = /import\s[\s\S]*?from\s*['"]([^'"]+)['"];/g;

/** Обход графа импортов: зависимости оказываются в списке раньше зависимых. */
function collect(file, seen = new Set(), order = []) {
  if (seen.has(file)) return order;
  seen.add(file);

  const source = read(file);
  const base = dirname(file);

  for (const [, specifier] of source.matchAll(IMPORT_RE)) {
    if (!specifier.startsWith('.')) {
      throw new Error(`${file}: внешний импорт «${specifier}» не поддерживается автономной сборкой`);
    }
    collect(relative(root, resolve(root, base, specifier)), seen, order);
  }

  order.push(file);
  return order;
}

/** Снимает import/export: в одном файле модульные границы больше не нужны. */
function flatten(source, file) {
  const body = source
    .replace(IMPORT_RE, '')
    .replace(/^export\s+(?=(const|let|function|async|class)\b)/gm, '')
    .trimEnd()
    .replace(/^\n+/, '');

  return `\n/* ────────── ${file} ────────── */\n\n${body}\n`;
}

/**
 * Имена верхнего уровня в модуле.
 *
 * В автономной сборке все модули попадают в одну область видимости, поэтому
 * два одинаковых имени в разных файлах ломают страницу целиком
 * («Identifier 'x' has already been declared»). Объявления в этих модулях
 * не имеют отступа, так что regexp по началу строки их надёжно находит.
 */
function declarations(source) {
  const names = [];
  const pattern = /^(?:export\s+)?(?:const|let|var|class|(?:async\s+)?function)\s+([A-Za-z_$][\w$]*)/gm;

  for (const [, name] of source.matchAll(pattern)) names.push(name);
  return names;
}

/** Падаем на сборке, а не в браузере у пользователя. */
function assertNoCollisions(files) {
  const owners = new Map();
  const clashes = [];

  for (const file of files) {
    for (const name of declarations(read(file))) {
      if (owners.has(name)) clashes.push(`${name}: ${owners.get(name)} и ${file}`);
      else owners.set(name, file);
    }
  }

  if (clashes.length) {
    throw new Error(
      `в автономной сборке совпали имена верхнего уровня:\n  ${clashes.join('\n  ')}\n` +
      'Переименуйте одно из них — модули склеиваются в общую область видимости.',
    );
  }
}

const modules = collect(ENTRY);
assertNoCollisions(modules);
const bundle = modules.map((file) => flatten(read(file), file)).join('\n');
const css = read('assets/css/styles.css');

// Замена задаётся функцией, а не строкой: иначе `$$` и `$&` внутри кода
// (например, в регулярках и в утилите $$) будут подставлены как спецсимволы.
const insert = (text) => () => text;

const html = read('index.html')
  .replace('<link rel="manifest" href="manifest.webmanifest">\n', '')
  .replace(
    '<link rel="stylesheet" href="assets/css/styles.css">',
    insert(`<style>\n${css}\n</style>`),
  )
  .replace(
    '<script type="module" src="assets/js/app.js"></script>',
    insert(`<script>\n(() => {\n'use strict';\n${bundle}\n})();\n</script>`),
  );

writeFileSync(join(root, 'promptai.html'), html, 'utf8');

console.log(`promptai.html собран: ${modules.length} модулей, ${(Buffer.byteLength(html) / 1024).toFixed(0)} КБ`);
modules.forEach((file, index) => console.log(`  ${String(index + 1).padStart(2)}. ${file}`));
