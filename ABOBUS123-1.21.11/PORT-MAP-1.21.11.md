# ABOBUS123 — карта портирования 1.18.2 → 1.21.11

Замер по твоему исходнику: **147 .java файлов, 15 052 строки, 86 точек инъекции миксинов.**

Между 1.18.2 и 1.21.11 лежит **9 ломающих релизов**. Ниже — что именно
в твоём коде умрёт и на что это меняется. Цифры — реальные вхождения,
посчитанные grep'ом по твоему архиву.

---

## 0. Версии (проверено по fabricmc.net и maven.fabricmc.net)

| Компонент | Значение |
|---|---|
| Minecraft | `1.21.11` (последняя обфусцированная версия) |
| Yarn | `1.21.11+build.1` (последний Yarn вообще) |
| Fabric Loader | `0.18.1` |
| Fabric API | `0.141.6+1.21.11` |
| Loom | `1.14-SNAPSHOT` |
| **Java** | **21** (не 17) |
| Gradle | 8.14 (твой wrapper подходит) |

Готовые файлы — в папке `config-1.21.11/`.

> Важно: следующая версия — `26.1`, необфусцированная. Yarn для неё
> не будет. Если планируешь жить дальше 1.21.11 — переходи сразу
> на Mojang mappings, иначе портировать придётся дважды.

---

## 1. Что ломается — по слоям

### 1.1 Рендер 2D/GUI — переписывается полностью

| Твой код | Вхождений | Умерло в | Замена в 1.21.11 |
|---|---|---|---|
| `RenderSystem.enableTexture()` / `disableTexture()` | 56 | 1.19.3 | удалить; текстурирование задаётся выбором pipeline |
| `Tessellator.getInstance().getBuffer()` | 30 | 1.20.1 | `Tessellator.getInstance().begin(DrawMode, VertexFormat)` |
| `bufferBuilder.begin(...)` | 44 | 1.20.1 | см. выше — begin теперь на Tessellator |
| `.next()` после vertex | **311** | 1.20.5 | удалить, вершина коммитится сама |
| `Tessellator.getInstance().draw()` | 42 | 1.20.1 | `BufferRenderer.drawWithGlobalProgram(builder.end())` |
| `RenderSystem.setShader(...)` | 47 | **1.21.5** | `RenderPipeline` / `RenderPipelines.*` |
| `GameRenderer::getPositionColorShader` и др. | 47 | **1.21.5** | удалены полностью |

**Это ядро твоего `RenderUtils` (635 строк), `RenderUtils3D` (486),
`CrosshairRenderUtils`, `CustomFont` (349).** В 1.21.5 Mojang выкинул
старый shader-путь `RenderSystem.setShader` и заменил его на
декларативные `RenderPipeline`. Прямой замены строка-в-строку нет:
это переписывание, а не замена импортов.

Типичный переход:

```java
// было (1.18.2)
RenderSystem.setShader(GameRenderer::getPositionColorShader);
BufferBuilder bb = Tessellator.getInstance().getBuffer();
bb.begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
bb.vertex(matrix, x, y2, 0).color(r,g,b,a).next();
...
Tessellator.getInstance().draw();

// стало (1.21.11)
BufferBuilder bb = Tessellator.getInstance()
        .begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
bb.vertex(matrix, x, y2, 0).color(r,g,b,a);
...
BufferRenderer.drawWithGlobalProgram(bb.end());
// либо, что правильнее для GUI, — через context.draw(...) с RenderLayer
```

### 1.2 GUI-контекст — сигнатуры всех экранов

| Твой код | Умерло в | Замена |
|---|---|---|
| `render(MatrixStack, int, int, float)` | 1.20 | `render(DrawContext, int, int, float)` |
| `drawTexture(MatrixStack, ...)` | 1.20 | `context.drawTexture(RenderLayer::getGuiTextured, id, ...)` |
| `textRenderer.drawWithShadow(matrices, ...)` | 1.20 / 1.21.5 | `context.drawTextWithShadow(textRenderer, ...)` |
| `fill(MatrixStack, ...)` | 1.20 | `context.fill(...)` |

