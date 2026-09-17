/**
 * Проверяет, что для каждой английской формулировки, которую способен выдать
 * движок, есть русский перевод. Запуск: node tests/fragments.test.mjs
 */

import { FRAGMENTS_RU } from '../assets/js/data/fragments-ru.js';
import { ENHANCERS, SETTING_GROUPS } from '../assets/js/data/options.js';
import { STYLES } from '../assets/js/data/styles.js';
import { buildPrompt, buildVariants, enhancePrompt } from '../assets/js/engine/prompt-builder.js';
import { translateToRussian } from '../assets/js/engine/ru-translator.js';

const fragments = new Set();
const add = (value) => String(value)
  .split(/,\s*/)
  .map((part) => part.trim())
  .filter(Boolean)
  .forEach((part) => fragments.add(part.toLowerCase()));

for (const style of STYLES) {
  style.modifiers.forEach(add);
  style.environments.forEach(add);
  add(style.lighting);
  style.detail.forEach(add);
  add(style.negative);
}
for (const group of SETTING_GROUPS) group.options.forEach((option) => add(option.prompt));
for (const group of Object.values(ENHANCERS)) group.forEach(add);

// Формулировки, которые движок собирает на лету.
['glowing neon accents', 'warm evening sky'].forEach(add);
for (const style of STYLES) {
  for (const variant of buildVariants({ idea: 'кот', settings: { style: style.id }, count: 5 })) {
    // Первый фрагмент — описание пользователя, его переводить не нужно.
    add(variant.prompt.split(/,\s*/).slice(1).join(', '));
  }
}

const missing = [...fragments].filter((fragment) => !FRAGMENTS_RU[fragment]);

let failed = 0;

if (missing.length) {
  failed += 1;
  console.error(`✗ без перевода осталось фрагментов: ${missing.length}`);
  missing.forEach((fragment) => console.error(`    '${fragment}':`));
} else {
  console.log(`✓ все ${fragments.size} фрагментов переведены`);
}

// Полный прогон: в переводе не должно остаться английских слов.
const settings = { style: 'cyberpunk', camera: 'drone', lighting: 'sunset', quality: '8k', aspect: '9:16' };
const idea = 'старый маяк на скале во время шторма';
const built = buildPrompt({ idea, settings });
const improved = enhancePrompt(built.prompt, built.multiline);
const russian = translateToRussian(improved.multiline, { idea });
const leftovers = russian.match(/\b[a-z]{4,}\b/gi) || [];
const allowed = new Set(['artstation', 'canon', 'eos', 'octane', 'blender', 'cycles', 'jpeg']);
const unexpected = leftovers.filter((word) => !allowed.has(word.toLowerCase()));

if (unexpected.length) {
  failed += 1;
  console.error('✗ в переводе остались английские слова:', [...new Set(unexpected)].join(', '));
} else {
  console.log('✓ перевод промпта полностью на русском');
}

console.log('\n--- пример перевода ---\n' + russian);
process.exit(failed ? 1 : 0);
