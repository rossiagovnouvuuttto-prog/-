# ABOBUS123 — контекст порта на Minecraft 1.21.11 Fabric

## Промпт для начала работы

> Продолжаю портирование моего клиентского мода ABOBUS123 с Minecraft 1.18.2
> на 1.21.11 Fabric. Порт компиляции ЗАВЕРШЁН — было 951 ошибка, стало 0,
> JAR собирается. Осталась доводка миксинов по логам запуска игры.
>
> Проект в репозитории. Сборка идёт через GitHub Actions (файл
> .github/workflows/build.yml), потому что локально скачать Minecraft
> с Maven может не получиться.
>
> Читай ABOBUS123-CONTEXT.md — там версии, инструменты и список
> уже потерянных функций. Не удаляй функции без причины и не откатывай
> defaultRequire: 1 в reallyvisuals.mixins.json.

---

## Версии (проверены по jar'у, не угаданы)

| Компонент | Значение |
|---|---|
| Minecraft | 1.21.11 |
| Yarn | 1.21.11+build.4 |
| Fabric Loader | 0.18.1 |
| Fabric API | 0.141.6+1.21.11 |
| Loom | 1.14 |
| Gradle | 9.7.1 |
| Java | 21 |

## КРИТИЧНО

`reallyvisuals.mixins.json` → `injectors.defaultRequire = 1`.

С нулём несовпавшие инъекции отключаются МОЛЧА. За время порта так
нашлось больше десяти миксинов, целившихся в удалённые методы, —
компилятор их не видит в принципе, только рантайм.

## Инструменты в build.gradle

Задачи, написанные под этот порт. Спрашивают живой замапленный jar,
а не догадки:

```
./gradlew sig2   -Pcls=<класс> [-Pm=<фильтр>]   # сигнатуры методов
./gradlew findCls -Pq=<подстрока>               # где лежит класс
./gradlew findRet -Pt=<тип>                     # кто возвращает тип
./gradlew auditMixins                           # сверить ВСЕ инъекции разом
```

`auditMixins` — главный. Показывает по каждому миксину, существует ли
целевой класс и метод. Экономит цикл «сборка → запуск → краш».

## Новые файлы порта

- `gui/render/Buf.java` — прослойка вместо удалённого immediate-mode
  (Tessellator + RenderSystem.setShader). Держит старый API поверх
  VertexConsumerProvider + RenderLayers.
- `WorldRenderHandler.java` — WorldRenderEvents.AFTER_ENTITIES вместо
  инъекции в WorldRenderer.render.
- `assets/really/textures/gui/disc.png` — сглаженный диск; его квадранты
  дают скруглённые углы вместо удалённого SDF-шейдера.

## Ключевые замены API

| Было (1.18.2) | Стало (1.21.11) |
|---|---|
| Tessellator + setShader | Buf → VertexConsumerProvider + RenderLayers |
| MatrixStack в GUI | DrawContext + Matrix3x2fStack (int-координаты!) |
| RenderLayer.getLines() | RenderLayers.LINES |
| new Identifier() | Identifier.of() |
| Vec3f | org.joml + RotationAxis |
| mouseClicked(x,y,btn) | mouseClicked(Click, boolean) |
| keyPressed(k,s,m) | keyPressed(KeyInput) |
| onMouseButton(w,b,a,m) | onMouseButton(w, MouseInput, a) |
| поля GameOptions | SimpleOption + геттеры |
| NBT шалкера | DataComponentTypes.CONTAINER |
| getSkinTexture() | getSkin().body().texturePath() |
| GameProfile.getName() | .name() |
| entity.getPos() | getEntityWorld() / getEntityPos() |
| isFallFlying() | isGliding() |
| isPermanent() | isInfinite() |
| PositionedSoundInstance.master | .ui() |
| TextRenderer.drawWithShadow | context.drawTextWithShadow (int!) |

## Потерянные функции (цели удалены в 1.21.11)

| Функция | Причина |
|---|---|
| Скайбокс и цвет неба CustomWorld | renderSky, getSkyColor удалены |
| Дальность тумана + NoFluid | setShaderFogStart/End убраны |
| Модель биты SwordBat | BakedModel удалён |
| Режим «Свеня» в TargetESP | нужен OrderedRenderCommandQueue |
| Подписи путевых точек | в мире нет DrawContext (луч маяка работает) |
| Строки клиента в F3 | DebugHud.getLeftText удалён |
| Подсветка предметов на земле | ItemEntityRenderer.renderLayer удалён |
| Анимация чанков | WorldRenderer.render переписан |
| Отсечение сущностей | render получает EntityRenderState, не Entity |
| Sodium-миксины | целятся в Sodium 0.4.x, классов нет |

Возвращаемы при желании: подписи точек (проекция на экран в HUD-проходе),
«Свеня» (протянуть commandQueue из WorldRenderEvents).

## Изменения поведения (компилятор не поймает)

- Скругления: текстура диска вместо SDF — на больших радиусах мягче
- Кольца: сегменты вместо стенсила — возможна ступенчатость
- Голова цели в TargetHud: только у игроков, мобы → запасной квадрат
- Анимация чата: общий сдвиг блока вместо построчного
- PerformanceBoost при выключении вернёт графику на FANCY

## Что осталось

Прогнать `auditMixins`, собрать, запустить игру, править несовпадения
сигнатур по логу. Крупных несовпадений на момент паузы не осталось.
