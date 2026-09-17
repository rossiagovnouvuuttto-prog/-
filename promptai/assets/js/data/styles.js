/**
 * Стили изображения.
 *
 * Каждый стиль — это готовый набор английских формулировок, из которых
 * собирается промпт: модификаторы стиля, типичное окружение, освещение,
 * детализация и negative prompt.
 */

export const STYLES = [
  {
    id: 'realism',
    name: 'Реализм',
    icon: '📷',
    desc: 'Фотография как есть',
    gradient: 'linear-gradient(135deg, #38bdf8, #0ea5e9)',
    modifiers: ['photorealistic', 'hyperrealistic', 'shot on Canon EOS R5, 85mm lens'],
    environments: [
      'natural surroundings with realistic depth of field',
      'lifelike setting with authentic textures',
      'real-world location with atmospheric perspective',
    ],
    lighting: 'soft natural light',
    detail: ['ultra realistic', 'sharp focus', 'fine skin and material texture'],
    negative: 'cartoon, illustration, plastic skin, distorted anatomy, watermark',
  },
  {
    id: 'anime',
    name: 'Аниме',
    icon: '🌸',
    desc: 'Японская анимация',
    gradient: 'linear-gradient(135deg, #f472b6, #a855f7)',
    modifiers: ['anime style', 'Japanese animation', 'cel shaded', 'clean line art'],
    environments: [
      'vibrant anime background with expressive skies',
      'detailed anime cityscape',
      'pastel scenery with soft gradients',
    ],
    lighting: 'bright rim lighting',
    detail: ['highly detailed', 'crisp linework', 'vivid saturated colors'],
    negative: 'photorealistic, 3d render, blurry lines, extra fingers, text',
  },
  {
    id: 'cyberpunk',
    name: 'Киберпанк',
    icon: '🌃',
    desc: 'Неон и будущее',
    gradient: 'linear-gradient(135deg, #22d3ee, #a855f7)',
    modifiers: ['cyberpunk style', 'futuristic dystopia', 'blade runner aesthetic'],
    environments: [
      'rainy streets with neon lights',
      'dense megacity with holographic billboards',
      'neon-soaked alley with wet reflective asphalt',
    ],
    lighting: 'neon lights, volumetric lighting',
    detail: ['ultra detailed', 'reflections and wet surfaces', 'chromatic aberration'],
    negative: 'daylight, rustic, medieval, low contrast, watermark',
  },
  {
    id: 'fantasy',
    name: 'Фэнтези',
    icon: '🐉',
    desc: 'Магия и эпос',
    gradient: 'linear-gradient(135deg, #34d399, #6366f1)',
    modifiers: ['epic fantasy art', 'matte painting', 'mythical atmosphere'],
    environments: [
      'ancient magical landscape with floating particles',
      'enchanted forest wrapped in mist',
      'legendary kingdom under a vast sky',
    ],
    lighting: 'golden magical glow',
    detail: ['intricate details', 'epic scale', 'rich ornamental design'],
    negative: 'modern clothing, cars, urban signage, low detail',
  },
  {
    id: '3d',
    name: '3D',
    icon: '🧊',
    desc: 'Объёмный рендер',
    gradient: 'linear-gradient(135deg, #818cf8, #22d3ee)',
    modifiers: ['3D render', 'Octane render', 'Blender cycles', 'subsurface scattering'],
    environments: [
      'clean studio environment with soft shadows',
      'stylized 3D scene with depth of field',
      'isometric 3D set with polished materials',
    ],
    lighting: 'studio lighting with soft shadows',
    detail: ['ray tracing', 'physically based materials', 'high poly detail'],
    negative: 'flat shading, jpeg artifacts, noisy render, text',
  },
  {
    id: 'cinema',
    name: 'Кино',
    icon: '🎬',
    desc: 'Кадр из фильма',
    gradient: 'linear-gradient(135deg, #fbbf24, #f97316)',
    modifiers: ['cinematic film still', 'movie scene', 'anamorphic lens, 35mm film grain'],
    environments: [
      'dramatic location with atmospheric haze',
      'cinematic set with layered background',
      'moody environment with shallow depth of field',
    ],
    lighting: 'cinematic lighting, dramatic contrast',
    detail: ['cinematic color grading', 'high dynamic range', 'film-like detail'],
    negative: 'flat lighting, amateur snapshot, oversaturated, watermark',
  },
  {
    id: 'art',
    name: 'Арт',
    icon: '🎨',
    desc: 'Цифровая живопись',
    gradient: 'linear-gradient(135deg, #fb7185, #f59e0b)',
    modifiers: ['digital painting', 'concept art', 'expressive brush strokes', 'trending on ArtStation'],
    environments: [
      'painterly background with bold composition',
      'stylized scene with textured brushwork',
      'artistic environment with strong silhouette',
    ],
    lighting: 'dramatic painterly light',
    detail: ['masterpiece', 'rich color palette', 'detailed brush texture'],
    negative: 'photo, 3d render, flat colors, low effort sketch',
  },
];

export const STYLE_MAP = Object.fromEntries(STYLES.map((style) => [style.id, style]));

export const getStyle = (id) => STYLE_MAP[id] || STYLES[0];