Затрагивает: `ReallyVisualsScreen` (2461 строка), `CrosshairEditorScreen`,
`CustomHandEditorScreen`, `CrosshairPresetsDialog`, `ReportConfirmationScreen`,
`HudEditor`, `HUDManager`.

### 1.3 Математика → JOML

16 вхождений в 12 файлах.

```java
net.minecraft.util.math.Matrix4f  →  org.joml.Matrix4f
net.minecraft.util.math.Vec3f     →  org.joml.Vector3f
net.minecraft.util.math.Quaternion→  org.joml.Quaternionf
```

Повороты (34 вхождения):
```java
// было
matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(angle));
// стало
matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));
```

### 1.4 Identifier

17 вхождений `new Identifier(...)` — конструктор закрыт в 1.21.

```java
new Identifier("really", "textures/x.png")
→ Identifier.of("really", "textures/x.png")
```

### 1.5 Text

30 вхождений `LiteralText` / `TranslatableText` — удалены в 1.19.

```java
new LiteralText("x")       → Text.literal("x")
new TranslatableText("k")  → Text.translatable("k")
```

### 1.6 tickDelta → RenderTickCounter

48 вхождений в 17 файлах. С 1.21.2 в render-методах вместо `float tickDelta`
приходит `RenderTickCounter`. Значение берётся как
`counter.getTickDelta(false)` (или `getDynamicDeltaTicks()` — зависит от
контекста: для рендера мира нужен «сырой», для GUI — сглаженный).

### 1.7 ItemStack NBT → компоненты

4 вхождения. `stack.getNbt()` больше нет (1.20.5).

```java
stack.getNbt().getInt("x")
→ stack.get(DataComponentTypes.CUSTOM_DATA)  // и далее по типу компонента
```

Твой `ShulkerPreview` читает содержимое шалкера — там теперь
`DataComponentTypes.CONTAINER` (`ContainerComponent`).

---

## 2. Миксины — самый тяжёлый блок

**86 точек инъекции.** Проблема не в аннотациях, а в том, что
все дескрипторы указывают на сигнатуры 1.18.2, а они изменились все.

Найденные в твоём коде хардкод-дескрипторы (**ни один не валиден в 1.21.11**):

```
TextRenderer;drawWithShadow(Lnet/minecraft/client/util/math/MatrixStack;...)
InGameHud;drawTexture(Lnet/minecraft/client/util/math/MatrixStack;IIIIII)V
Matrix4f;viewboxMatrix(DFFF)Lnet/minecraft/util/math/Matrix4f;
EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;...)
ChunkBuilder$BuiltChunk;getOrigin()Lnet/minecraft/util/math/BlockPos;
MatrixStack;translate(DDD)V                     ← стало translate(FFF)
LivingEntityRenderer;scale(...MatrixStack;F)V   ← EntityRenderState
PlayerEntityRenderer;render(...MatrixStack;...) ← PlayerEntityRenderState
```

Отдельно критично:

- **`InGameHudMixin` (220 строк)** — в 1.21.9 `InGameHud` разобрали на
  слои (layered HUD components). Методы `renderHotbar`, `renderCrosshair`,
  `renderScoreboardSidebar` живут теперь иначе. Часть твоих инъекций
  проще заменить на Fabric API `HudElementRegistry` вместо миксинов.
- **`WorldRendererMixin` (262 строки)** — `WorldRenderer` переписан
  дважды (1.21.2 и 1.21.5). Для ESP/трейсеров правильнее уйти на
  **World Render Events**, которые Fabric вернул в API как раз в 1.21.10.
- **`LivingEntityRendererMixin` / `PlayerEntityRendererMixin` /
  `FeatureRendererMixin` / `ArmorFeatureRendererMixin`** — с 1.21.2
  рендер сущностей идёт через `EntityRenderState`: у рендерера появился
  второй generic-параметр, а сама сущность в render() **больше не
  передаётся**. Любой код, который внутри рендера смотрит на
  `LivingEntity`, надо переносить в `updateRenderState()`.

