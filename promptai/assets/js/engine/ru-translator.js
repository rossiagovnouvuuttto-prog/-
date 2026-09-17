/**
 * Обратный перевод: готовый английский промпт → русский текст.
 *
 * Нужен, чтобы пользователь понимал, что именно он копирует. Сам промпт
 * при этом остаётся английским — генераторы изображений работают с ним.
 *
 * Порядок поиска для каждого фрагмента:
 *   1. точное соответствие из FRAGMENTS_RU — там лежат все формулировки,
 *      которые способен выдать движок, поэтому основной текст переводится
 *      дословно и грамотно;
 *   2. обратный словарь WORDS — для описания, которое ввёл пользователь;
 *   3. если ничего не нашлось, фрагмент остаётся как есть.
 */

import { FRAGMENTS_RU } from '../data/fragments-ru.js';
import { WORDS } from '../data/dictionary.js';
import { hasCyrillic } from '../utils.js';

/** Синонимы, которых нет среди значений основного словаря. */
const EXTRA_WORDS = {
  big: 'большой', little: 'маленький', nice: 'красивый', lovely: 'милый',
  giant: 'гигантский', tall: 'высокий', dark: 'тёмный', light: 'светлый',
  shiny: 'блестящий', glowing: 'светящийся', flying: 'летающий',
  standing: 'стоящий', sitting: 'сидящий', sleeping: 'спящий',
  running: 'бегущий', burning: 'горящий', floating: 'парящий',
  guy: 'парень', lady: 'женщина', kid: 'ребёнок', view: 'вид',
  one: 'один', two: 'два', three: 'три', four: 'четыре', five: 'пять',
  ten: 'десять', many: 'много', several: 'несколько',
  roof: 'крыша', sunset: 'закат', sunrise: 'рассвет', clouds: 'облака',
};

/** Английское значение → русское слово. Первое вхождение выигрывает. */
const REVERSE_WORDS = (() => {
  const index = new Map();
  for (const [ru, en] of Object.entries(WORDS)) {
    const key = en.toLowerCase();
    if (!index.has(key)) index.set(key, ru);
  }
  for (const [en, ru] of Object.entries(EXTRA_WORDS)) {
    if (!index.has(en)) index.set(en, ru);
  }
  return index;
})();

/** Самая длинная фраза в обратном словаре — граница для жадного поиска. */
const MAX_PHRASE = Math.max(...[...REVERSE_WORDS.keys()].map((key) => key.split(' ').length));

const ARTICLES = new Set(['a', 'an', 'the']);


/**
 * Предлоги, которые в пересказе заменяются запятой.
 *
 * Русский предлог требует падежа («на скале», а не «на скала»), а склонять
 * слова движок не умеет. Поэтому описание, введённое по-английски, мы не
 * собираем в фразу, а показываем ключевыми словами через запятую — ровно
 * так, как их читает генератор изображений.
 */
const SEPARATORS = new Set([
  'in', 'on', 'at', 'under', 'above', 'over', 'with', 'without', 'of', 'from',
  'to', 'into', 'near', 'behind', 'before', 'between', 'among', 'around',
  'through', 'during', 'inside', 'outside', 'against', 'and', 'or',
]);

/** Слова мужского рода с «женским» окончанием — исключения для согласования. */
const MASCULINE_EXCEPTIONS = new Set(['мужчина', 'дедушка', 'папа', 'юноша', 'судья', 'коллега', 'слуга']);

/** Английское слово стоит во множественном числе? */
const isPluralEn = (word) => /s$/i.test(word) && !/(ss|us|is|ous)$/i.test(word);

/** Русское слово уже во множественном числе? В словаре такие оканчиваются на -ы/-и. */
const isPluralRu = (word) => /(ы|и)$/.test(word);

/** Английское слово в единственном числе: cars → car, cities → city, boxes → box. */
function singularizeEn(word) {
  if (/ies$/i.test(word)) return `${word.slice(0, -3)}y`;
  if (/(ch|sh|x|z|s)es$/i.test(word)) return word.slice(0, -2);
  if (/s$/i.test(word)) return word.slice(0, -1);
  return word;
}

/** После шипящих и заднеязычных вместо -ы/-ый пишется -и/-ий. */
const isSoftStem = (stem) => /[гкхжчшщ]$/.test(stem);

/** Множественное число существительного: машина → машины, облако → облака. */
function pluralizeRu(word) {
  if (isPluralRu(word)) return word;
  if (/(а|я)$/.test(word)) {
    const stem = word.slice(0, -1);
    if (word.endsWith('я')) return `${stem}и`;
    return stem + (isSoftStem(stem) ? 'и' : 'ы');
  }
  if (/о$/.test(word)) return `${word.slice(0, -1)}а`;
  if (/е$/.test(word)) return `${word.slice(0, -1)}я`;
  if (/ь$/.test(word)) return `${word.slice(0, -1)}и`;
  return word + (isSoftStem(word) ? 'и' : 'ы');
}

/** Род существительного по окончанию. */
function gender(word) {
  const value = word.toLowerCase();
  if (MASCULINE_EXCEPTIONS.has(value)) return 'm';
  if (/(а|я)$/.test(value)) return 'f';
  if (/(о|е|ё)$/.test(value)) return 'n';
  return 'm';
}

