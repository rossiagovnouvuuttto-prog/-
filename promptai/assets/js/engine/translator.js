/**
 * Перевод русского описания в английскую основу промпта.
 *
 * Это не машинный перевод «слово в слово», а подготовка ключевых слов:
 * генераторам изображений важнее точные существительные и прилагательные,
 * чем грамматика. Порядок работы:
 *   1. заменяем устойчивые словосочетания;
 *   2. переводим слова по словарю, снимая падежные окончания;
 *   3. неизвестные слова транслитерируем, чтобы ничего не терялось.
 */

import { ENDINGS, FUNCTION_WORDS, PHRASES, PLURAL_ENDINGS, TRANSLIT, WORDS } from '../data/dictionary.js';
import { hasCyrillic, tidyPrompt } from '../utils.js';

/** Снимает падежное/числовое окончание, оставляя основу минимум из 3 букв. */
function stem(word) {
  const base = word.toLowerCase().replace(/ё/g, 'е');
  for (const ending of ENDINGS) {
    if (base.length - ending.length >= 3 && base.endsWith(ending)) {
      return base.slice(0, -ending.length);
    }
  }
  return base;
}

/** Индекс основ строится один раз при загрузке модуля. */
const STEM_INDEX = (() => {
  const index = new Map();
  for (const [key, value] of Object.entries(WORDS)) {
    const normalized = key.toLowerCase().replace(/ё/g, 'е');
    if (!index.has(normalized)) index.set(normalized, value);
    const base = stem(key);
    if (!index.has(base)) index.set(base, value);
  }
  return index;
})();

const FUNCTION_INDEX = new Map(
  Object.entries(FUNCTION_WORDS).map(([key, value]) => [key.toLowerCase().replace(/ё/g, 'е'), value]),
);

/** Множественное число английского существительного. */
function pluralize(word) {
  if (/\s/.test(word) || /s$/i.test(word)) return word;
  if (/(sh|ch|x|z)$/i.test(word)) return `${word}es`;
  if (/[^aeiou]y$/i.test(word)) return `${word.slice(0, -1)}ies`;
  return `${word}s`;
}

/** Русское слово стоит во множественном числе? */
function looksPlural(word) {
  return PLURAL_ENDINGS.some((ending) => word.length - ending.length >= 3 && word.endsWith(ending));
}

function transliterate(word) {
  return word
    .toLowerCase()
    .split('')
    .map((char) => (char in TRANSLIT ? TRANSLIT[char] : char))
    .join('');
}

/** Перевод одного слова. */
export function translateWord(word) {
  const normalized = word.toLowerCase().replace(/ё/g, 'е');

  if (FUNCTION_INDEX.has(normalized)) return FUNCTION_INDEX.get(normalized);

  const plural = looksPlural(normalized);
  const finish = (value) => (plural ? pluralize(value) : value);

  if (STEM_INDEX.has(normalized)) return STEM_INDEX.get(normalized);

  const base = stem(normalized);
  if (STEM_INDEX.has(base)) return finish(STEM_INDEX.get(base));

  // Пробуем снять ещё одну букву — помогает для форм вроде «городами».
  if (base.length > 4 && STEM_INDEX.has(base.slice(0, -1))) {
    return finish(STEM_INDEX.get(base.slice(0, -1)));
  }

  return transliterate(word);
}

/** Заменяет известные словосочетания на английские эквиваленты. */
function replacePhrases(text) {
  let result = text;
  const phrases = Object.keys(PHRASES).sort((a, b) => b.length - a.length);

  for (const phrase of phrases) {
    const pattern = new RegExp(`(^|[^a-zа-яё])${escapeRe(phrase)}([^a-zа-яё]|$)`, 'giu');
    result = result.replace(pattern, (_match, before, after) => `${before}⟦${PHRASES[phrase]}⟧${after}`);
  }
  return result;
}

function escapeRe(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

/**
 * Существительные словаря — ключи, которые не похожи на прилагательные и глаголы.
 * Нужны, чтобы аккуратно расставить артикли после предлогов.
 */
const NOUN_VALUES = new Set(
  Object.entries(WORDS)
    .filter(([key]) => !/(ый|ий|ой|ая|ое|ые|ье)$/.test(key))
    .filter(([key]) => !/(ит|ет|ёт|ут|ют|ся|ает|еет)$/.test(key))
    .map(([, value]) => value.toLowerCase()),
);

/** Неисчисляемые и «безартиклевые» слова. */
const UNCOUNTABLE = new Set([
  'fire', 'water', 'snow', 'rain', 'fog', 'haze', 'smoke', 'sand', 'ice', 'dust',
  'gold', 'silver', 'metal', 'glass', 'wood', 'blood', 'air', 'earth', 'space',
  'light', 'nature', 'art', 'food', 'coffee', 'tea', 'grass', 'moss', 'leather',
  'paper', 'fabric', 'silence', 'chaos', 'power', 'speed', 'memory', 'war',
  'night', 'day', 'morning', 'evening', 'midnight', 'dawn', 'sunset', 'twilight',
  'winter', 'summer', 'spring', 'autumn', 'anime', 'manga', 'wind', 'hair',
]);

const ARTICLE_PREPOSITIONS = 'in|on|under|above|near|behind|through|during|inside|around|with|of|inside';

/** «on cliff» → «on a cliff»: короткий проход для читаемости промпта. */
function addArticles(text) {
  return text.replace(
    new RegExp(`\\b(${ARTICLE_PREPOSITIONS})\\s+([a-z]+)\\b`, 'g'),
    (match, preposition, noun) => {
      if (!NOUN_VALUES.has(noun) || UNCOUNTABLE.has(noun) || /s$/.test(noun)) return match;
      const article = /^[aeiou]/.test(noun) ? 'an' : 'a';
      return `${preposition} ${article} ${noun}`;
    },
  );
}

/**
 * Переводит произвольное описание на английский.
 * Английский текст возвращается без изменений.
 */
export function translateToEnglish(text = '') {
  const source = String(text).trim();
  if (!source) return '';
  if (!hasCyrillic(source)) return tidyPrompt(source);

  // Составные слова через дефис («кот-астронавт») читаются как единое описание.
  const normalized = source.replace(/(\S)-(\S)/g, '$1 $2');
  const withPhrases = replacePhrases(normalized);
  const tokens = withPhrases.match(/⟦[^⟧]+⟧|[a-zA-Zа-яёА-ЯЁ0-9]+|[,.:;!?—-]/gu) || [];

  const out = [];
  for (const token of tokens) {
    if (token.startsWith('⟦')) {
      out.push(token.slice(1, -1));
      continue;
    }
    if (/^[,.:;!?—-]$/.test(token)) {
      if (out.length) out.push(token === '-' || token === '—' ? ',' : token);
      continue;
    }
    if (!/[а-яё]/i.test(token)) {
      out.push(token);
      continue;
    }

    const translated = translateWord(token);
    if (translated) out.push(translated);
  }

  return tidyPrompt(
    addArticles(
      out
        .join(' ')
        .replace(/\s+([,.:;!?])/g, '$1')
        .replace(/[.!?]+$/g, ''),
    ),
  );
}

/**
 * Переводит готовый промпт целиком: английские части не трогаются,
 * русские фрагменты заменяются на английские.
 */
export function translatePrompt(prompt = '') {
  return prompt
    .split(/(\n)/)
    .map((chunk) => (chunk === '\n' ? chunk : hasCyrillic(chunk) ? translateToEnglish(chunk) : chunk))
    .join('');
}