### 2.1 Sodium-миксины — их надо выбросить

`mixin/sodium/ChunkRenderManagerMixin`, `ChunkGraphicsStateMixin`,
`ChunkCameraContextMixin` целятся во внутренности Sodium 0.4.x (эпоха 1.18.2).
В современном Sodium этих классов **не существует** — там
`RenderSectionManager` с другой архитектурой. Плюс `SodiumChunkAnimator`,
`SodiumChunkOffset`, `VanillaChunksMixinPlugin`.

`ChunkAnimator` придётся либо переписать под актуальный Sodium с нуля,
либо оставить только ванильную ветку.

### 2.2 Конфиг миксинов

```jsonc
"compatibilityLevel": "JAVA_17"  →  "JAVA_21"
"refmap": "reallyvisuals-refmap.json"  →  убрать (Loom 1.14 ремапит сам)
"injectors": { "defaultRequire": 0 }   →  подними до 1 после порта
```

`defaultRequire: 0` — это причина, по которой твой мод «собирается,
но половина не работает»: миксины молча не применяются. На время
порта пусть остаётся 0, но в конце обязательно 1.

---

## 3. Fabric API

| Твой вызов | Статус | Замена |
|---|---|---|
| `ModelLoadingRegistry.INSTANCE.registerModelProvider` | удалён (1.20.5) | `ModelLoadingPlugin` (`fabric-model-loading-api-v1`) |
| `ClientTickEvents.START_CLIENT_TICK` | жив | без изменений |
| `KeyBindingHelper.registerKeyBinding` | жив | без изменений |

Хорошая новость: `ConfigManager` (355 строк), `ModuleManager`, `Module`,
`Category`, система биндов, `FriendManager`, `WaypointManager` —
чистая Java без API Minecraft. **Они переносятся почти как есть.**
Это примерно 30–35 % кодовой базы.

---

## 4. Шейдеры

`assets/really/shaders/fragment/*.fsh` (16 файлов) грузятся через
старый `ShaderProgram`-путь, который убран в 1.21.5. Под новую
систему их надо регистрировать как `RenderPipeline` со snippet'ами.
GLSL-код в основном переживёт, обвязка — нет.

---

## 5. Порядок работ

Портировать всё разом нельзя — 86 миксинов упадут лавиной
нечитаемых ошибок. Рабочая последовательность:

1. **Скелет.** Положить `config-1.21.11/*`, вынести весь `src` в сторону,
   оставить только `ReallyVisualsMod` + пустой mixins.json.
   Добиться `BUILD SUCCESSFUL` на пустышке — это проверит toolchain.
2. **Чистый слой.** Вернуть `config/`, `module/` (логика), `utils/`
   без рендера. Здесь правок почти нет.
3. **RenderUtils.** Переписать под `RenderPipeline` — это фундамент,
   от него зависит весь GUI. Пока не заработает, дальше нет смысла.
4. **GUI.** `ReallyVisualsScreen` и редакторы на `DrawContext`.
5. **HUD-модули.** По одному.
6. **Миксины.** По одному, с `defaultRequire: 1`, проверяя каждый
   запуском. Здесь без открытого маппленного jar'а никак —
   дескрипторы надо смотреть глазами.
7. **Sodium / ChunkAnimator.** В самом конце, отдельно.

---

## 6. Оценка

| Блок | Объём | Сложность |
|---|---|---|
| Конфиг Gradle/Fabric | готово | — |
| Логика (config, module, utils) | ~5 000 строк | низкая, механическая |
| RenderUtils / RenderUtils3D / CustomFont | ~1 500 строк | **переписывание** |
| GUI-экраны | ~3 700 строк | высокая, механическая |
| HUD-модули | ~3 000 строк | средняя |
| 86 миксинов | ~2 500 строк | **очень высокая** |
| Sodium-слой | ~300 строк | переписывание с нуля |