/** Согласование прилагательного с существительным: «красный планета» → «красная планета». */
function agree(adjective, noun, plural) {
  if (plural) {
    if (!/(ый|ой|ий)$/.test(adjective)) return adjective;
    const stem = adjective.slice(0, -2);
    return stem + (isSoftStem(stem) ? 'ие' : 'ые');
  }

  const form = gender(noun);
  if (form === 'm') return adjective;

  const soft = isSoftStem(adjective.slice(0, -2));

  if (/(ый|ой)$/.test(adjective)) return adjective.slice(0, -2) + (form === 'f' ? 'ая' : 'ое');
  if (/ий$/.test(adjective)) {
    return adjective.slice(0, -2) + (form === 'f' ? (soft ? 'ая' : 'яя') : (soft ? 'ее' : 'ее'));
  }
  return adjective;
}

const SMALL_NUMERALS = new Set(['два', 'три', 'четыре']);

/** Родительный падеж единственного числа: робот → робота, машины → машины. */
function afterNumeral(word) {
  // Готовые формы множественного числа («цветы») склонять нечем — оставляем.
  if (/(ы|и)$/.test(word)) return word;
  if (/(а|я)$/.test(word)) {
    const stem = word.slice(0, -1);
    return stem + (word.endsWith('я') ? 'и' : isSoftStem(stem) ? 'и' : 'ы');
  }
  if (/(о|е)$/.test(word)) return `${word.slice(0, -1)}а`;
  if (/ь$/.test(word)) return `${word.slice(0, -1)}я`;
  return `${word}а`;
}

/** Согласует каждую пару «прилагательное + существительное». */
function agreeAll(tokens) {
  return tokens.map((token, index) => {
    const previous = tokens[index - 1];
    if (previous && SMALL_NUMERALS.has(previous.text)) {
      return { ...token, text: afterNumeral(token.singular || token.text) };
    }

    const next = tokens[index + 1];
    if (!next || next.separator || !/(ый|ий|ой)$/.test(token.text)) return token;
    return { ...token, text: agree(token.text, next.text, next.plural) };
  });
}

/** Пословный перевод: жадно ищем самое длинное совпадение в обратном словаре. */
function translateWords(fragment) {
  const words = fragment.split(/\s+/).filter(Boolean);
  const tokens = [];
  let i = 0;

  while (i < words.length) {
    let matched = false;

    for (let size = Math.min(MAX_PHRASE, words.length - i); size > 0; size -= 1) {
      const source = words.slice(i, i + size).join(' ');
      const key = source.toLowerCase().replace(/[.,!?]/g, '');
      const direct = REVERSE_WORDS.get(key);
      const viaSingular = direct ? undefined : REVERSE_WORDS.get(singularizeEn(key));
      const hit = direct || viaSingular;

      if (hit) {
        const plural = isPluralEn(source) || isPluralRu(hit);
        // Склоняем только то, что нашлось по единственному числу: в словаре
        // формы вроде «деревья» и «облака» уже стоят во множественном.
        const text = viaSingular && isPluralEn(source) ? pluralizeRu(hit) : hit;
        tokens.push({ text, plural, singular: viaSingular ? hit : undefined });
        i += size;
        matched = true;
        break;
      }
    }

    if (!matched) {
      const word = words[i];
      const plain = word.toLowerCase().replace(/[.,!?]/g, '');

      if (SEPARATORS.has(plain)) tokens.push({ text: ',', separator: true });
      else if (!ARTICLES.has(plain)) tokens.push({ text: word, plural: isPluralEn(word) });
      i += 1;
    }
  }

  return agreeAll(tokens)
    .map((token) => token.text)
    .join(' ')
    .replace(/\s+,/g, ',')
    .replace(/(,\s*)+/g, ', ')
    .replace(/^[,\s]+|[,\s]+$/g, '');
}

/** Перевод одного фрагмента промпта. */
export function translateFragment(fragment) {
  const value = fragment.trim();
  if (!value) return '';
  if (hasCyrillic(value)) return value;

  const exact = FRAGMENTS_RU[value.toLowerCase()];
  if (exact) return exact;

  return translateWords(value);
}

/**
 * Перевод всего промпта с сохранением структуры по строкам.
 *
 * @param {string} text   — промпт (многострочный или в одну строку)
 * @param {object} [options]
 * @param {string} [options.idea] — исходное описание пользователя: если оно
 *        на русском, первая строка берётся из него без перевода.
 */
export function translateToRussian(text, { idea = '' } = {}) {
  const original = String(idea).trim();
  const useIdea = original && hasCyrillic(original);
  let isFirst = true;

  return String(text)
    .split('\n')
    .map((line) => line
      .split(/,\s*/)
      .map((fragment) => {
        if (!fragment.trim()) return '';

        if (isFirst) {
          isFirst = false;
          if (useIdea) return original.replace(/[.!?]+$/, '');
        }
        return translateFragment(fragment);
      })
      .filter(Boolean)
      .join(', '))
    .filter(Boolean)
    .join('\n');
}
