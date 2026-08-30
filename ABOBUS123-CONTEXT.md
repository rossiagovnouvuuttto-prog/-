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
./gradlew dump   -Pcls=<класс> [-Pm=<фильтр>]   # + поля и родительские классы
./gradlew findCls -Pq=<подстрока>               # где лежит класс
./gradlew findRet -Pt=<тип>                     # кто возвращает тип
./gradlew auditMixins                           # сверить ВСЕ инъекции разом
```

Если `maven.fabricmc.net` недоступен локально, всё это гоняется в CI:
`.github/workflows/audit.yml` (вкладка Actions → audit). Там же лежит
готовый список классов для `dump`.

Проект лежит в подкаталоге `ABOBUS123-1.21.11/`, поэтому оба workflow
используют `working-directory`.

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

## Проход по миксинам (сверено с замапленным jar через CI)

Несовпадений remap было 12 → стало 4. Оставшиеся 4 сидят в миксинах,
которые не подключены ни к одному конфигу, то есть в рантайм не идут.

### Починено

| Было | Стало | Что вернулось |
|---|---|---|
| `InGameHud.scaledWidth/scaledHeight` | полей нет → `DrawContext.getScaledWindowWidth/Height()` | HUD, скорборд, ClickPearl |
| `GameRenderer.bobViewWhenHurt` | `tiltViewWhenHurt` | твик «Тряска урона» |
| `ClientWorld.playSound(...FFZ)` | `(...FFZJ)` — добавился `long seed` | Hit Sounds: снятие ванильного крита |
| `PlayerEntityRenderer.getTexture(Entity)` | `getTexture(PlayerEntityRenderState)` | Player Skins |
| `PlayerEntityRenderer.scale(Entity,…,float)` | `scale(PlayerEntityRenderState, MatrixStack)` | морф Player Skins |
| `PlayerEntityRenderer.render` (метода нет) | `ChinaHat.render3D` из `WorldRenderHandler` | China Hat (локальный игрок) |
| `World.getTime` | метода нет; `getTimeOfDay` уже перехвачен | Time Changer не пострадал |
| `Screen.renderTooltip` | метода нет; `HandledScreen.drawMouseoverTooltip` жив | Shulker Preview не пострадал |

Снят `require = 0` со всех инъекций, чьи цели подтверждены аудитом, —
теперь они падают громко, а не отключаются молча. Это тот же принцип,
что и `defaultRequire: 1`, только на уровне отдельной инъекции.

### Осталось потерянным (цели правда удалены)

`DebugHud.getLeftText`, `WorldRenderer.renderLayer` ×2,
`ChunkBuilder$BuiltChunk.setOrigin` — строки F3 и анимация чанков,
уже были в списке выше.

### Миксины-сироты (файл есть, в конфиге нет → не грузятся)

`EntityRenderDispatcherMixin`, `ClientWorldPropertiesMixin`,
`BuiltChunkMixin`, `LivingEntityMixin`, `ItemEntityRendererMixin`,
`DebugHudMixin`, `accessor.WorldRendererAccessor`.
Оставлены как заготовки для восстановления; кода они сейчас не меняют.

`reallyvisuals.sodium.mixins.json` и `reallyvisuals.vanillachunks.mixins.json`
не перечислены в `fabric.mod.json`, поэтому тоже не грузятся.

### Выпотрошены до пустых классов при порте (в списке потерь их не было)

`ArmorFeatureRendererMixin`, `FeatureRendererMixin`, `OverlayTextureMixin` —
остались `public class X {}` без единой инъекции. Что именно они делали
в 1.18.2, по текущему исходнику не восстановить: нужен старый проект.

## Что осталось

Запустить игру и пройти лог. China Hat теперь рисуется только на своём
игроке — на чужих нужен `OrderedRenderCommandQueue`, тот же блокер,
что и у режима «Свеня».
