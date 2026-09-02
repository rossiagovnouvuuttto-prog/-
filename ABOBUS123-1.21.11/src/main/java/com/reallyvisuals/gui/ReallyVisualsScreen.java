package com.reallyvisuals.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reallyvisuals.audio.UISoundHelper;
import com.reallyvisuals.config.ConfigManager;
import com.reallyvisuals.gui.font.CustomFont;
import com.reallyvisuals.gui.font.FontManager;
import com.reallyvisuals.gui.render.RenderUtils;
import com.reallyvisuals.module.AutoMarkers;
import com.reallyvisuals.module.Category;
import com.reallyvisuals.module.CreateMarker;
import com.reallyvisuals.module.Crosshair;
import com.reallyvisuals.module.CustomHand;
import com.reallyvisuals.module.DeathPosition;
import com.reallyvisuals.module.FriendManager;
import com.reallyvisuals.module.MarkerSettings;
import com.reallyvisuals.module.Module;
import com.reallyvisuals.module.ModuleManager;
import com.reallyvisuals.module.WaypointManager;
import com.reallyvisuals.utils.AnimationUtils;
import com.reallyvisuals.utils.KeyUtils;
import java.awt.Color;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class ReallyVisualsScreen extends Screen {
   private static final Identifier LOGO_TEXTURE = Identifier.of("really", "textures/logo/rv_colored.png");
   private boolean markersSettingsExpanded = true;
   private boolean autoMarkersExpanded = false;
   private boolean createMarkerExpanded = false;
   private String newMarkerName = "Быстрая метка";
   private int newMarkerColorIndex = 0;
   private String newMarkerX = "89";
   private String newMarkerY = "75";
   private String newMarkerZ = "-140";
   private int newMarkerIconIndex = 0;
   private int activeMarkerInput = 0;
   private boolean autoEventsDropdownOpen = false;
   private static final int[] MARKER_COLORS = new int[]{-34019, -13382401, -13369481, -5622785, -52395, -13312};
   private static final String[] MARKER_COLOR_HEXES = new String[]{"#FF7B1D", "#33CCFF", "#33FF77", "#AA33FF", "#FF3355", "#FFCC00"};
   private static final Identifier AVATAR_TEXTURE = Identifier.of("really", "textures/logo/avatar.png");
   public static boolean soundEnabled = true;
   public static boolean moduleSoundsEnabled = true;
   public static int accentColor = -34019;
   public static int clientColor = -1016538;
   public boolean pickerOpen = false;
   public String activeColorPicker = "";
   private Module colorEditModule = null;
   public int pickerDragging = 0;
   public float pickerHue = 0.08F;
   public float pickerSat = 0.9F;
   public float pickerVal = 0.95F;
   public static float uiScale = 1.0F;
   public static float soundVolume = 70.0F;
   public static String menuKeyName = "RSHFT";
   public static int menuKeyCode = 344;
   public static Category lastCategory = Category.VISUALS;
   public static float lastScrollY = 0.0F;
   public static boolean lastShowingClientSettings = false;
   private Category selectedCategory = lastCategory;
   private String searchQuery = "";
   private Module bindingModule = null;
   private boolean showingClientSettings = lastShowingClientSettings;
   private boolean bindingMenuKey = false;
   private boolean sidebarCollapsed = false;
   private boolean showingDragmode = false;
   private String friendInputQuery = "";
   private boolean friendInputFocused = false;
   private HUDManager.HUDElement draggingHUDElement = null;
   private float dragOffsetX = 0.0F;
   private float dragOffsetY = 0.0F;
   private HUDManager.HUDElement activeMenuHUDElement = null;
   private Module.NumberSetting draggingNumberSetting = null;
   private int draggingModuleLeft = 0;
   private int draggingCardWidth = 0;
    private int draggingClientSlider = 0;
    private int clientSliderX = 0;
    private int clientSliderW = 0;
    private float clientSliderStartScale = 1.0F;
    private float clientSliderDragRatio = -1.0F;
    private int draggingMarkerSlider = 0;
    private int markerSliderDragX = 0;
    private int markerSliderDragW = 0;
    private AnimationUtils.Animation openAnim = new AnimationUtils.Animation(0.75F);
    private AnimationUtils.Animation openYAnim = new AnimationUtils.Animation(0.0F);
   private AnimationUtils.Animation navYAnimation = null;
   private AnimationUtils.Animation contentSlideAnim = new AnimationUtils.Animation(0.0F);
   private AnimationUtils.Animation sidebarAnim = new AnimationUtils.Animation(120.0F);
   private AnimationUtils.Animation scrollAnimation = new AnimationUtils.Animation(lastScrollY);
   private float targetScroll = lastScrollY;
   private float maxScroll = 0.0F;
   private final int windowWidth = 484;
   private final int windowHeight = 280;
   private int guiLeft;
   private int guiTop;

   public ReallyVisualsScreen() {
      super(Text.literal("ABOBUS123 GUI"));
   }

   private static String fitText(CustomFont font, String text, int maxWidth) {
      if (text == null || maxWidth <= 0) {
         return "";
      } else if (font.getStringWidth(text) <= maxWidth) {
         return text;
      } else {
         String cut = text;

         while (cut.length() > 1 && font.getStringWidth(cut) > maxWidth) {
            cut = cut.substring(0, cut.length() - 1);
         }

         return cut;
      }
   }

   private int getSettingsHeight(Module module) {
      if (!module.isExpanded()) {
         return 0;
      }

      if (module instanceof Crosshair) {
         return 30;
      }

      if (module instanceof CustomHand) {
         return 30;
      }

      int h = 0;

      for (Module.Setting s : module.getSettings()) {
         if (s.isVisible()) {
            if (s instanceof Module.BooleanSetting) {
               Module.BooleanSetting bool = (Module.BooleanSetting)s;
               h += 18;
               if ("Цвет клиента".equalsIgnoreCase(bool.name) && !bool.value) {
                  h += 18;
               }
            } else if (s instanceof Module.NumberSetting) {
               h += 24;
            } else if (s instanceof Module.ModeSetting) {
               Module.ModeSetting modeSetting = (Module.ModeSetting)s;
               h += 30;
               if (modeSetting.open) {
                  h += modeSetting.modes.length * 20 + 8;
               }
            } else if (s instanceof Module.MultiSelectSetting) {
               Module.Setting m = (Module.MultiSelectSetting)s;
               h += 30;
               if (((Module.MultiSelectSetting)m).open) {
                  h += ((Module.MultiSelectSetting)m).options.length * 20 + 8;
               }
            } else if (s instanceof Module.TextSetting) {
               h += 30;
            } else if (s instanceof Module.KeySetting) {
               h += 18;
            }
         }
      }

      return h > 0 ? h + 6 : 0;
   }

   protected void init() {
      super.init();
      this.guiLeft = (this.width - 484) / 2;
      this.guiTop = (this.height - 280) / 2;
      this.selectedCategory = lastCategory;
      this.showingClientSettings = lastShowingClientSettings;
      this.targetScroll = lastScrollY;
      this.scrollAnimation = new AnimationUtils.Animation(lastScrollY);
      FontManager.init();
      this.openAnim.force(0.85F);
      this.openAnim.setEasing(1);
      this.openAnim.animateTo(1.0F, 400L);
      this.openYAnim.force(24.0F);
      this.openYAnim.setEasing(1);
      this.openYAnim.animateTo(0.0F, 400L);
      UISoundHelper.playSound("ui.gui_open");
   }

   public void removed() {
      super.removed();
      lastCategory = this.selectedCategory;
      lastScrollY = this.targetScroll;
      lastShowingClientSettings = this.showingClientSettings;
      long windowHandle = MinecraftClient.getInstance().getWindow().getHandle();
      try {
         GLFW.glfwSetCursor(windowHandle, 0L);
      } catch (Throwable ignored) {
      }
      UISoundHelper.playSound("ui.gui_close");
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double amount) {
      if (this.showingDragmode) {
         return false;
      } else if (this.maxScroll > 0.0F) {
         this.targetScroll -= (float)(amount * 24.0);
         this.targetScroll = Math.max(0.0F, Math.min(this.maxScroll, this.targetScroll));
         this.scrollAnimation.animateTo(this.targetScroll, 100L);
         UISoundHelper.playSound("ui.slider_tick", 1.1F, 0.4F);
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, horizontal, amount);
      }
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      // 1.21.11 allows exactly one blur per frame and vanilla already spends it
      // before render() is called; renderBackground() would blur a second time and
      // throw "Can only blur once per frame". renderInGameBackground is the
      // darkening half without the blur.
      this.renderInGameBackground(context);
      CustomFont mainFont = FontManager.getMainFont();
      CustomFont titleFont = FontManager.getTitleFont();
      CustomFont subFont = FontManager.getSubFont();
      CustomFont iconFont = FontManager.getIconFont();
      if (this.showingDragmode) {
         this.renderDragmodeOverlay(context, mouseX, mouseY, mainFont, titleFont, subFont, iconFont);
      } else {
         float scaleVal = this.openAnim.getValue();
         float slideVal = this.openYAnim.getValue();
         context.getMatrices().pushMatrix();
         context.getMatrices().translate((float) (0.0F), (float) (slideVal));
         context.getMatrices().pushMatrix();
         if (scaleVal < 0.999F) {
            float cx = this.width / 2.0F;
            float cy = this.height / 2.0F;
            context.getMatrices().translate((float) (cx), (float) (cy));
            context.getMatrices().scale((float) (scaleVal), (float) (scaleVal));
            context.getMatrices().translate((float) (-cx), (float) (-cy));
         }

         this.sidebarAnim.animateTo(this.sidebarCollapsed ? 42.0F : 93.0F, 180L);
         int sidebarWidth = (int)this.sidebarAnim.getValue();
         float renderScale = this.draggingClientSlider == 1 ? this.clientSliderStartScale : uiScale;
         context.getMatrices().pushMatrix();
         context.getMatrices().translate((float) (this.guiLeft + 242.0F), (float) (this.guiTop + 140.0F));
         context.getMatrices().scale((float) (renderScale), (float) (renderScale));
         context.getMatrices().translate((float) (-(this.guiLeft + 242.0F)), (float) (-(this.guiTop + 140.0F)));
         RenderUtils.drawRoundedRect(context, this.guiLeft, this.guiTop, 484.0F, 280.0F, 12.0F, -15461353);
         RenderUtils.drawRoundedRect(context, this.guiLeft, this.guiTop, sidebarWidth, 280.0F, 12.0F, -15395561);
         if (sidebarWidth > 12) {
            RenderUtils.drawRect(context, this.guiLeft + sidebarWidth - 12, this.guiTop, 12.0F, 280.0F, -15395561);
         }

         RenderUtils.drawRect(context, this.guiLeft + sidebarWidth, this.guiTop, 1.0F, 280.0F, -14079697);

         try {
            RenderUtils.drawTexture(context, LOGO_TEXTURE, this.guiLeft + 7, this.guiTop + 13, 18.0F, 10.0F);
         } catch (Exception e) {
            RenderUtils.drawRoundedRect(context, this.guiLeft + 7, this.guiTop + 13, 12.0F, 10.0F, 3.0F, accentColor);
         }

         if (!this.sidebarCollapsed && sidebarWidth > 55) {
            subFont.drawString(context, "ABOBUS123", this.guiLeft + 29, this.guiTop + 13.5F, -1);
         }

         float targetPillY = this.guiTop + 38 + this.selectedCategory.ordinal() * 26;
         if (this.navYAnimation == null) {
            this.navYAnimation = new AnimationUtils.Animation(targetPillY);
         } else {
            this.navYAnimation.animateTo(targetPillY, 160L);
         }

         float animPillY = this.navYAnimation.getValue();
         float itemX = this.guiLeft + 7;
         float itemW = sidebarWidth - 14;
         float itemH = 18.0F;
         float pillRadius = itemH / 2.0F;
         if (!this.showingClientSettings) {
            RenderUtils.drawGlow(context, itemX, animPillY, itemW, itemH, pillRadius, accentColor, 3, 0.12F);
            RenderUtils.drawRoundedRect(context, itemX, animPillY, itemW, itemH, pillRadius, accentColor);
         }

         int navY = this.guiTop + 38;
         Category[] cats = Category.values();

         for (int i = 0; i < cats.length; i++) {
            Category category = cats[i];
            boolean active = !this.showingClientSettings && category == this.selectedCategory;
            boolean navHovered = !active && mouseX >= itemX && mouseX <= itemX + itemW && mouseY >= navY && mouseY <= navY + itemH;
            if (navHovered) {
               RenderUtils.drawRoundedRect(context, itemX, navY, itemW, itemH, pillRadius, -15198177);
            }

            int textColor = active ? -1 : (navHovered ? -2039579 : -3618611);
            float iconYExtra = category == Category.VISUALS ? 0.5F : 0.0F;
            context.getMatrices().pushMatrix();
            float catScale = 0.83F;
            context.getMatrices().scale((float) (catScale), (float) (catScale));
            float invS = 1.0F / catScale;
            if (this.sidebarCollapsed) {
               iconFont.drawString(context, category.getIconGlyph(), (this.guiLeft + 15.0F) * invS, (navY + 3.5F + iconYExtra) * invS, textColor);
            } else if (active) {
               subFont.drawString(context, "::", (this.guiLeft + 9.0F) * invS, (navY + 4.5F) * invS, -1);
               iconFont.drawString(context, category.getIconGlyph(), (this.guiLeft + 18.0F) * invS, (navY + 3.5F + iconYExtra) * invS, -1);
               subFont.drawString(context, category.getName(), (this.guiLeft + 32.0F) * invS, (navY + 4.5F) * invS, -1);
            } else if (navHovered) {
               subFont.drawString(context, "::", (this.guiLeft + 9.0F) * invS, (navY + 4.5F) * invS, -8750459);
               iconFont.drawString(context, category.getIconGlyph(), (this.guiLeft + 18.0F) * invS, (navY + 3.5F + iconYExtra) * invS, textColor);
               subFont.drawString(context, category.getName(), (this.guiLeft + 32.0F) * invS, (navY + 4.5F) * invS, textColor);
            } else {
               iconFont.drawString(context, category.getIconGlyph(), (this.guiLeft + 13.0F) * invS, (navY + 3.5F + iconYExtra) * invS, textColor);
               subFont.drawString(context, category.getName(), (this.guiLeft + 27.0F) * invS, (navY + 4.5F) * invS, textColor);
            }

            context.getMatrices().popMatrix();
            if (i < cats.length - 1 && !this.sidebarCollapsed) {
               int divW = 20;
               int divX = this.guiLeft + (sidebarWidth - divW) / 2;
               RenderUtils.drawRect(context, divX, navY + 22.0F, divW, 1.0F, -14935006);
            }

            navY += 26;
         }

         int profileY = this.guiTop + 280 - 34;

         try {
            RenderUtils.drawTexture(context, AVATAR_TEXTURE, this.guiLeft + 10, profileY + 4, 11.5F, 7.5F);
         } catch (Exception e) {
            RenderUtils.drawRoundedRect(context, this.guiLeft + 10, profileY + 4, 11.5F, 7.5F, 3.0F, accentColor);
         }

         if (!this.sidebarCollapsed && sidebarWidth > 70) {
            mainFont.drawString(context, "@soezproject", this.guiLeft + 26, profileY + 1, -1);
            subFont.drawString(context, "User", this.guiLeft + 26, profileY + 11, -8750459);
            int hamX = this.guiLeft + sidebarWidth - 16;
            int hamY = profileY + 7;
            RenderUtils.drawHamburgerIcon(context, hamX, hamY, 8.5F, -11250595);
         } else {
            int hamX = this.guiLeft + 21;
            int hamY = profileY + 18;
            RenderUtils.drawHamburgerIcon(context, hamX, hamY, 8.5F, -11250595);
         }

         int contentLeft = this.guiLeft + sidebarWidth + 18;
         String headerTitleText = this.showingClientSettings ? "Настройки клиента" : this.selectedCategory.getName();
         mainFont.drawString(context, headerTitleText, contentLeft, this.guiTop + 17, -1);
         if (!this.showingClientSettings && this.selectedCategory == Category.HUD) {
            int hudTitleW = mainFont.getStringWidth("HUD");
            int dragBtnX = contentLeft + hudTitleW + 8;
            int dragBtnY = this.guiTop + 15;
            boolean dragBtnHovered = mouseX >= dragBtnX && mouseX <= dragBtnX + 18 && mouseY >= dragBtnY && mouseY <= dragBtnY + 18;
            RenderUtils.drawRoundedRect(context, dragBtnX, dragBtnY, 18.0F, 18.0F, 5.0F, dragBtnHovered ? -14408660 : -15198179);
            iconFont.drawString(context, "\ue90b", dragBtnX + 3, dragBtnY + 3, dragBtnHovered ? accentColor : -7434605);
            if (dragBtnHovered) {
               RenderUtils.drawRoundedRect(context, dragBtnX + 22, dragBtnY + 1, 62.0F, 16.0F, 5.0F, -15198179);
               subFont.drawString(context, "Dragmode", dragBtnX + 26, dragBtnY + 4, -1);
            }
         }

         int gearX = this.guiLeft + 484 - 20;
         int gearY = this.guiTop + 14;
         RenderUtils.drawGearIcon(context, gearX + 6, gearY + 8, 3.2F, accentColor);
         int searchW = 178;
         int searchH = 16;
         int searchX = gearX - searchW - 8;
         int searchY = this.guiTop + 14;
         RenderUtils.drawRoundedRect(context, searchX - 1.5F, searchY - 1.5F, searchW + 3.0F, searchH + 3.0F, 9.5F, -15329767);
         RenderUtils.drawRoundedRect(context, searchX - 0.75F, searchY - 0.75F, searchW + 1.5F, searchH + 1.5F, 8.8F, -15000802);
         RenderUtils.drawRoundedRect(context, searchX, searchY, searchW, searchH, 8.0F, -15461353);
         String searchDisplayText = this.searchQuery.isEmpty() ? "Поиск" : this.searchQuery;
         int searchTextColor = this.searchQuery.isEmpty() ? -10132112 : -1;
         subFont.drawString(context, searchDisplayText, searchX + 8, searchY + 1.5F, searchTextColor);
         iconFont.drawString(context, "\ue921", searchX + searchW - 15, searchY + 2.0F, -8750459);
         int areaLeft = this.guiLeft + sidebarWidth;
         int contentWidth = 484 - sidebarWidth;
         RenderUtils.drawRect(context, areaLeft, this.guiTop + 45, contentWidth, 1.0F, -14079697);
         int viewportH = 219;
         int startY = this.guiTop + 60;
         double scale = MinecraftClient.getInstance().getWindow().getScaleFactor();
         float pivotX = this.guiLeft + 242.0F;
         float pivotY = this.guiTop + 140.0F;
         float sx1 = pivotX + (areaLeft + 1 - pivotX) * renderScale;
         float sx2 = pivotX + (areaLeft + contentWidth - 1 - pivotX) * renderScale;
         float sy1 = pivotY + (this.guiTop + 55.0F - pivotY) * renderScale;
         float sy2 = pivotY + (this.guiTop + 278.0F - pivotY) * renderScale;
         int scissorX = (int)(sx1 * scale);
         int scissorY = (int)(MinecraftClient.getInstance().getWindow().getHeight() - sy2 * scale);
         int scissorW = (int)((sx2 - sx1) * scale);
         int scissorH = (int)((sy2 - sy1) * scale);
         context.enableScissor(scissorX, scissorY, scissorW, scissorH);
         float currentScrollY = this.scrollAnimation.getValue();
         if (this.showingClientSettings) {
            this.maxScroll = 120.0F;
            context.getMatrices().pushMatrix();
            context.getMatrices().translate((float) (0.0), (float) (-currentScrollY));
            int cardX = areaLeft + 14;
            int cardW = contentWidth - 28;
            int cardY = startY;
            int interfaceH = 100;
            RenderUtils.drawRoundedRect(context, cardX, cardY, cardW, interfaceH, 8.0F, -15461351);
            iconFont.drawString(context, "\ue929", cardX + 12, cardY + 12, accentColor);
            mainFont.drawString(context, "Interface", cardX + 28, cardY + 10, -1);
            subFont.drawString(context, "Всё, что влияет на внешний вид GUI и HUD", cardX + 12, cardY + 24, -8750459);
            RenderUtils.drawRect(context, cardX + 10, cardY + 38, cardW - 20, 1.0F, -14935006);
            subFont.drawString(context, "Масштаб UI", cardX + 12, cardY + 46, -1);
            String uiScaleStr = String.format("%.1f", uiScale).replace(".", ",");
            subFont.drawString(context, uiScaleStr, cardX + cardW - 12 - subFont.getStringWidth(uiScaleStr), cardY + 46, accentColor);
            int uiSliderX = cardX + 12;
            int uiSliderW = cardW - 24;
            RenderUtils.drawRoundedRect(context, uiSliderX, cardY + 58, uiSliderW, 3.0F, 1.5F, -14935006);
            float uiFill = this.draggingClientSlider == 1 && this.clientSliderDragRatio >= 0.0F ? this.clientSliderDragRatio : (uiScale - 0.5F) / 1.5F;
            RenderUtils.drawRoundedRect(context, uiSliderX, cardY + 58, (int)(uiSliderW * uiFill), 3.0F, 1.5F, accentColor);
            RenderUtils.drawRoundedRect(context, uiSliderX + uiSliderW * uiFill - 3.0F, cardY + 56.5F, 6.0F, 6.0F, 3.0F, -1);
            String accentHex = String.format("#%06X", accentColor & 16777215);
            subFont.drawString(context, "Акцентный цвет", cardX + 12, cardY + 68, -1);
            RenderUtils.drawCircle(context, cardX + cardW - 75, cardY + 71, 4.5F, accentColor);
            subFont.drawString(context, accentHex, cardX + cardW - 66, cardY + 68, -1);
            String clientHex = String.format("#%06X", clientColor & 16777215);
            subFont.drawString(context, "Цвет клиента", cardX + 12, cardY + 84, -1);
            RenderUtils.drawCircle(context, cardX + cardW - 75, cardY + 87, 4.5F, clientColor);
            subFont.drawString(context, clientHex, cardX + cardW - 66, cardY + 84, -1);
            int controlsH = 70;
            int var119;
            RenderUtils.drawRoundedRect(context, cardX, var119 = cardY + interfaceH + 10, cardW, controlsH, 8.0F, -15461351);
            iconFont.drawString(context, "\ue910", cardX + 12, var119 + 12, accentColor);
            mainFont.drawString(context, "Controls", cardX + 28, var119 + 10, -1);
            subFont.drawString(context, "Управление и поведение интерфейса", cardX + 12, var119 + 24, -8750459);
            RenderUtils.drawRect(context, cardX + 10, var119 + 38, cardW - 20, 1.0F, -14935006);
            subFont.drawString(context, "Клавиша меню", cardX + 12, var119 + 48, -1);
            String keyStr = this.bindingMenuKey ? "..." : menuKeyName;
            int keyStrBoxW = 46;
            keyStr = fitText(subFont, keyStr, keyStrBoxW - 4);
            int keyStrBoxX = cardX + cardW - 12 - keyStrBoxW;
            subFont.drawString(
               context,
               keyStr,
               keyStrBoxX + (keyStrBoxW - subFont.getStringWidth(keyStr)) / 2.0F,
               var119 + 48,
               this.bindingMenuKey ? accentColor : -1
            );
            int audioH = 95;
            RenderUtils.drawRoundedRect(context, cardX, cardY = var119 + controlsH + 10, cardW, audioH, 8.0F, -15461351);
            iconFont.drawString(context, "\ue90b", cardX + 12, cardY + 12, accentColor);
            mainFont.drawString(context, "Audio", cardX + 28, cardY + 10, -1);
            subFont.drawString(context, "Звуки клиента и громкость", cardX + 12, cardY + 24, -8750459);
            RenderUtils.drawRect(context, cardX + 10, cardY + 38, cardW - 20, 1.0F, -14935006);
            subFont.drawString(context, "Звуки", cardX + 12, cardY + 46, -1);
            int sndBoxX = cardX + cardW - 22;
            int sndBoxY = cardY + 44;
            if (soundEnabled) {
               RenderUtils.drawRoundedRect(context, sndBoxX, sndBoxY, 10.0F, 10.0F, 3.0F, accentColor);
               RenderUtils.drawCheckmark(context, sndBoxX, sndBoxY, 10.0F, -1);
            } else {
               RenderUtils.drawRoundedRect(context, sndBoxX, sndBoxY, 10.0F, 10.0F, 3.0F, -14935006);
            }

            subFont.drawString(context, "Громкость", cardX + 12, cardY + 58, -1);
            String volStr = String.format("%.1f", soundVolume).replace(".", ",");
            subFont.drawString(context, volStr, cardX + cardW - 12 - subFont.getStringWidth(volStr), cardY + 58, accentColor);
            int volSliderX = cardX + 12;
            int volSliderW = cardW - 24;
            RenderUtils.drawRoundedRect(context, volSliderX, cardY + 70, volSliderW, 3.0F, 1.5F, -14935006);
            float volFill = this.draggingClientSlider == 2 && this.clientSliderDragRatio >= 0.0F ? this.clientSliderDragRatio : soundVolume / 100.0F;
            RenderUtils.drawRoundedRect(context, volSliderX, cardY + 70, (int)(volSliderW * volFill), 3.0F, 1.5F, accentColor);
            RenderUtils.drawRoundedRect(context, volSliderX + volSliderW * volFill - 3.0F, cardY + 68.5F, 6.0F, 6.0F, 3.0F, -1);
            subFont.drawString(context, "Звук вкл/выкл модулей", cardX + 12, cardY + 78, -1);
            int modSndBoxX = cardX + cardW - 22;
            int modSndBoxY = cardY + 76;
            if (moduleSoundsEnabled) {
               RenderUtils.drawRoundedRect(context, modSndBoxX, modSndBoxY, 10.0F, 10.0F, 3.0F, accentColor);
               RenderUtils.drawCheckmark(context, modSndBoxX, modSndBoxY, 10.0F, -1);
            } else {
               RenderUtils.drawRoundedRect(context, modSndBoxX, modSndBoxY, 10.0F, 10.0F, 3.0F, -14935006);
            }

            context.getMatrices().popMatrix();
         } else if (this.selectedCategory == Category.FRIENDS) {
            this.maxScroll = 120.0F;
            double scrolledMouseY = mouseY + currentScrollY;
            context.getMatrices().pushMatrix();
            context.getMatrices().translate((float) (0.0), (float) (-currentScrollY));
            int cardX = areaLeft + 14;
            int cardW = contentWidth - 28;
            int cardY = startY;
            Module friendSysMod = ModuleManager.getInstance().getModule("Настройки друзей");
            int friendSysSettingsH = friendSysMod != null && friendSysMod.isExpanded() ? this.getSettingsHeight(friendSysMod) : 0;
            int settingsCardH = 38 + friendSysSettingsH;
            RenderUtils.drawRoundedRect(context, cardX, cardY, cardW, settingsCardH, 8.0F, -15461351);
            iconFont.drawString(context, "\ue929", cardX + 12, cardY + 12, accentColor);
            mainFont.drawString(context, "Настройки друзей", cardX + 28, cardY + 10, -1);
            subFont.drawString(context, "Поведение клиента по отношению к друзей", cardX + 12, cardY + 24, -8750459);
            context.getMatrices().pushMatrix();
            float arrowScaleY = friendSysMod != null && friendSysMod.isExpanded() ? -1.0F : 1.0F;
            context.getMatrices().translate((float) (cardX + cardW - 18), (float) (cardY + 19));
            context.getMatrices().scale((float) (1.0F), (float) (arrowScaleY));
            iconFont.drawString(context, "\ue90d", -4.0F, -4.0F, -10132112);
            context.getMatrices().popMatrix();
            if (friendSysMod != null && friendSysMod.isExpanded()) {
               RenderUtils.drawRect(context, cardX + 10, cardY + 37, cardW - 20, 1.0F, -14540246);
               int settingY = cardY + 42;

               for (Module.Setting setting : friendSysMod.getSettings()) {
                  if (setting.isVisible()) {
                     if (setting instanceof Module.BooleanSetting) {
                        Module.BooleanSetting bool = (Module.BooleanSetting)setting;
                        subFont.drawString(context, bool.name, cardX + 12, settingY + 2, -1);
                        int boxSize = 11;
                        int boxX = cardX + cardW - 12 - boxSize;
                        int boxY = settingY + 1;
                        if (bool.value) {
                                  RenderUtils.drawRoundedRect(context, boxX, boxY, boxSize, boxSize, 3.0F, accentColor);
                                  RenderUtils.drawCheckmark(context, boxX, boxY, boxSize, -1);
                        } else {
                           RenderUtils.drawRoundedRect(context, boxX, boxY, boxSize, boxSize, 3.0F, -14935006);
                        }

                        settingY += 18;
                        if ("Цвет клиента".equalsIgnoreCase(bool.name) && !bool.value) {
                           subFont.drawString(context, "Кастомный цвет", cardX + 12, settingY + 2, -1);
                           int dotX = cardX + cardW - 75;
                           RenderUtils.drawCircle(context, dotX, settingY + 6, 4.5F, friendSysMod != null ? friendSysMod.customColor : -1);
                           String customHex = String.format("#%06X", (friendSysMod != null ? friendSysMod.customColor : -1) & 16777215);
                           subFont.drawString(context, customHex, dotX + 8, settingY + 2, -1);
                           settingY += 18;
                        }
                     } else if (setting instanceof Module.MultiSelectSetting) {
                        Module.MultiSelectSetting multi = (Module.MultiSelectSetting)setting;
                        subFont.drawString(context, multi.name, cardX + 12, settingY, -1);
                        RenderUtils.drawRoundedRect(context, cardX + 12, settingY + 11, cardW - 24, 16.0F, 5.0F, -15198179);
                        iconFont.drawString(context, "\ue910", cardX + 17, settingY + 15, -10132112);
                        String valSummary = String.join(", ", multi.selected);
                        subFont.drawString(context, valSummary, cardX + 30, settingY + 15, -1);
                        if (multi.open) {
                           int dropH = multi.options.length * 20;
                           RenderUtils.drawRoundedRect(context, cardX + 12, settingY + 29, cardW - 24, dropH + 4, 6.0F, -15198179);

                           for (int k = 0; k < multi.options.length; k++) {
                              String m = multi.options[k];
                              int itemYOffset = settingY + 31 + k * 20;
                              boolean isSel = multi.isSelected(m);
                              if (isSel) {
                                 RenderUtils.drawRoundedRect(context, cardX + 15, itemYOffset, cardW - 30, 18.0F, 5.0F, accentColor);
                                 subFont.drawString(context, m, cardX + 24, itemYOffset + 4, -1);
                              } else {
                                 subFont.drawString(context, m, cardX + 24, itemYOffset + 4, -2960681);
                              }
                           }

                           settingY += dropH + 8;
                        }

                        settingY += 30;
                     }
                  }
               }
            }

            context.getMatrices().popMatrix();
         } else if (this.selectedCategory == Category.MARKERS) {
            this.maxScroll = 250.0F;
            double scrolledMouseY = mouseY + currentScrollY;
            context.getMatrices().pushMatrix();
            context.getMatrices().translate((float) (0.0), (float) (-currentScrollY));
            int cardX = areaLeft + 14;
            int cardW = contentWidth - 28;
            int cardY = startY;
            int settingsHeaderH = 38;
            int settingsExpandedH = 150;
            int settingsCardH = settingsHeaderH + (this.markersSettingsExpanded ? settingsExpandedH : 0);
            RenderUtils.drawRoundedRect(context, cardX, cardY, cardW, settingsCardH, 8.0F, -15461351);
            iconFont.drawString(context, "\ue929", cardX + 12, cardY + 12, accentColor);
            mainFont.drawString(context, "Настройки меток", cardX + 28, cardY + 10, -1);
            subFont.drawString(context, "Глобальные параметры системы меток", cardX + 12, cardY + 24, -8750459);
            context.getMatrices().pushMatrix();
            float arrowScaleY = this.markersSettingsExpanded ? -1.0F : 1.0F;
            context.getMatrices().translate((float) (cardX + cardW - 18), (float) (cardY + 19));
            context.getMatrices().scale((float) (1.0F), (float) (arrowScaleY));
            iconFont.drawString(context, "\ue90d", -4.0F, -4.0F, -10132112);
            context.getMatrices().popMatrix();
            if (this.markersSettingsExpanded) {
               RenderUtils.drawRect(context, cardX + 10, cardY + 37, cardW - 20, 1.0F, -14540246);
               int sY = cardY + 44;
               subFont.drawString(context, "Метки смерти", cardX + 12, sY + 2, -1);
               DeathPosition deathMod = (DeathPosition)ModuleManager.getInstance().getModule("Death Markers");
               boolean deathOn = deathMod != null && deathMod.isEnabled();
               RenderUtils.drawRoundedRect(context, cardX + cardW - 23, sY + 1, 11.0F, 11.0F, 3.0F, deathOn ? accentColor : -14935006);
               if (deathOn) {
                  RenderUtils.drawCheckmark(context, cardX + cardW - 23, sY + 1, 11.0F, -1);
               }

               sY += 20;
               subFont.drawString(context, "Только последняя смерть", cardX + 12, sY + 2, -1);
               boolean lastDeathOn = deathMod != null && deathMod.autoRemove.value;
               RenderUtils.drawRoundedRect(context, cardX + cardW - 23, sY + 1, 11.0F, 11.0F, 3.0F, lastDeathOn ? accentColor : -14935006);
               if (lastDeathOn) {
                  RenderUtils.drawCheckmark(context, cardX + cardW - 23, sY + 1, 11.0F, -1);
               }

               sY += 20;
               subFont.drawString(context, "Лимит меток", cardX + 12, sY + 2, -1);
               MarkerSettings mSettings = (MarkerSettings)ModuleManager.getInstance().getModule("MarkerSettings");
               boolean limitOn = mSettings != null && mSettings.showIcons.value;
               RenderUtils.drawRoundedRect(context, cardX + cardW - 23, sY + 1, 11.0F, 11.0F, 3.0F, limitOn ? accentColor : -14935006);
               if (limitOn) {
                  RenderUtils.drawCheckmark(context, cardX + cardW - 23, sY + 1, 11.0F, -1);
               }

               sY += 18;
               double limitVal = mSettings != null ? mSettings.limitValue : 50.0;
               float pctLimit = (float)((limitVal - 1.0) / 99.0);
               pctLimit = Math.max(0.0F, Math.min(1.0F, pctLimit));
               subFont.drawString(context, "Максимум", cardX + 12, sY, -1);
               subFont.drawString(context, String.format("%.1f", limitVal), cardX + cardW - 46, sY, accentColor);
               sY += 10;
               int sliderW = cardW - 24;
               RenderUtils.drawRoundedRect(context, cardX + 12, sY, sliderW, 3.0F, 1.5F, -14935006);
               RenderUtils.drawRoundedRect(context, cardX + 12, sY, sliderW * pctLimit, 3.0F, 1.5F, accentColor);
               RenderUtils.drawRoundedRect(context, cardX + 12 + sliderW * pctLimit - 3.0F, sY - 1.5F, 6.0F, 6.0F, 3.0F, -1);
               sY += 16;
               subFont.drawString(context, "Дальность отображения", cardX + 12, sY + 2, -1);
               boolean distOn = mSettings != null && mSettings.showDistance.value;
               RenderUtils.drawRoundedRect(context, cardX + cardW - 23, sY + 1, 11.0F, 11.0F, 3.0F, distOn ? accentColor : -14935006);
               if (distOn) {
                  RenderUtils.drawCheckmark(context, cardX + cardW - 23, sY + 1, 11.0F, -1);
               }

               sY += 18;
               double distVal = mSettings != null ? mSettings.distance.value : 500.0;
               float pctDist = (float)((distVal - 50.0) / 4950.0);
               pctDist = Math.max(0.0F, Math.min(1.0F, pctDist));
               subFont.drawString(context, "Дистанция", cardX + 12, sY, -1);
               subFont.drawString(context, String.format("%.1f", distVal), cardX + cardW - 46, sY, accentColor);
               sY += 10;
               RenderUtils.drawRoundedRect(context, cardX + 12, sY, sliderW, 3.0F, 1.5F, -14935006);
               RenderUtils.drawRoundedRect(context, cardX + 12, sY, sliderW * pctDist, 3.0F, 1.5F, accentColor);
               RenderUtils.drawRoundedRect(context, cardX + 12 + sliderW * pctDist - 3.0F, sY - 1.5F, 6.0F, 6.0F, 3.0F, -1);
               sY += 16;
               subFont.drawString(context, "Быстрая метка", cardX + 12, sY + 2, -1);
               RenderUtils.drawRoundedRect(context, cardX + cardW - 32, sY, 20.0F, 14.0F, 4.0F, -15198180);
               subFont.drawString(context, "G", cardX + cardW - 25, sY + 3, -1);
            }

            cardY += settingsCardH + 10;
            AutoMarkers autoMod = (AutoMarkers)ModuleManager.getInstance().getModule("AutoMarkers");
            boolean autoOn = autoMod != null && autoMod.isEnabled();
            int autoHeaderH = 38;
            int autoExpandedH = 110;
            int autoCardH = autoHeaderH + (this.autoMarkersExpanded ? autoExpandedH : 0);
            RenderUtils.drawRoundedRect(context, cardX, cardY, cardW, autoCardH, 8.0F, -15461351);
            iconFont.drawString(context, "\ue910", cardX + 12, cardY + 12, accentColor);
            mainFont.drawString(context, "Авто метки", cardX + 28, cardY + 10, -1);
            subFont.drawString(context, "Автоматические метки на ивенты сервера ReallyWorld", cardX + 12, cardY + 24, -8750459);
            int swX = cardX + cardW - 48;
            int swY = cardY + 14;
            RenderUtils.drawRoundedRect(context, swX, swY, 18.0F, 10.0F, 5.0F, autoOn ? accentColor : -14935006);
            RenderUtils.drawRoundedRect(context, autoOn ? swX + 9 : swX + 1, swY + 1, 8.0F, 8.0F, 4.0F, -1);
            context.getMatrices().pushMatrix();
            float autoArrowScaleY = this.autoMarkersExpanded ? -1.0F : 1.0F;
            context.getMatrices().translate((float) (cardX + cardW - 18), (float) (cardY + 19));
            context.getMatrices().scale((float) (1.0F), (float) (autoArrowScaleY));
            iconFont.drawString(context, "\ue90d", -4.0F, -4.0F, -10132112);
            context.getMatrices().popMatrix();
            if (this.autoMarkersExpanded) {
               RenderUtils.drawRect(context, cardX + 10, cardY + 37, cardW - 20, 1.0F, -14540246);
               int aY = cardY + 44;
               subFont.drawString(context, "События", cardX + 12, aY, -1);
               aY += 12;
               RenderUtils.drawRoundedRect(context, cardX + 12, aY, cardW - 24, 16.0F, 5.0F, -15198179);
               iconFont.drawString(context, "\ue910", cardX + 17, aY + 4, -10132112);
               subFont.drawString(context, "АирДроп, Талисман, Тайный Торговец, Шахта Дворфов...", cardX + 30, aY + 4, -1);
               aY += 22;
               subFont.drawString(context, "Временные метки", cardX + 12, aY + 2, -1);
               RenderUtils.drawRoundedRect(context, cardX + cardW - 23, aY + 1, 11.0F, 11.0F, 3.0F, accentColor);
               RenderUtils.drawCheckmark(context, cardX + cardW - 23, aY + 1, 11.0F, -1);
               aY += 18;
               subFont.drawString(context, "Время жизни (мин)", cardX + 12, aY, -1);
               subFont.drawString(context, "15,0", cardX + cardW - 36, aY, accentColor);
               aY += 10;
               int sliderW = cardW - 24;
               RenderUtils.drawRoundedRect(context, cardX + 12, aY, sliderW, 3.0F, 1.5F, -14935006);
               RenderUtils.drawRoundedRect(context, cardX + 12, aY, sliderW / 4, 3.0F, 1.5F, accentColor);
               RenderUtils.drawRoundedRect(context, cardX + 12 + sliderW / 4 - 3, aY - 1.5F, 6.0F, 6.0F, 3.0F, -1);
               aY += 16;
               subFont.drawString(context, "Сообщение в чат", cardX + 12, aY + 2, -1);
               RenderUtils.drawRoundedRect(context, cardX + cardW - 23, aY + 1, 11.0F, 11.0F, 3.0F, accentColor);
               RenderUtils.drawCheckmark(context, cardX + cardW - 23, aY + 1, 11.0F, -1);
            }

            cardY += autoCardH + 10;
            int createHeaderH = 38;
            int createExpandedH = 175;
            int createCardH = createHeaderH + (this.createMarkerExpanded ? createExpandedH : 0);
            RenderUtils.drawRoundedRect(context, cardX, cardY, cardW, createCardH, 8.0F, -15461351);
            iconFont.drawString(context, "\ud83d\udea9", cardX + 12, cardY + 12, accentColor);
            mainFont.drawString(context, "Создание метки", cardX + 28, cardY + 10, -1);
            subFont.drawString(context, "Создавайте новые метки в одно касание!", cardX + 12, cardY + 24, -8750459);
            context.getMatrices().pushMatrix();
            float createArrowScaleY = this.createMarkerExpanded ? -1.0F : 1.0F;
            context.getMatrices().translate((float) (cardX + cardW - 18), (float) (cardY + 19));
            context.getMatrices().scale((float) (1.0F), (float) (createArrowScaleY));
            iconFont.drawString(context, "\ue90d", -4.0F, -4.0F, -10132112);
            context.getMatrices().popMatrix();
            if (this.createMarkerExpanded) {
               RenderUtils.drawRect(context, cardX + 10, cardY + 37, cardW - 20, 1.0F, -14540246);
               int cY = cardY + 44;
               subFont.drawString(context, "Название метки", cardX + 12, cY, -1);
               cY += 12;
               RenderUtils.drawRoundedRect(context, cardX + 12, cY, cardW - 24, 18.0F, 6.0F, this.activeMarkerInput == 1 ? -14408660 : -15198179);
               subFont.drawString(context, this.newMarkerName, cardX + 20, cY + 5, -1);
               cY += 24;
               subFont.drawString(context, "Цвет метки", cardX + 12, cY + 2, -1);
               int colorDotX = cardX + cardW - 75;
               RenderUtils.drawCircle(context, colorDotX, cY + 6, 4.5F, MARKER_COLORS[this.newMarkerColorIndex]);
               subFont.drawString(context, MARKER_COLOR_HEXES[this.newMarkerColorIndex], colorDotX + 8, cY + 2, -1);
               cY += 20;
               subFont.drawString(context, "Координаты", cardX + 12, cY, -1);
               cY += 12;
               int coordW = (cardW - 36) / 3;
               RenderUtils.drawRoundedRect(context, cardX + 12, cY, coordW, 18.0F, 6.0F, this.activeMarkerInput == 2 ? -14408660 : -15198179);
               subFont.drawString(context, this.newMarkerX, cardX + 12 + coordW / 2 - subFont.getStringWidth(this.newMarkerX) / 2, cY + 5, -1);
               RenderUtils.drawRoundedRect(context, cardX + 18 + coordW, cY, coordW, 18.0F, 6.0F, this.activeMarkerInput == 3 ? -14408660 : -15198179);
               subFont.drawString(context, this.newMarkerY, cardX + 18 + coordW + coordW / 2 - subFont.getStringWidth(this.newMarkerY) / 2, cY + 5, -1);
               RenderUtils.drawRoundedRect(context, cardX + 24 + coordW * 2, cY, coordW, 18.0F, 6.0F, this.activeMarkerInput == 4 ? -14408660 : -15198179);
               subFont.drawString(context, this.newMarkerZ, cardX + 24 + coordW * 2 + coordW / 2 - subFont.getStringWidth(this.newMarkerZ) / 2, cY + 5, -1);
               cY += 24;
               subFont.drawString(context, "Иконка", cardX + 12, cY, -1);
               cY += 12;
               String[] iconGlyphs = new String[]{"\ue934", "\ue900", "\ue925", "\ue910", "\ue91b", "\ue91a", "\ue926", "\ue905"};
               int iconBtnW = 18;

               for (int ic = 0; ic < 8; ic++) {
                  int btnX = cardX + 12 + ic * 22;
                  boolean isSel = this.newMarkerIconIndex == ic;
                  RenderUtils.drawRoundedRect(context, btnX, cY, iconBtnW, 18.0F, 5.0F, isSel ? accentColor : -15198179);
                  iconFont.drawString(context, iconGlyphs[ic], btnX + 5, cY + 4, isSel ? -1 : -8750459);
               }

               cY += 26;
               int createActionBtnX = cardX + 12;
               int createActionBtnW = 60;
               int createActionBtnH = 18;
               boolean cBtnHovered = mouseX >= createActionBtnX
                  && mouseX <= createActionBtnX + createActionBtnW
                  && scrolledMouseY >= cY
                  && scrolledMouseY <= cY + createActionBtnH;
               RenderUtils.drawRoundedRect(context, createActionBtnX, cY, createActionBtnW, createActionBtnH, 6.0F, cBtnHovered ? -14408654 : -14803414);
               subFont.drawString(context, "Создать", createActionBtnX + 10, cY + 4, -1);
            }

            cardY += createCardH + 15;
            List<WaypointManager.Waypoint> waypoints = WaypointManager.getWaypoints();
            int wpColW = (cardW - 12) / 2;
            int wpYLeft = cardY;
            int wpYRight = cardY;

            for (int i = 0; i < waypoints.size(); i++) {
               WaypointManager.Waypoint wp = waypoints.get(i);
               boolean isLeft = i % 2 == 0;
               int wpX = cardX + (isLeft ? 0 : wpColW + 12);
               int currentWpY = isLeft ? wpYLeft : wpYRight;
               int wpCardH = wp.expandedInGui ? 62 : 44;
               RenderUtils.drawRoundedRect(context, wpX, currentWpY, wpColW, wpCardH, 8.0F, -15461351);
               iconFont.drawString(context, wp.getIconString(), wpX + 10, currentWpY + 10, wp.color);
               mainFont.drawString(context, wp.name, wpX + 26, currentWpY + 8, -1);
               String coordsStr = (int)wp.pos.x + " / " + (int)wp.pos.y + " / " + (int)wp.pos.z;
               int coordsW = subFont.getStringWidth(coordsStr) + 8;
               int coordsX = wpX + wpColW - 46 - coordsW;
               RenderUtils.drawRoundedRect(context, coordsX, currentWpY + 7, coordsW, 13.0F, 4.0F, -15000802);
               subFont.drawString(context, coordsStr, coordsX + 4, currentWpY + 10, -8750459);
               int eyeX = wpX + wpColW - 36;
               iconFont.drawString(context, "\ue91c", eyeX, currentWpY + 10, wp.visible ? accentColor : -10132112);
               int chevX = wpX + wpColW - 18;
               iconFont.drawString(context, "\ue90d", chevX, currentWpY + 10, -10132112);
               subFont.drawString(context, wp.createdText, wpX + 10, currentWpY + 26, -8750459);
               if (wp.expandedInGui) {
                  int delBtnX = wpX + 10;
                  RenderUtils.drawRoundedRect(context, delBtnX, currentWpY + 42, 50.0F, 14.0F, 4.0F, -12773348);
                  subFont.drawString(context, "Удалить", delBtnX + 8, currentWpY + 45, -1);
               }

               if (isLeft) {
                  wpYLeft += wpCardH + 10;
               } else {
                  wpYRight += wpCardH + 10;
               }
            }

            context.getMatrices().popMatrix();
         } else {
            List<Module> categoryModules = ModuleManager.getInstance().searchModules(this.selectedCategory, this.searchQuery);
            int cardWidth = 171;
            int calcYLeft = 0;
            int calcYRight = 0;

            for (Module m : categoryModules) {
               int sH = this.getSettingsHeight(m);
               int cH = 41 + sH;
               if (calcYLeft <= calcYRight) {
                  calcYLeft += cH + 14;
               } else {
                  calcYRight += cH + 14;
               }
            }

            int maxColumnH = Math.max(calcYLeft, calcYRight);
            this.maxScroll = Math.max(0.0F, maxColumnH - viewportH + 30);
            float contentOffsetY = this.contentSlideAnim.getValue();
            context.getMatrices().pushMatrix();
            context.getMatrices().translate((float) (0.0), (float) (contentOffsetY - currentScrollY));
            int currentYLeft = startY;
            int currentYRight = startY;

            for (int i = 0; i < categoryModules.size(); i++) {
               Module module = categoryModules.get(i);
               float targetExp = module.isExpanded() ? 1.0F : 0.0F;
               float curExp = module.getExpandProgress();
               if (curExp < targetExp) {
                  module.setExpandProgress(Math.min(targetExp, curExp + 0.2F * delta));
               } else if (curExp > targetExp) {
                  module.setExpandProgress(Math.max(targetExp, curExp - 0.2F * delta));
               }

               if (module.isEnabled() && module.getToggleProgress() < 1.0F) {
                  module.setToggleProgress(Math.min(1.0F, module.getToggleProgress() + 0.2F * delta));
               } else if (!module.isEnabled() && module.getToggleProgress() > 0.0F) {
                  module.setToggleProgress(Math.max(0.0F, module.getToggleProgress() - 0.2F * delta));
               }

               int settingsHeight = this.getSettingsHeight(module);
               int baseCardHeight = 41;
               int cardHeight = baseCardHeight + (int)(settingsHeight * module.getExpandProgress());
               boolean isLeftCol = currentYLeft <= currentYRight;
               int moduleLeft = (int)(areaLeft + 14.5F + (isLeftCol ? 0.0F : cardWidth + 17.5F));
               int currentY = isLeftCol ? currentYLeft : currentYRight;
               double scrolledMouseY = mouseY + currentScrollY;
               boolean isExpanded = module.isExpanded();
               boolean isHovered = !isExpanded
                  && mouseX >= moduleLeft
                  && mouseX <= moduleLeft + cardWidth
                  && scrolledMouseY >= currentY
                  && scrolledMouseY <= currentY + cardHeight;
               RenderUtils.drawRoundedRect(context, moduleLeft - 1.5F, currentY - 1.5F, cardWidth + 3.0F, cardHeight + 3.0F, 9.5F, -15329767);
               RenderUtils.drawRoundedRect(context, moduleLeft - 0.75F, currentY - 0.75F, cardWidth + 1.5F, cardHeight + 1.5F, 8.8F, -15000802);
               RenderUtils.drawRoundedRect(context, moduleLeft, currentY, cardWidth, cardHeight, 8.0F, isHovered ? -15000800 : -15395561);
               int itemY = currentY + 7;
               int rightEdgeMargin = module.hasDropdown() ? 19 : 12;
               int switchX = moduleLeft + cardWidth - rightEdgeMargin - 16;
               String tagText = this.bindingModule == module ? "..." : module.getTag();
               int tagTextColor = this.bindingModule == module ? accentColor : -10132111;
               int tagBoxWidth = 34;
               tagText = fitText(subFont, tagText, tagBoxWidth - 4);
                int tagTextWidth = subFont.getStringWidth(tagText);
                float tagBoxHeight = 12.0F;
                int tagX = switchX - tagBoxWidth - 4;
                float tagY = itemY + 1.0F;
               if ("Создание метки".equals(module.getName())) {
                  mainFont.drawString(context, module.getName(), moduleLeft + 10, currentY + 7, -1);
                  subFont.drawString(context, module.getDescription(), moduleLeft + 10, currentY + 22, -7829354);
                  int createBtnX = moduleLeft + 10;
                  int createBtnY = currentY + 36;
                  int createBtnW = 55;
                  int createBtnH = 16;
                  boolean createBtnHovered = mouseX >= createBtnX
                     && mouseX <= createBtnX + createBtnW
                     && scrolledMouseY >= createBtnY
                     && scrolledMouseY <= createBtnY + createBtnH;
                  RenderUtils.drawRoundedRect(context, createBtnX, createBtnY, createBtnW, createBtnH, 5.0F, createBtnHovered ? -14408654 : -14803414);
                  subFont.drawString(context, "Создать", createBtnX + 8, createBtnY + 4, -1);
               } else {
                  String modTitle = module.getName();
                  float maxTitleW = tagX - (moduleLeft + 10);
                  if (mainFont.getStringWidth(modTitle) > maxTitleW) {
                     while (modTitle.length() > 3) {
                        StringBuilder stringBuilder = new StringBuilder();
                        if (!(mainFont.getStringWidth(stringBuilder.append(modTitle).append("..").toString()) > maxTitleW)) {
                           break;
                        }

                        modTitle = modTitle.substring(0, modTitle.length() - 1);
                     }

                     modTitle = modTitle + "..";
                  }

                  mainFont.drawString(context, modTitle, moduleLeft + 10, currentY + 7, -1);
                  String modDesc = module.getDescription();
                  float maxDescW = cardWidth - 20;
                  if (subFont.getStringWidth(modDesc) > maxDescW) {
                     while (modDesc.length() > 3) {
                        StringBuilder stringBuilder = new StringBuilder();
                        if (!(subFont.getStringWidth(stringBuilder.append(modDesc).append("..").toString()) > maxDescW)) {
                           break;
                        }

                        modDesc = modDesc.substring(0, modDesc.length() - 1);
                     }

                     modDesc = modDesc + "..";
                  }

                  subFont.drawString(context, modDesc, moduleLeft + 10, currentY + 22, -7829354);
                    RenderUtils.drawRoundedRect(context, tagX, tagY, tagBoxWidth, tagBoxHeight, 2.5F, this.bindingModule == module ? -14013894 : -15198180);
                    subFont.drawString(context, tagText, tagX + (tagBoxWidth - tagTextWidth) / 2.0F, subFont.getCenteredTextY(tagY, tagBoxHeight), tagTextColor);
                  int starX = tagX - 16;
                  if (module.isFavorite() || isHovered) {
                     int starColor = module.isFavorite() ? accentColor : -10132112;
                     iconFont.drawString(context, "\ue900", starX, itemY + 7 - iconFont.getGlyphHeight('\ue900') / 2.0F, starColor);
                  }

                  float tp = module.getToggleProgress();
                  int bgR = (int)(20.0F + tp * ((accentColor >> 16 & 0xFF) - 20));
                  int bgG = (int)(20.0F + tp * ((accentColor >> 8 & 0xFF) - 20));
                  int bgB = (int)(23.0F + tp * ((accentColor & 0xFF) - 23));
                  int bgColor = 0xFF000000 | bgR << 16 | bgG << 8 | bgB;
                  RenderUtils.drawRoundedRect(context, switchX, itemY + 2.5F, 14.0F, 9.0F, 4.5F, bgColor);
                  float thumbX = switchX + 1.5F + tp * 5.5F;
                  int thumbR = (int)(101.0F + tp * 154.0F);
                  int thumbG = (int)(101.0F + tp * 154.0F);
                  int thumbB = (int)(113.0F + tp * 142.0F);
                  int thumbColor = 0xFF000000 | thumbR << 16 | thumbG << 8 | thumbB;
                  RenderUtils.drawRoundedRect(context, thumbX, itemY + 4.25F, 5.5F, 5.5F, 2.75F, thumbColor);
                  if (module.hasDropdown()) {
                     context.getMatrices().pushMatrix();
                     float arrowScaleY = module.isExpanded() ? -1.0F : 1.0F;
                     context.getMatrices().translate((float) (moduleLeft + cardWidth - 14), (float) (itemY + 7));
                     context.getMatrices().scale((float) (0.7F), (float) (0.7F * arrowScaleY));
                     iconFont.drawString(context, "\ue90d", -4.0F, -4.0F, -12961216);
                     context.getMatrices().popMatrix();
                  }
               }

               if (module.getExpandProgress() > 0.0F) {
                  int revealBottom = currentY + cardHeight - 4;
                  RenderUtils.drawRect(context, moduleLeft + 10, currentY + 37, cardWidth - 20, 1.0F, -14540246);
                  if (module instanceof Crosshair) {
                     int btnX = moduleLeft + 10;
                     int btnY = currentY + 42;
                     int btnW = cardWidth - 20;
                     int btnH = 18;
                     if (btnY + btnH <= revealBottom) {
                        if (mouseX >= btnX && mouseX <= btnX + btnW && scrolledMouseY >= btnY && scrolledMouseY <= btnY + btnH) {
                           boolean btnHovered = true;
                        } else {
                           boolean btnHovered = false;
                        }

                        RenderUtils.drawRoundedRect(context, btnX, btnY, btnW, btnH, 8.0F, accentColor);
                        RenderUtils.drawTargetIcon(context, btnX + 8, btnY + 5.0F, 8.0F, -1);
                        mainFont.drawString(context, "Настройки прицела", btnX + 20, btnY + 4.5F, -1);
                     }
                  } else if (module instanceof CustomHand) {
                     int btnX = moduleLeft + 10;
                     int btnY = currentY + 42;
                     int btnW = cardWidth - 20;
                     int btnH = 18;
                     if (btnY + btnH <= revealBottom) {
                        boolean btnHovered = mouseX >= btnX && mouseX <= btnX + btnW && scrolledMouseY >= btnY && scrolledMouseY <= btnY + btnH;
                        RenderUtils.drawRoundedRect(context, btnX, btnY, btnW, btnH, 8.0F, btnHovered ? -14408660 : -15198179);
                        iconFont.drawString(context, "\ue917", btnX + 8, btnY + 5.0F, -8750459);
                        mainFont.drawString(context, "Открыть редактор", btnX + 22, btnY + 4.5F, -1);
                     }
                  } else {
                     int settingY = currentY + 42;

                     for (Module.Setting setting : module.getSettings()) {
                        if (settingY >= revealBottom) {
                           break;
                        }

                        if (setting.isVisible()) {
                           if (setting instanceof Module.BooleanSetting) {
                              Module.BooleanSetting bool = (Module.BooleanSetting)setting;
                              subFont.drawString(context, bool.name, moduleLeft + 10, settingY + 2, -1);
                              int boxSize = 11;
                              int boxX = moduleLeft + cardWidth - 10 - boxSize;
                              int boxY = settingY + 1;
                              if (bool.value) {
                                  RenderUtils.drawRoundedRect(context, boxX, boxY, boxSize, boxSize, 3.0F, accentColor);
                                  RenderUtils.drawCheckmark(context, boxX, boxY, boxSize, -1);
                              } else {
                                 RenderUtils.drawRoundedRect(context, boxX, boxY, boxSize, boxSize, 3.0F, -14935006);
                              }

                              settingY += 18;
                              if ("Цвет клиента".equalsIgnoreCase(bool.name) && !bool.value) {
                                 subFont.drawString(context, "Кастомный цвет", moduleLeft + 10, settingY + 2, -1);
                                 int dotX = moduleLeft + cardWidth - 65;
                                 RenderUtils.drawCircle(context, dotX, settingY + 6, 4.5F, module.customColor);
                                 String customHex = String.format("#%06X", module.customColor & 16777215);
                                 subFont.drawString(context, customHex, dotX + 8, settingY + 2, -1);
                                 settingY += 18;
                              }
                           } else if (setting instanceof Module.NumberSetting) {
                              Module.NumberSetting num = (Module.NumberSetting)setting;
                              subFont.drawString(context, num.name, moduleLeft + 10, settingY, -1);
                              String valStr = String.format("%.1f", num.value).replace(".", ",");
                              subFont.drawString(context, valStr, moduleLeft + cardWidth - 10 - subFont.getStringWidth(valStr), settingY, accentColor);
                              int sliderWidth = cardWidth - 20;
                              RenderUtils.drawRoundedRect(context, moduleLeft + 10, settingY + 13, sliderWidth, 2.0F, 1.0F, -14935006);
                              float fillRatio = (float)((num.value - num.min) / (num.max - num.min));
                              RenderUtils.drawRoundedRect(context, moduleLeft + 10, settingY + 13, (int)(sliderWidth * fillRatio), 2.0F, 1.0F, accentColor);
                              float knobX = moduleLeft + 10 + sliderWidth * fillRatio - 2.5F;
                              RenderUtils.drawRoundedRect(context, knobX, settingY + 11.5F, 5.0F, 5.0F, 2.5F, -1);
                              settingY += 24;
                           } else if (setting instanceof Module.ModeSetting) {
                              Module.ModeSetting mode = (Module.ModeSetting)setting;
                              subFont.drawString(context, mode.name, moduleLeft + 10, settingY, -1);
                              RenderUtils.drawRoundedRect(context, moduleLeft + 10, settingY + 11, cardWidth - 20, 16.0F, 5.0F, -15198179);
                              iconFont.drawString(context, "\ue910", moduleLeft + 15, settingY + 15, -10132112);
                              subFont.drawString(context, mode.value, moduleLeft + 28, settingY + 15, -1);
                              context.getMatrices().pushMatrix();
                              float arrowScaleY = mode.open ? -1.0F : 1.0F;
                              context.getMatrices().translate((float) (moduleLeft + cardWidth - 18), (float) (settingY + 19));
                              context.getMatrices().scale((float) (1.0F), (float) (arrowScaleY));
                              iconFont.drawString(context, "\ue90d", -4.0F, -4.0F, mode.open ? -1 : -10132112);
                               context.getMatrices().popMatrix();
                               if (mode.open) {
                                  mode.dropProgress = Math.min(1.0F, mode.dropProgress + 0.2F * delta);
                               } else {
                                  mode.dropProgress = Math.max(0.0F, mode.dropProgress - 0.2F * delta);
                               }

                               if (mode.open) {
                                  int dropH = mode.modes.length * 20;
                                  RenderUtils.drawRoundedRect(context, moduleLeft + 10, settingY + 29, cardWidth - 20, dropH + 4, 6.0F, -15198179);

                                  for (int k = 0; k < mode.modes.length; k++) {
                                     String m = mode.modes[k];
                                     int itemYOffset = settingY + 31 + k * 20;
                                     boolean isSel = m.equals(mode.value);
                                     boolean isItemHovered = mouseX >= moduleLeft + 10
                                        && mouseX <= moduleLeft + cardWidth - 10
                                        && scrolledMouseY >= itemYOffset
                                        && scrolledMouseY < itemYOffset + 18;
                                     if (isSel) {
                                        RenderUtils.drawRoundedRect(context, moduleLeft + 13, itemYOffset, cardWidth - 26, 18.0F, 5.0F, accentColor);
                                        subFont.drawString(context, m, moduleLeft + 22, itemYOffset + 4, -1);
                                     } else {
                                        if (isItemHovered) {
                                           RenderUtils.drawRoundedRect(context, moduleLeft + 13, itemYOffset, cardWidth - 26, 18.0F, 5.0F, -14408660);
                                        }

                                        subFont.drawString(context, "> " + m, moduleLeft + 22, itemYOffset + 4, -2960681);
                                     }
                                  }

                                  if (mode.dropProgress < 1.0F) {
                                     RenderUtils.drawRect(context, moduleLeft + 10, settingY + 29, cardWidth - 20, dropH + 4, (int)((1.0F - mode.dropProgress) * 180.0F) << 24);
                                  }

                                  settingY += dropH + 8;
                               }

                              settingY += 30;
                           } else if (setting instanceof Module.MultiSelectSetting) {
                              Module.MultiSelectSetting multi = (Module.MultiSelectSetting)setting;
                              subFont.drawString(context, multi.name, moduleLeft + 10, settingY, -1);
                              RenderUtils.drawRoundedRect(context, moduleLeft + 10, settingY + 11, cardWidth - 20, 16.0F, 5.0F, -15198179);
                              iconFont.drawString(context, "\ue910", moduleLeft + 15, settingY + 15, -10132112);
                              String valSummary = multi.selected.isEmpty()
                                 ? "Ничего"
                                 : (
                                    multi.selected.size() == multi.options.length
                                       ? "Всё (" + multi.selected.size() + ")"
                                       : (multi.selected.size() > 2 ? "Выбрано: " + multi.selected.size() : String.join(", ", multi.selected))
                                 );
                              subFont.drawString(context, valSummary, moduleLeft + 28, settingY + 15, -1);
                              context.getMatrices().pushMatrix();
                              float arrowScaleY = multi.open ? -1.0F : 1.0F;
                              context.getMatrices().translate((float) (moduleLeft + cardWidth - 18), (float) (settingY + 19));
                              context.getMatrices().scale((float) (1.0F), (float) (arrowScaleY));
                              iconFont.drawString(context, "\ue90d", -4.0F, -4.0F, multi.open ? -1 : -10132112);
                               context.getMatrices().popMatrix();
                               if (multi.open) {
                                  multi.dropProgress = Math.min(1.0F, multi.dropProgress + 0.2F * delta);
                               } else {
                                  multi.dropProgress = Math.max(0.0F, multi.dropProgress - 0.2F * delta);
                               }

                               if (multi.open) {
                                  int dropH = multi.options.length * 20;
                                  RenderUtils.drawRoundedRect(context, moduleLeft + 10, settingY + 29, cardWidth - 20, dropH + 4, 6.0F, -15198179);

                                  for (int k = 0; k < multi.options.length; k++) {
                                     String m = multi.options[k];
                                     int itemYOffset = settingY + 31 + k * 20;
                                     boolean isSel = multi.isSelected(m);
                                     boolean isItemHovered = mouseX >= moduleLeft + 10
                                        && mouseX <= moduleLeft + cardWidth - 10
                                        && scrolledMouseY >= itemYOffset
                                        && scrolledMouseY < itemYOffset + 18;
                                     if (isSel) {
                                        RenderUtils.drawRoundedRect(context, moduleLeft + 13, itemYOffset, cardWidth - 26, 18.0F, 5.0F, accentColor);
                                        subFont.drawString(context, m, moduleLeft + 22, itemYOffset + 4, -1);
                                     } else {
                                        if (isItemHovered) {
                                           RenderUtils.drawRoundedRect(context, moduleLeft + 13, itemYOffset, cardWidth - 26, 18.0F, 5.0F, -14408660);
                                        }

                                        subFont.drawString(context, "> " + m, moduleLeft + 22, itemYOffset + 4, -2960681);
                                     }
                                  }

                                  if (multi.dropProgress < 1.0F) {
                                     RenderUtils.drawRect(context, moduleLeft + 10, settingY + 29, cardWidth - 20, dropH + 4, (int)((1.0F - multi.dropProgress) * 180.0F) << 24);
                                  }

                                  settingY += dropH + 8;
                               }

                              settingY += 30;
                           } else if (setting instanceof Module.TextSetting) {
                              Module.TextSetting txt = (Module.TextSetting)setting;
                              subFont.drawString(context, txt.name, moduleLeft + 10, settingY, -1);
                              RenderUtils.drawRoundedRect(
                                 context, moduleLeft + 10, settingY + 11, cardWidth - 20, 16.0F, 5.0F, txt.focused ? -14408660 : -15198179
                              );
                              subFont.drawString(context, txt.value, moduleLeft + 18, settingY + 15, txt.focused ? accentColor : -1);
                              settingY += 30;
                           } else if (setting instanceof Module.KeySetting) {
                              Module.KeySetting keySet = (Module.KeySetting)setting;
                              subFont.drawString(context, keySet.name, moduleLeft + 10, settingY + 2, -1);
                               String keyText = keySet.listening ? "..." : keySet.keyName;
                               int keyBoxW = 42;
                               keyText = fitText(subFont, keyText, keyBoxW - 6);
                               int keyTextW = subFont.getStringWidth(keyText);
                              int keyBoxX = moduleLeft + cardWidth - 10 - keyBoxW;
                              RenderUtils.drawRoundedRect(context, keyBoxX, settingY, keyBoxW, 14.0F, 4.0F, keySet.listening ? -14013894 : -15198180);
                              subFont.drawString(
                                 context, keyText, keyBoxX + (keyBoxW - keyTextW) / 2.0F, subFont.getCenteredTextY(settingY, 14.0F), keySet.listening ? accentColor : -2960681
                              );
                              settingY += 18;
                           }
                        }
                     }
                  }
               }

               if (isLeftCol) {
                  currentYLeft += cardHeight + 14;
               } else {
                  currentYRight += cardHeight + 14;
               }
            }

            context.getMatrices().popMatrix();
         }

         context.disableScissor();
         if (this.maxScroll > 0.0F) {
            float scrollbarX = areaLeft + contentWidth - 5;
            int trackY = this.guiTop + 48;
            int trackH = viewportH;
            int thumbH = Math.max(24, (int)(viewportH / Math.max(1.0F, this.maxScroll + viewportH) * viewportH));
            float thumbY = trackY + currentScrollY / this.maxScroll * (trackH - thumbH);
            RenderUtils.drawGlow(context, scrollbarX, thumbY, 2.0F, thumbH, 1.0F, accentColor, 3, 0.2F);
            RenderUtils.drawRoundedRect(context, scrollbarX, thumbY, 2.0F, thumbH, 1.0F, accentColor);
         }

         context.getMatrices().popMatrix();
         context.getMatrices().popMatrix();
         context.getMatrices().popMatrix();

         if (this.pickerOpen) {
            this.renderColorPickerPopup(context, mouseX, mouseY);
         }

         super.render(context, mouseX, mouseY, delta);
      }
   }

   private void renderDragmodeOverlay(
      DrawContext context, int mouseX, int mouseY, CustomFont mainFont, CustomFont titleFont, CustomFont subFont, CustomFont iconFont
   ) {
      int w = this.width;
      int h = this.height;
      RenderUtils.drawRect(context, 0.0F, 0.0F, w, h, 805306368);

      for (int gx = 0; gx < w; gx += 20) {
         RenderUtils.drawRect(context, gx, 0.0F, 1.0F, h, 369098751);
      }

      for (int gy = 0; gy < h; gy += 20) {
         RenderUtils.drawRect(context, 0.0F, gy, w, 1.0F, 369098751);
      }

      for (HUDManager.HUDElement elem : HUDManager.getAllElements()) {
          if (elem.enabled) {
             float ex = (float)Math.round(elem.x);
             float ey = (float)Math.round(elem.y);
             float ew = elem.width;
             float eh = elem.height;
            boolean isHovered = mouseX >= ex && mouseX <= ex + ew && mouseY >= ey && mouseY <= ey + eh;
            Module mod = ModuleManager.getInstance().getModule(elem.name);
            if (mod != null && mod.isEnabled()) {
               mod.onRenderHUD(context);
            } else if ("Watermark".equals(elem.name)) {
               Module wm = ModuleManager.getInstance().getModule("Watermark");
               if (wm != null) {
                  wm.onRenderHUD(context);
               }
            } else if ("Bossbar".equals(elem.name)) {
               RenderUtils.drawRoundedRect(context, ex, ey, ew, eh, 6.0F, -300674023);
               subFont.drawString(context, "Bossbar", ex + (ew - subFont.getStringWidth("Bossbar")) / 2.0F, ey + 5.0F, -8750459);
            } else if ("Scoreboard".equals(elem.name)) {
               RenderUtils.drawRoundedRect(context, ex, ey, ew, eh, 6.0F, -300674023);
               subFont.drawString(context, "Scoreboard", ex + (ew - subFont.getStringWidth("Scoreboard")) / 2.0F, ey + (eh - 8.0F) / 2.0F, -8750459);
            } else if ("Notification Preview".equals(elem.name)) {
               RenderUtils.drawRoundedRect(context, ex, ey, ew, eh, 6.0F, -300674023);
               subFont.drawString(context, "i  Notification Preview", ex + 8.0F, ey + 5.0F, -1);
            } else {
               RenderUtils.drawRoundedRect(context, ex, ey, ew, eh, 6.0F, -300674023);
               String phLabel = subFont.getStringWidth(elem.name) > ew - 22.0F
                  ? fitText(subFont, elem.name, (int)(ew - 22.0F))
                  : elem.name;
               subFont.drawString(context, phLabel, ex + 6.0F, ey + 5.0F, -8750459);
               subFont.drawString(context, "OFF", ex + 6.0F, ey + 15.0F, -6250326);
            }

            RenderUtils.drawSingleRoundedRect(context, ex, ey, ew, eh, 6.0F, isHovered ? accentColor : 1358954495);
            float handleX = ex + ew - 16.0F;
            float handleY = ey + 4.0F;
            boolean handleHovered = mouseX >= handleX - 2.0F && mouseX <= handleX + 14.0F && mouseY >= handleY - 2.0F && mouseY <= handleY + 14.0F;
            iconFont.drawString(context, "\ue90b", handleX, handleY, handleHovered ? accentColor : -7434605);
            if (this.activeMenuHUDElement == elem) {
               float menuX = handleX - 40.0F;
               float menuY = handleY + 16.0F;
               float menuW = 110.0F;
               float menuH = "Notification Preview".equals(elem.name) ? 75.0F : 85.0F;
               RenderUtils.drawRoundedRect(context, menuX - 1.0F, menuY - 1.0F, menuW + 2.0F, menuH + 2.0F, 8.0F, accentColor);
               RenderUtils.drawRoundedRect(context, menuX, menuY, menuW, menuH, 8.0F, -15461351);
               iconFont.drawString(context, "\ue90b", menuX + 8.0F, menuY + 8.0F, accentColor);
               if ("Notification Preview".equals(elem.name)) {
                  mainFont.drawString(context, "Notifications", menuX + 22.0F, menuY + 6.0F, -1);
                  subFont.drawString(context, "Настройки уведомлений", menuX + 8.0F, menuY + 20.0F, -8750459);
                  subFont.drawString(context, "Уведомления", menuX + 10.0F, menuY + 36.0F, -1);
                  RenderUtils.drawRoundedRect(context, menuX + menuW - 20.0F, menuY + 35.0F, 10.0F, 10.0F, 3.0F, accentColor);
                  RenderUtils.drawCheckmark(context, menuX + menuW - 20.0F, menuY + 35.0F, 10.0F, -1);
                  subFont.drawString(context, "Модули", menuX + 10.0F, menuY + 54.0F, -1);
                  RenderUtils.drawRoundedRect(context, menuX + menuW - 20.0F, menuY + 53.0F, 10.0F, 10.0F, 3.0F, accentColor);
                  RenderUtils.drawCheckmark(context, menuX + menuW - 20.0F, menuY + 53.0F, 10.0F, -1);
               } else {
                  mainFont.drawString(context, "Scale", menuX + 22.0F, menuY + 6.0F, -1);
                  subFont.drawString(context, "Выбери размер элемента", menuX + 8.0F, menuY + 20.0F, -8750459);
                  String[] scales = new String[]{"Small", "Medium", "Large"};
                  float optionY = menuY + 34.0F;

                  for (String sc : scales) {
                     boolean isSelectedScale = sc.equalsIgnoreCase(elem.scale);
                     if (isSelectedScale) {
                        RenderUtils.drawRoundedRect(context, menuX + 6.0F, optionY, menuW - 12.0F, 14.0F, 5.0F, accentColor);
                        subFont.drawString(context, sc, menuX + 14.0F, optionY + 3.0F, -1);
                     } else {
                        subFont.drawString(context, sc, menuX + 14.0F, optionY + 3.0F, -2960681);
                     }

                     optionY += 16.0F;
                  }
               }
            }
         }
      }

      int exitX = 15;
      int exitY = h - 25;
      boolean exitHovered = mouseX >= exitX && mouseX <= exitX + 85 && mouseY >= exitY && mouseY <= exitY + 18;
      RenderUtils.drawRoundedRect(context, exitX, exitY, 85.0F, 18.0F, 9.0F, exitHovered ? -14408660 : -15198179);
      iconFont.drawString(context, "\ue90b", exitX + 8, exitY + 4, accentColor);
      subFont.drawString(context, "Dragmode", exitX + 24, exitY + 5, -1);
   }

   public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
      double mouseX = click.x(); double mouseY = click.y(); int button = click.button();
      Module.KeySetting listeningKey = this.getListeningKeySetting();
      if (listeningKey != null) {
         listeningKey.setKey(KeyUtils.toMouseBind(button));
         listeningKey.listening = false;
         UISoundHelper.playSound("ui.keybind_set");
         return true;
      }

      if (this.bindingMenuKey) {
         this.bindingMenuKey = false;
         menuKeyCode = KeyUtils.toMouseBind(button);
         menuKeyName = KeyUtils.getShortKeyName(menuKeyCode);
         UISoundHelper.playSound("ui.keybind_set");
         return true;
      }

      if (this.bindingModule != null) {
         Module bm = this.bindingModule;
         this.bindingModule = null;
         bm.setKey(KeyUtils.toMouseBind(button));
         UISoundHelper.playSound("ui.keybind_set");
         return true;
      }

      if (!this.showingDragmode) {
         float openScale = this.openAnim.getValue();
         float openSlide = this.openYAnim.getValue();
         if (openScale < 0.999F || openSlide > 0.5F) {
            float cx = this.width / 2.0F;
            float cy = this.height / 2.0F;
            double my = mouseY - openSlide;
            mouseX = (mouseX - cx) / openScale + cx;
            mouseY = (my - cy) / openScale + cy;
         }

         if (uiScale != 1.0F && uiScale > 0.1F) {
            mouseX = (mouseX - (this.guiLeft + 242.0F)) / uiScale + (this.guiLeft + 242.0F);
            mouseY = (mouseY - (this.guiTop + 140.0F)) / uiScale + (this.guiTop + 140.0F);
         }
      }

      if (this.pickerOpen) {
         int popupW = 172;
         int popupH = 186;
         int popupX = this.colorPickerX();
         int popupY = this.colorPickerY();
         int svX = this.pickerSvX();
         int svY = this.pickerSvY();
         int svW = this.pickerSvW();
         int svH = this.pickerSvH();
         int hueY = this.pickerHueY();
         if (mouseX >= svX && mouseX <= svX + svW && mouseY >= svY && mouseY <= svY + svH) {
            this.pickerDragging = 1;
            this.updatePickerFromMouse(mouseX, mouseY);
            UISoundHelper.playSound("ui.slider_grab");
            return true;
         } else if (mouseX >= svX && mouseX <= svX + svW && mouseY >= hueY && mouseY <= hueY + this.pickerHueH()) {
            this.pickerDragging = 2;
            this.updatePickerFromMouse(mouseX, mouseY);
            UISoundHelper.playSound("ui.slider_grab");
            return true;
         } else if (mouseX >= popupX && mouseX <= popupX + popupW && mouseY >= popupY && mouseY <= popupY + popupH) {
            return true;
         } else {
            this.pickerOpen = false;
            this.pickerDragging = 0;
            this.colorEditModule = null;
            return true;
         }
      }

      int sidebarWidth = (int)this.sidebarAnim.getValue();
      if (this.showingDragmode) {
         if (this.activeMenuHUDElement == null) {
            for (HUDManager.HUDElement elem : HUDManager.getAllElements()) {
               if (elem.enabled) {
                  float ex = elem.x;
                  float ey = elem.y;
                  float ew = elem.width;
                  float eh = elem.height;
                  float handleX = ex + ew - 16.0F;
                  float handleY = ey + 5.0F;
                  if (mouseX >= handleX - 2.0F && mouseX <= handleX + 14.0F && mouseY >= handleY - 2.0F && mouseY <= handleY + 14.0F) {
                     this.activeMenuHUDElement = this.activeMenuHUDElement == elem ? null : elem;
                     UISoundHelper.playSound("ui.dropdown_open");
                     return true;
                  }

                  if (mouseX >= ex && mouseX <= ex + ew && mouseY >= ey && mouseY <= ey + eh) {
                     this.draggingHUDElement = elem;
                     this.dragOffsetX = (float)mouseX - elem.x;
                     this.dragOffsetY = (float)mouseY - elem.y;
                     UISoundHelper.playSound("ui.hud_grab");
                     return true;
                  }
               }
            }

            int exitX = 15;
            int exitY = this.height - 25;
            if (mouseX >= exitX && mouseX <= exitX + 85 && mouseY >= exitY && mouseY <= exitY + 18) {
               this.showingDragmode = false;
               UISoundHelper.playSound("ui.dropdown_close");
               return true;
            } else {
               return true;
            }
         } else {
            HUDManager.HUDElement elem = this.activeMenuHUDElement;
            float handleX = elem.x + elem.width - 16.0F;
            float handleY = elem.y + 5.0F;
            float menuX = handleX - 40.0F;
            float menuY = handleY + 16.0F;
            float menuW = 110.0F;
            if (!"Notification Preview".equals(elem.name) && !"Bossbar".equals(elem.name) && !"Scoreboard".equals(elem.name)) {
               String[] scales = new String[]{"Small", "Medium", "Large"};
               float optionY = menuY + 34.0F;

               for (String sc : scales) {
                  if (mouseX >= menuX + 6.0F && mouseX <= menuX + menuW - 6.0F && mouseY >= optionY && mouseY <= optionY + 14.0F) {
                     elem.scale = sc;
                     UISoundHelper.playSound("ui.dropdown_select");
                     this.activeMenuHUDElement = null;
                     return true;
                  }

                  optionY += 16.0F;
               }
            }

            this.activeMenuHUDElement = null;
            UISoundHelper.playSound("ui.dropdown_close");
            return true;
         }
      } else {
         int profileY = this.guiTop + 280 - 34;
         int hamX = !this.sidebarCollapsed ? this.guiLeft + sidebarWidth - 18 : this.guiLeft + 21;
         int hamY = !this.sidebarCollapsed ? profileY + 5 : profileY + 18;
         if (mouseX >= hamX - 4 && mouseX <= hamX + 14 && mouseY >= hamY - 4 && mouseY <= hamY + 14) {
            this.sidebarCollapsed = !this.sidebarCollapsed;
            UISoundHelper.playSound("ui.navigate");
            return true;
         }

         if (!this.showingClientSettings && this.selectedCategory == Category.HUD) {
            int hudTitleW = FontManager.getTitleFont().getStringWidth("HUD");
            int dragBtnX = this.guiLeft + sidebarWidth + 18 + hudTitleW + 8;
            int dragBtnY = this.guiTop + 15;
            if (mouseX >= dragBtnX && mouseX <= dragBtnX + 18 && mouseY >= dragBtnY && mouseY <= dragBtnY + 18) {
               this.showingDragmode = true;
               UISoundHelper.playSound("ui.dropdown_open");
               return true;
            }
         }

         int gearX = this.guiLeft + 484 - 28;
         int gearY = this.guiTop + 13;
          if (mouseX >= gearX && mouseX <= gearX + 18 && mouseY >= gearY && mouseY <= gearY + 18) {
             this.showingClientSettings = !this.showingClientSettings;
             this.targetScroll = 0.0F;
             this.scrollAnimation.force(0.0F);
             this.contentSlideAnim.force(10.0F);
             this.contentSlideAnim.animateTo(0.0F, 200L);
             UISoundHelper.playSound("ui.dropdown_open");
             return true;
          }

         int navY = this.guiTop + 38;

         for (Category category : Category.values()) {
            if (mouseX >= this.guiLeft + 4 && mouseX <= this.guiLeft + sidebarWidth - 4 && mouseY >= navY - 2 && mouseY <= navY + 22) {
               if (this.selectedCategory != category || this.showingClientSettings) {
                  this.showingClientSettings = false;
                  this.selectedCategory = category;
                  this.targetScroll = 0.0F;
                  this.scrollAnimation.force(0.0F);
                  this.contentSlideAnim.force(10.0F);
                  this.contentSlideAnim.animateTo(0.0F, 200L);
                  UISoundHelper.playSound("ui.navigate");
               }

               return true;
            }

            navY += 26;
         }

         float currentScrollY = this.scrollAnimation.getValue();
         double scrolledMouseY = mouseY + currentScrollY;
         int areaLeft = this.guiLeft + sidebarWidth;
         int contentWidth = 484 - sidebarWidth;
         int startY = this.guiTop + 60;
         if (this.showingClientSettings) {
            int cardX = areaLeft + 14;
            int cardW = contentWidth - 28;
            int cardY = startY;
            if (mouseX >= cardX + 12 && mouseX <= cardX + cardW - 12) {
               if (scrolledMouseY >= cardY + 65 && scrolledMouseY <= cardY + 78) {
                  this.activeColorPicker = "accent";
                  this.syncPickerFromColor(accentColor);
                  this.pickerOpen = true;
                  UISoundHelper.playSound("ui.color_pick");
                  return true;
               } else if (scrolledMouseY >= cardY + 81 && scrolledMouseY <= cardY + 94) {
                  this.activeColorPicker = "client";
                  this.syncPickerFromColor(clientColor);
                  this.pickerOpen = true;
                  UISoundHelper.playSound("ui.color_pick");
                  return true;
               }
            }

            int uiSliderX = cardX + 12;
            int uiSliderY = startY + 54;
            int uiSliderW = cardW - 24;
            if (mouseX >= uiSliderX && mouseX <= uiSliderX + uiSliderW && scrolledMouseY >= uiSliderY && scrolledMouseY <= uiSliderY + 12) {
               this.draggingClientSlider = 1;
               this.clientSliderX = uiSliderX;
               this.clientSliderW = uiSliderW;
               this.clientSliderStartScale = uiScale;
               float ratio = (float)(mouseX - uiSliderX) / uiSliderW;
               this.clientSliderDragRatio = Math.max(0.0F, Math.min(1.0F, ratio));
               uiScale = Math.round((0.5F + this.clientSliderDragRatio * 1.5F) * 10.0F) / 10.0F;
               UISoundHelper.playSound("ui.slider_grab");
               return true;
            } else {
               int keyY = startY + 110 + 44;
               if (mouseX >= cardX + cardW - 60 && mouseX <= cardX + cardW - 10 && scrolledMouseY >= keyY - 4 && scrolledMouseY <= keyY + 14) {
                  this.bindingMenuKey = true;
                  UISoundHelper.playSound("ui.keybind_listen");
                  return true;
               } else {
                   int sndBoxX = cardX + cardW - 22;
                   int sndBoxY = startY + 190 + 44;
                  if (mouseX >= sndBoxX - 4 && mouseX <= sndBoxX + 14 && scrolledMouseY >= sndBoxY - 4 && scrolledMouseY <= sndBoxY + 14) {
                     soundEnabled = !soundEnabled;
                     UISoundHelper.playSound(soundEnabled ? "ui.toggle_on" : "ui.toggle_off");
                     return true;
                  } else {
                      int volSliderX = cardX + 12;
                      int volSliderY = startY + 190 + 66;
                     int volSliderW = cardW - 24;
                     if (mouseX >= volSliderX && mouseX <= volSliderX + volSliderW && scrolledMouseY >= volSliderY && scrolledMouseY <= volSliderY + 12) {
                        this.draggingClientSlider = 2;
                        this.clientSliderX = volSliderX;
                        this.clientSliderW = volSliderW;
                        float ratio = (float)(mouseX - volSliderX) / volSliderW;
                        this.clientSliderDragRatio = Math.max(0.0F, Math.min(1.0F, ratio));
                        soundVolume = Math.round(this.clientSliderDragRatio * 100.0F);
                        UISoundHelper.playSound("ui.slider_grab");
                        return true;
                     } else {
                         int modSndBoxX = cardX + cardW - 22;
                         int modSndBoxY = startY + 190 + 76;
                        if (mouseX >= modSndBoxX - 4 && mouseX <= modSndBoxX + 14 && scrolledMouseY >= modSndBoxY - 4 && scrolledMouseY <= modSndBoxY + 14) {
                           moduleSoundsEnabled = !moduleSoundsEnabled;
                           UISoundHelper.playSound(moduleSoundsEnabled ? "ui.toggle_on" : "ui.toggle_off");
                           return true;
                        } else {
                           return true;
                        }
                     }
                  }
               }
            }
         } else if (this.selectedCategory == Category.MARKERS) {
            int cardX = areaLeft + 14;
            int cardW = contentWidth - 28;
            int cardY = startY;
            int settingsHeaderH = 38;
            int settingsExpandedH = 150;
            int settingsCardH = settingsHeaderH + (this.markersSettingsExpanded ? settingsExpandedH : 0);
            if (mouseX >= cardX && mouseX <= cardX + cardW && scrolledMouseY >= cardY && scrolledMouseY <= cardY + 38) {
               this.markersSettingsExpanded = !this.markersSettingsExpanded;
               UISoundHelper.playSound(this.markersSettingsExpanded ? "ui.module_expand" : "ui.module_collapse");
               return true;
            }

            if (this.markersSettingsExpanded) {
               int sY = cardY + 44;
               DeathPosition deathMod = (DeathPosition)ModuleManager.getInstance().getModule("Death Markers");
               if (mouseX >= cardX + 10 && mouseX <= cardX + cardW - 10 && scrolledMouseY >= sY - 2 && scrolledMouseY <= sY + 16) {
                  if (deathMod != null) {
                     deathMod.toggle();
                  }

                  UISoundHelper.playSound(deathMod != null && deathMod.isEnabled() ? "ui.toggle_on" : "ui.toggle_off");
                  return true;
               }

               sY += 20;
               if (mouseX >= cardX + 10 && mouseX <= cardX + cardW - 10 && scrolledMouseY >= sY - 2 && scrolledMouseY <= sY + 16) {
                  if (deathMod != null) {
                     deathMod.autoRemove.value = !deathMod.autoRemove.value;
                  }

                  UISoundHelper.playSound(deathMod != null && deathMod.autoRemove.value ? "ui.toggle_on" : "ui.toggle_off");
                  return true;
               }

               sY += 20;
               MarkerSettings mSettings = (MarkerSettings)ModuleManager.getInstance().getModule("MarkerSettings");
               if (mouseX >= cardX + 10 && mouseX <= cardX + cardW - 10 && scrolledMouseY >= sY - 2 && scrolledMouseY <= sY + 14) {
                  if (mSettings != null) {
                     mSettings.showIcons.value = !mSettings.showIcons.value;
                  }

                  UISoundHelper.playSound(mSettings != null && mSettings.showIcons.value ? "ui.toggle_on" : "ui.toggle_off");
                  return true;
               }

               sY += 18;
               if (mouseX >= cardX + 12 && mouseX <= cardX + cardW - 12 && scrolledMouseY >= sY - 4 && scrolledMouseY <= sY + 12) {
                  int sliderW = cardW - 24;
                  this.draggingMarkerSlider = 1;
                  this.markerSliderDragX = cardX + 12;
                  this.markerSliderDragW = sliderW;
                  float pct = (float)(mouseX - (cardX + 12)) / sliderW;
                  pct = Math.max(0.0F, Math.min(1.0F, pct));
                  if (mSettings != null) {
                     mSettings.limitValue = 1.0 + pct * 99.0;
                  }

                  return true;
               }

               sY += 26;
               if (mouseX >= cardX + 10 && mouseX <= cardX + cardW - 10 && scrolledMouseY >= sY - 2 && scrolledMouseY <= sY + 14) {
                  if (mSettings != null) {
                     mSettings.showDistance.value = !mSettings.showDistance.value;
                  }

                  UISoundHelper.playSound(mSettings != null && mSettings.showDistance.value ? "ui.toggle_on" : "ui.toggle_off");
                  return true;
               }

               sY += 18;
               if (mouseX >= cardX + 12 && mouseX <= cardX + cardW - 12 && scrolledMouseY >= sY - 4 && scrolledMouseY <= sY + 12) {
                  int sliderW = cardW - 24;
                  this.draggingMarkerSlider = 2;
                  this.markerSliderDragX = cardX + 12;
                  this.markerSliderDragW = sliderW;
                  float pct = (float)(mouseX - (cardX + 12)) / sliderW;
                  pct = Math.max(0.0F, Math.min(1.0F, pct));
                  if (mSettings != null) {
                     mSettings.distance.value = 50.0 + pct * 4950.0;
                  }

                  return true;
               }
            }

            cardY += settingsCardH + 10;
            AutoMarkers autoMod = (AutoMarkers)ModuleManager.getInstance().getModule("AutoMarkers");
            int autoHeaderH = 38;
            int autoExpandedH = 110;
            int autoCardH = autoHeaderH + (this.autoMarkersExpanded ? autoExpandedH : 0);
            if (mouseX >= cardX && mouseX <= cardX + cardW && scrolledMouseY >= cardY && scrolledMouseY <= cardY + 38) {
               if (mouseX >= cardX + cardW - 60 && mouseX <= cardX + cardW - 30) {
                  if (autoMod != null) {
                     autoMod.toggle();
                  }

                  UISoundHelper.playSound(autoMod != null && autoMod.isEnabled() ? "ui.toggle_on" : "ui.toggle_off");
                  return true;
               } else {
                  this.autoMarkersExpanded = !this.autoMarkersExpanded;
                  UISoundHelper.playSound(this.autoMarkersExpanded ? "ui.module_expand" : "ui.module_collapse");
                  return true;
               }
            } else {
               cardY += autoCardH + 10;
               int createHeaderH = 38;
               int createExpandedH = 175;
               int createCardH = createHeaderH + (this.createMarkerExpanded ? createExpandedH : 0);
               if (mouseX >= cardX && mouseX <= cardX + cardW && scrolledMouseY >= cardY && scrolledMouseY <= cardY + 38) {
                  this.createMarkerExpanded = !this.createMarkerExpanded;
                  UISoundHelper.playSound(this.createMarkerExpanded ? "ui.module_expand" : "ui.module_collapse");
                  return true;
               }

               if (this.createMarkerExpanded) {
                  int cY = cardY + 44;
                  cY += 12;
                  if (mouseX >= cardX + 12 && mouseX <= cardX + cardW - 12 && scrolledMouseY >= cY && scrolledMouseY <= cY + 18) {
                     this.activeMarkerInput = 1;
                     if ("Точка".equals(this.newMarkerName) || "Метка".equals(this.newMarkerName)) {
                        this.newMarkerName = "";
                     }

                     UISoundHelper.playSound("ui.search_type");
                     return true;
                  }

                  cY += 24;
                  cY += 2;
                  if (mouseX >= cardX + cardW - 80 && mouseX <= cardX + cardW - 10 && scrolledMouseY >= cY && scrolledMouseY <= cY + 16) {
                     this.newMarkerColorIndex = (this.newMarkerColorIndex + 1) % MARKER_COLORS.length;
                     UISoundHelper.playSound("ui.color_pick");
                     return true;
                  }

                  cY += 20;
                  cY += 12;
                  int coordW = (cardW - 36) / 3;
                  if (mouseX >= cardX + 12 && mouseX <= cardX + 12 + coordW && scrolledMouseY >= cY && scrolledMouseY <= cY + 18) {
                     this.activeMarkerInput = 2;
                     if ("89".equals(this.newMarkerX)) {
                        this.newMarkerX = "";
                     }

                     UISoundHelper.playSound("ui.search_type");
                     return true;
                  }

                  if (mouseX >= cardX + 18 + coordW && mouseX <= cardX + 18 + coordW * 2 && scrolledMouseY >= cY && scrolledMouseY <= cY + 18) {
                     this.activeMarkerInput = 3;
                     if ("75".equals(this.newMarkerY)) {
                        this.newMarkerY = "";
                     }

                     UISoundHelper.playSound("ui.search_type");
                     return true;
                  }

                  if (mouseX >= cardX + 24 + coordW * 2 && mouseX <= cardX + 24 + coordW * 3 && scrolledMouseY >= cY && scrolledMouseY <= cY + 18) {
                     this.activeMarkerInput = 4;
                     if ("-140".equals(this.newMarkerZ)) {
                        this.newMarkerZ = "";
                     }

                     UISoundHelper.playSound("ui.search_type");
                     return true;
                  }

                  cY += 24;
                  cY += 12;

                  for (int ic = 0; ic < 8; ic++) {
                     int btnX = cardX + 12 + ic * 22;
                     if (mouseX >= btnX && mouseX <= btnX + 18 && scrolledMouseY >= cY && scrolledMouseY <= cY + 18) {
                        this.newMarkerIconIndex = ic;
                        UISoundHelper.playSound("ui.dropdown_select");
                        return true;
                     }
                  }

                  cY += 26;
                  if (mouseX >= cardX + 12 && mouseX <= cardX + 72 && scrolledMouseY >= cY && scrolledMouseY <= cY + 18) {
                     MinecraftClient mc = MinecraftClient.getInstance();
                     double px = mc.player != null ? Math.floor(mc.player.getX()) : 0.0;
                     double py = mc.player != null ? Math.floor(mc.player.getY()) : 64.0;
                     double pz = mc.player != null ? Math.floor(mc.player.getZ()) : 0.0;

                     try {
                        if (!this.newMarkerX.isEmpty()) {
                           px = Double.parseDouble(this.newMarkerX);
                        }
                     } catch (Exception var52) {
                     }

                     try {
                        if (!this.newMarkerY.isEmpty()) {
                           py = Double.parseDouble(this.newMarkerY);
                        }
                     } catch (Exception var51) {
                     }

                     try {
                        if (!this.newMarkerZ.isEmpty()) {
                           pz = Double.parseDouble(this.newMarkerZ);
                        }
                     } catch (Exception var50) {
                     }

                     String name = this.newMarkerName != null && !this.newMarkerName.trim().isEmpty() ? this.newMarkerName : "Метка";
                     WaypointManager.addWaypoint(
                        name, new Vec3d(px, py, pz), MARKER_COLORS[this.newMarkerColorIndex], this.newMarkerIconIndex, "Создана только что"
                     );
                     UISoundHelper.playSound("ui.notif_success");
                     return true;
                  }
               }

               cardY += createCardH + 15;
               List<WaypointManager.Waypoint> waypoints = WaypointManager.getWaypoints();
               int wpColW = (cardW - 12) / 2;
               int wpYLeft = cardY;
               int wpYRight = cardY;

               for (int i = 0; i < waypoints.size(); i++) {
                  WaypointManager.Waypoint wp = waypoints.get(i);
                  boolean isLeft = i % 2 == 0;
                  int wpX = cardX + (isLeft ? 0 : wpColW + 12);
                  int currentWpY = isLeft ? wpYLeft : wpYRight;
                  int wpCardH = wp.expandedInGui ? 62 : 44;
                  int eyeX = wpX + wpColW - 36;
                  int chevX = wpX + wpColW - 18;
                  if (mouseX >= eyeX - 4 && mouseX <= eyeX + 14 && scrolledMouseY >= currentWpY + 6 && scrolledMouseY <= currentWpY + 22) {
                     wp.visible = !wp.visible;
                     UISoundHelper.playSound(wp.visible ? "ui.toggle_on" : "ui.toggle_off");
                     return true;
                  }

                  if (mouseX >= chevX - 4 && mouseX <= chevX + 14 && scrolledMouseY >= currentWpY + 6 && scrolledMouseY <= currentWpY + 22) {
                     wp.expandedInGui = !wp.expandedInGui;
                     UISoundHelper.playSound("ui.dropdown_select");
                     return true;
                  }

                  if (wp.expandedInGui) {
                     int delBtnX = wpX + 10;
                     if (mouseX >= delBtnX && mouseX <= delBtnX + 50 && scrolledMouseY >= currentWpY + 42 && scrolledMouseY <= currentWpY + 56) {
                        WaypointManager.removeWaypoint(wp);
                        UISoundHelper.playSound("ui.favorite_remove");
                        return true;
                     }
                  }

                  if (isLeft) {
                     wpYLeft += wpCardH + 10;
                  } else {
                     wpYRight += wpCardH + 10;
                  }
               }

               return true;
            }
         } else if (this.selectedCategory == Category.FRIENDS) {
            int cardX = areaLeft + 14;
            int cardW = contentWidth - 28;
            Module friendSysMod = ModuleManager.getInstance().getModule("Настройки друзей");
            int settingsHeaderY = startY;
            if (mouseX >= cardX && mouseX <= cardX + cardW && scrolledMouseY >= settingsHeaderY && scrolledMouseY <= settingsHeaderY + 38) {
               if (friendSysMod != null) {
                  friendSysMod.setExpanded(!friendSysMod.isExpanded());
                  UISoundHelper.playSound(friendSysMod.isExpanded() ? "ui.module_expand" : "ui.module_collapse");
               }

               return true;
            } else {
               int friendSysSettingsH = friendSysMod != null && friendSysMod.isExpanded() ? this.getSettingsHeight(friendSysMod) : 0;
               int settingsCardH = 38 + friendSysSettingsH;
               int addCardY = startY + settingsCardH + 10;
               int inputX = cardX + 12;
               int inputY = addCardY + 48;
               int btnW = 70;
               int inputW = cardW - 32 - btnW;
               int btnX = cardX + cardW - 12 - btnW;
               if (mouseX >= inputX && mouseX <= inputX + inputW && scrolledMouseY >= inputY && scrolledMouseY <= inputY + 20) {
                  this.friendInputFocused = true;
                  UISoundHelper.playSound("ui.search_focus");
                  return true;
               }

               this.friendInputFocused = false;
               if (mouseX >= btnX && mouseX <= btnX + btnW && scrolledMouseY >= inputY && scrolledMouseY <= inputY + 20) {
                  if (!this.friendInputQuery.trim().isEmpty()) {
                     FriendManager.addFriend(this.friendInputQuery.trim());
                     this.friendInputQuery = "";
                     UISoundHelper.playSound("ui.favorite_add");
                  }

                  return true;
               } else {
                  int friendListY = addCardY + 75 + 15;

                  for (FriendManager.Friend f : FriendManager.getFriends()) {
                     int fCardW = 180;
                     int fCardH = 46;
                     int delX = cardX + fCardW - 22;
                     int delY = friendListY + 16;
                     if (mouseX >= delX - 4 && mouseX <= delX + 16 && scrolledMouseY >= delY - 4 && scrolledMouseY <= delY + 16) {
                        FriendManager.removeFriend(f.name);
                        UISoundHelper.playSound("ui.favorite_remove");
                        return true;
                     }

                     friendListY += fCardH + 8;
                  }

                  return true;
               }
            }
         } else {
            List<Module> categoryModules = ModuleManager.getInstance().searchModules(this.selectedCategory, this.searchQuery);
            int cardWidth = 171;
            int baseCardHeight = 41;
            int currentYLeft = startY;
            int currentYRight = startY;

            for (int i = 0; i < categoryModules.size(); i++) {
               Module module = categoryModules.get(i);
               int settingsHeight = this.getSettingsHeight(module);
               int cardHeight = baseCardHeight + (int)(settingsHeight * module.getExpandProgress());
               boolean isLeftCol = currentYLeft <= currentYRight;
               int moduleLeft = (int)(areaLeft + 14.5F + (isLeftCol ? 0.0F : cardWidth + 17.5F));
               int currentY = isLeftCol ? currentYLeft : currentYRight;
               if (mouseX >= moduleLeft && mouseX <= moduleLeft + cardWidth && scrolledMouseY >= currentY && scrolledMouseY <= currentY + cardHeight) {
                  if (scrolledMouseY <= currentY + baseCardHeight) {
                     int rightEdgeMargin = module.hasDropdown() ? 19 : 12;
                     int switchX = moduleLeft + cardWidth - rightEdgeMargin - 16;
                      String tagText = this.bindingModule == module ? "..." : module.getTag();
                      int tagTextWidth = FontManager.getSubFont().getStringWidth(tagText);
                      int tagBoxWidth = 34;
                      int tagX = switchX - tagBoxWidth - 4;
                     int starX = tagX - 11;
                     int arrowX = moduleLeft + cardWidth - 14;
                     if ("Создание метки".equals(module.getName())) {
                        int createBtnX = moduleLeft + 10;
                        int createBtnY = currentY + 36;
                        int createBtnW = 50;
                        int createBtnH = 16;
                        if (mouseX >= createBtnX
                           && mouseX <= createBtnX + createBtnW
                           && scrolledMouseY >= createBtnY
                           && scrolledMouseY <= createBtnY + createBtnH) {
                           CreateMarker.onClickCreate();
                           UISoundHelper.playSound("ui.click_confirm");
                           return true;
                        }
                     }

                     if (mouseX >= tagX && mouseX <= tagX + tagBoxWidth) {
                        this.bindingModule = this.bindingModule == module ? null : module;
                        UISoundHelper.playSound("ui.keybind_listen");
                        return true;
                     }

                     if (mouseX >= starX - 4 && mouseX <= starX + 14) {
                        if (button == 0) {
                           module.setFavorite(!module.isFavorite());
                           UISoundHelper.playSound(module.isFavorite() ? "ui.favorite_add" : "ui.favorite_remove");
                        }
                     } else if (mouseX >= switchX && mouseX <= switchX + 19 && button == 0) {
                        module.toggle();
                        UISoundHelper.playSound(module.isEnabled() ? "ui.toggle_on" : "ui.toggle_off");
                     } else if (mouseX >= arrowX - 6 && mouseX <= arrowX + 10 && module.hasDropdown()) {
                        module.setExpanded(!module.isExpanded());
                        UISoundHelper.playSound(module.isExpanded() ? "ui.module_expand" : "ui.module_collapse");
                     } else if (button == 1 && module.hasDropdown()) {
                        module.setExpanded(!module.isExpanded());
                        UISoundHelper.playSound(module.isExpanded() ? "ui.module_expand" : "ui.module_collapse");
                     }

                     return true;
                  }

                  if (module.isExpanded()) {
                     if (module instanceof Crosshair) {
                        int btnX = moduleLeft + 10;
                        int btnY = currentY + 42;
                        int btnW = cardWidth - 20;
                        int btnH = 18;
                        if (mouseX >= btnX && mouseX <= btnX + btnW && scrolledMouseY >= btnY && scrolledMouseY <= btnY + btnH) {
                           this.client.setScreen(new CrosshairEditorScreen(this, (Crosshair)module));
                           UISoundHelper.playSound("ui.editor_enter");
                           return true;
                        }
                     } else if (module instanceof CustomHand) {
                        int btnX = moduleLeft + 10;
                        int btnY = currentY + 42;
                        int btnW = cardWidth - 20;
                        int btnH = 18;
                        if (mouseX >= btnX && mouseX <= btnX + btnW && scrolledMouseY >= btnY && scrolledMouseY <= btnY + btnH) {
                           this.client.setScreen(new CustomHandEditorScreen(this, (CustomHand)module));
                           UISoundHelper.playSound("ui.editor_enter");
                           return true;
                        }
                     } else {
                        int settingY = currentY + 42;

                        for (Module.Setting setting : module.getSettings()) {
                           if (setting.isVisible()) {
                              if (setting instanceof Module.BooleanSetting) {
                                 Module.BooleanSetting bool = (Module.BooleanSetting)setting;
                                 if (scrolledMouseY >= settingY && scrolledMouseY <= settingY + 18) {
                                    bool.value = !bool.value;
                                    UISoundHelper.playSound(bool.value ? "ui.toggle_on" : "ui.toggle_off");
                                    return true;
                                 }

                                 settingY += 18;
                                  if ("Цвет клиента".equalsIgnoreCase(bool.name) && !bool.value) {
                                     if (scrolledMouseY >= settingY && scrolledMouseY <= settingY + 18) {
                                        this.activeColorPicker = "custom";
                                        this.colorEditModule = module;
                                        this.syncPickerFromColor(module.customColor);
                                        this.pickerOpen = true;
                                        UISoundHelper.playSound("ui.color_pick");
                                        return true;
                                     }

                                    settingY += 18;
                                 }
                              } else if (setting instanceof Module.NumberSetting) {
                                 if (scrolledMouseY >= settingY + 2 && scrolledMouseY <= settingY + 26) {
                                    Module.NumberSetting num;
                                    this.draggingNumberSetting = num = (Module.NumberSetting)setting;
                                    this.draggingModuleLeft = moduleLeft;
                                    this.draggingCardWidth = cardWidth;
                                    float ratio = (float)(mouseX - (moduleLeft + 10)) / (cardWidth - 20);
                                    ratio = Math.max(0.0F, Math.min(1.0F, ratio));
                                    double val = num.min + ratio * (num.max - num.min);
                                    num.value = Math.round(val / num.inc) * num.inc;
                                    UISoundHelper.playSound("ui.slider_grab");
                                    return true;
                                 }

                                 settingY += 24;
                              } else if (setting instanceof Module.ModeSetting) {
                                 Module.ModeSetting mode = (Module.ModeSetting)setting;
                                 if (mode.open) {
                                    int dropH = mode.modes.length * 20;
                                    if (scrolledMouseY >= settingY + 29 && scrolledMouseY <= settingY + 29 + dropH) {
                                       int index = (int)((scrolledMouseY - (settingY + 29)) / 20.0);
                                       if (index >= 0 && index < mode.modes.length) {
                                          mode.value = mode.modes[index];
                                          UISoundHelper.playSound("ui.dropdown_select");
                                       }

                                       mode.open = false;
                                       return true;
                                    }
                                 }

                                  if (scrolledMouseY >= settingY - 2 && scrolledMouseY <= settingY + 27) {
                                     mode.open = !mode.open;
                                    UISoundHelper.playSound(mode.open ? "ui.dropdown_open" : "ui.dropdown_close");
                                    return true;
                                 }

                                 settingY += 30 + (mode.open ? mode.modes.length * 20 + 8 : 0);
                              } else if (setting instanceof Module.MultiSelectSetting) {
                                 Module.MultiSelectSetting multi = (Module.MultiSelectSetting)setting;
                                 if (multi.open) {
                                    int dropH = multi.options.length * 20;
                                    if (scrolledMouseY >= settingY + 29 && scrolledMouseY <= settingY + 29 + dropH) {
                                       int index = (int)((scrolledMouseY - (settingY + 29)) / 20.0);
                                       if (index >= 0 && index < multi.options.length) {
                                          multi.toggle(multi.options[index]);
                                          UISoundHelper.playSound("ui.dropdown_select");
                                       }

                                       return true;
                                    }
                                 }

                                  if (scrolledMouseY >= settingY - 2 && scrolledMouseY <= settingY + 27) {
                                     multi.open = !multi.open;
                                    UISoundHelper.playSound(multi.open ? "ui.dropdown_open" : "ui.dropdown_close");
                                    return true;
                                 }

                                 settingY += 30 + (multi.open ? multi.options.length * 20 + 8 : 0);
                              } else if (setting instanceof Module.TextSetting) {
                                 Module.TextSetting txt = (Module.TextSetting)setting;
                                 if (scrolledMouseY >= settingY + 11 && scrolledMouseY <= settingY + 27) {
                                    txt.focused = !txt.focused;
                                    UISoundHelper.playSound("ui.search_focus");
                                    return true;
                                 }

                                 txt.focused = false;
                                 settingY += 30;
                              } else if (setting instanceof Module.KeySetting) {
                                 Module.KeySetting keySet = (Module.KeySetting)setting;
                                 if (scrolledMouseY >= settingY - 2 && scrolledMouseY <= settingY + 18) {
                                    keySet.listening = !keySet.listening;
                                    UISoundHelper.playSound("ui.keybind_listen");
                                    return true;
                                 }

                                 keySet.listening = false;
                                 settingY += 18;
                              }
                           }
                        }
                     }

                     return true;
                  }
               }

               if (isLeftCol) {
                  currentYLeft += cardHeight + 14;
               } else {
                  currentYRight += cardHeight + 14;
               }
            }

            return super.mouseClicked(click, doubled);
         }
      }
   }

   public boolean mouseDragged(net.minecraft.client.gui.Click click, double deltaX, double deltaY) {
      double mouseX = click.x(); double mouseY = click.y(); int button = click.button();
      if (!this.showingDragmode) {
         float openScale = this.openAnim.getValue();
         float openSlide = this.openYAnim.getValue();
         if (openScale < 0.999F || openSlide > 0.5F) {
            float cx = this.width / 2.0F;
            float cy = this.height / 2.0F;
            double my = mouseY - openSlide;
            mouseX = (mouseX - cx) / openScale + cx;
            mouseY = (my - cy) / openScale + cy;
         }

         float dragScale = this.draggingClientSlider == 1 ? this.clientSliderStartScale : uiScale;
         if (dragScale != 1.0F && dragScale > 0.1F) {
            mouseX = (mouseX - (this.guiLeft + 242.0F)) / dragScale + (this.guiLeft + 242.0F);
            mouseY = (mouseY - (this.guiTop + 140.0F)) / dragScale + (this.guiTop + 140.0F);
         }
      }

      if (this.pickerOpen && this.pickerDragging != 0) {
         this.updatePickerFromMouse(mouseX, mouseY);
         return true;
      }

      if (this.showingDragmode && this.draggingHUDElement != null) {
          this.draggingHUDElement.x = Math.round((float)mouseX - this.dragOffsetX);
          this.draggingHUDElement.y = Math.round((float)mouseY - this.dragOffsetY);
         return true;
      }

      if (this.draggingClientSlider == 1 && this.clientSliderW > 0) {
         float ratio = (float)(mouseX - this.clientSliderX) / this.clientSliderW;
         ratio = Math.max(0.0F, Math.min(1.0F, ratio));
         this.clientSliderDragRatio = ratio;
         float newScale = Math.round((0.5F + ratio * 1.5F) * 10.0F) / 10.0F;
         if (newScale != uiScale) {
            uiScale = newScale;
            UISoundHelper.playSound("ui.slider_tick", 1.0F, 0.5F);
         }

         return true;
      } else if (this.draggingClientSlider == 2 && this.clientSliderW > 0) {
         float ratio = (float)(mouseX - this.clientSliderX) / this.clientSliderW;
         ratio = Math.max(0.0F, Math.min(1.0F, ratio));
         this.clientSliderDragRatio = ratio;
         float newVol = Math.round(ratio * 100.0F);
         if (newVol != soundVolume) {
            soundVolume = newVol;
            UISoundHelper.playSound("ui.slider_tick", 1.0F, 0.5F);
         }

         return true;
      } else if (this.draggingMarkerSlider == 1 && this.markerSliderDragW > 0) {
         MarkerSettings mSettings = (MarkerSettings)ModuleManager.getInstance().getModule("MarkerSettings");
         float pct = (float)(mouseX - this.markerSliderDragX) / this.markerSliderDragW;
         pct = Math.max(0.0F, Math.min(1.0F, pct));
         if (mSettings != null) {
            double newVal = 1.0 + pct * 99.0;
            if (newVal != mSettings.limitValue) {
               mSettings.limitValue = newVal;
               UISoundHelper.playSound("ui.slider_tick", 1.0F, 0.5F);
            }
         }

         return true;
      } else if (this.draggingMarkerSlider == 2 && this.markerSliderDragW > 0) {
         MarkerSettings mSettings = (MarkerSettings)ModuleManager.getInstance().getModule("MarkerSettings");
         float pct = (float)(mouseX - this.markerSliderDragX) / this.markerSliderDragW;
         pct = Math.max(0.0F, Math.min(1.0F, pct));
         if (mSettings != null) {
            double newVal = 50.0 + pct * 4950.0;
            if (newVal != mSettings.distance.value) {
               mSettings.distance.value = newVal;
               UISoundHelper.playSound("ui.slider_tick", 1.0F, 0.5F);
            }
         }

         return true;
      } else if (this.draggingNumberSetting != null && this.draggingModuleLeft > 0 && this.draggingCardWidth > 0) {
         float ratio = (float)(mouseX - (this.draggingModuleLeft + 10)) / (this.draggingCardWidth - 20);
         ratio = Math.max(0.0F, Math.min(1.0F, ratio));
         double val = this.draggingNumberSetting.min + ratio * (this.draggingNumberSetting.max - this.draggingNumberSetting.min);
         if ((val = Math.round(val / this.draggingNumberSetting.inc) * this.draggingNumberSetting.inc) != this.draggingNumberSetting.value) {
            this.draggingNumberSetting.value = val;
            UISoundHelper.playSound("ui.slider_tick", 1.0F, 0.5F);
         }

         return true;
      } else {
         return super.mouseDragged(click, deltaX, deltaY);
      }
   }

   public boolean mouseReleased(net.minecraft.client.gui.Click click) {
      double mouseX = click.x(); double mouseY = click.y(); int button = click.button();
      if (this.pickerDragging != 0) {
         this.pickerDragging = 0;
         UISoundHelper.playSound("ui.slider_release");
         return true;
      } else if (this.draggingHUDElement != null) {
         this.draggingHUDElement = null;
         UISoundHelper.playSound("ui.hud_drop");
         return true;
      } else if (this.draggingClientSlider != 0) {
         this.draggingClientSlider = 0;
         this.clientSliderDragRatio = -1.0F;
         UISoundHelper.playSound("ui.slider_release");
         return true;
      } else if (this.draggingMarkerSlider != 0) {
         this.draggingMarkerSlider = 0;
         UISoundHelper.playSound("ui.slider_release");
         return true;
      } else if (this.draggingNumberSetting != null) {
         this.draggingNumberSetting = null;
         UISoundHelper.playSound("ui.slider_release");
         return true;
      } else {
         return super.mouseReleased(click);
      }
   }

   private Module.TextSetting getFocusedTextSetting() {
      for (Module m : ModuleManager.getInstance().getModules()) {
         if (m.isExpanded()) {
            for (Module.Setting s : m.getSettings()) {
               if (s instanceof Module.TextSetting && ((Module.TextSetting)s).focused) {
                  return (Module.TextSetting)s;
               }
            }
         }
      }

      return null;
   }

   private Module.KeySetting getListeningKeySetting() {
      for (Module m : ModuleManager.getInstance().getModules()) {
         if (m.isExpanded()) {
            for (Module.Setting s : m.getSettings()) {
               if (s instanceof Module.KeySetting && ((Module.KeySetting)s).listening) {
                  return (Module.KeySetting)s;
               }
            }
         }
      }

      return null;
   }

   public boolean charTyped(net.minecraft.client.input.CharInput input) {
      char chr = (char) input.codepoint(); int modifiers = input.modifiers();
      if (this.selectedCategory == Category.MARKERS && this.activeMarkerInput > 0) {
         if (this.activeMarkerInput == 1) {
            this.newMarkerName = this.newMarkerName + chr;
         } else if (this.activeMarkerInput == 2) {
            if (Character.isDigit(chr) || chr == '-') {
               this.newMarkerX = this.newMarkerX + chr;
            }
         } else if (this.activeMarkerInput == 3) {
            if (Character.isDigit(chr) || chr == '-') {
               this.newMarkerY = this.newMarkerY + chr;
            }
         } else if (this.activeMarkerInput == 4 && (Character.isDigit(chr) || chr == '-')) {
            this.newMarkerZ = this.newMarkerZ + chr;
         }

         return true;
      } else {
         Module.TextSetting focusedTxt = this.getFocusedTextSetting();
         if (focusedTxt != null) {
            focusedTxt.value = focusedTxt.value + chr;
            UISoundHelper.playSound("ui.search_type");
            return true;
         } else if (this.selectedCategory == Category.FRIENDS && this.friendInputFocused) {
            this.friendInputQuery = this.friendInputQuery + chr;
            UISoundHelper.playSound("ui.search_type");
            return true;
         } else if (this.bindingModule == null && !this.bindingMenuKey && this.getListeningKeySetting() == null) {
            this.searchQuery = this.searchQuery + chr;
            UISoundHelper.playSound("ui.search_type");
            return true;
         } else {
            return super.charTyped(input);
         }
      }
   }

   public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
      int keyCode = input.key(); int scanCode = input.scancode(); int modifiers = input.modifiers();
      if (this.selectedCategory == Category.MARKERS && this.activeMarkerInput > 0) {
         if (keyCode == 259) {
            if (this.activeMarkerInput == 1 && !this.newMarkerName.isEmpty()) {
               this.newMarkerName = this.newMarkerName.substring(0, this.newMarkerName.length() - 1);
            } else if (this.activeMarkerInput == 2 && !this.newMarkerX.isEmpty()) {
               this.newMarkerX = this.newMarkerX.substring(0, this.newMarkerX.length() - 1);
            } else if (this.activeMarkerInput == 3 && !this.newMarkerY.isEmpty()) {
               this.newMarkerY = this.newMarkerY.substring(0, this.newMarkerY.length() - 1);
            } else if (this.activeMarkerInput == 4 && !this.newMarkerZ.isEmpty()) {
               this.newMarkerZ = this.newMarkerZ.substring(0, this.newMarkerZ.length() - 1);
            }

            return true;
         }

         if (keyCode == 257 || keyCode == 256) {
            this.activeMarkerInput = 0;
            return true;
         }
      }

      Module.KeySetting listeningKey = this.getListeningKeySetting();
      if (listeningKey != null) {
         if (keyCode != 256 && keyCode != 259 && keyCode != 261) {
            listeningKey.setKey(keyCode);
         } else {
            listeningKey.setKey(0);
         }

         listeningKey.listening = false;
         UISoundHelper.playSound("ui.keybind_set");
         return true;
      } else {
         Module.TextSetting focusedTxt = this.getFocusedTextSetting();
         if (focusedTxt != null) {
            if (keyCode == 259 && !focusedTxt.value.isEmpty()) {
               focusedTxt.value = focusedTxt.value.substring(0, focusedTxt.value.length() - 1);
               UISoundHelper.playSound("ui.search_type");
               return true;
            } else if (keyCode == 257) {
               focusedTxt.focused = false;
               UISoundHelper.playSound("ui.click_confirm");
               return true;
            } else {
               return true;
            }
         } else if (this.selectedCategory == Category.FRIENDS && this.friendInputFocused) {
            if (keyCode == 259 && !this.friendInputQuery.isEmpty()) {
               this.friendInputQuery = this.friendInputQuery.substring(0, this.friendInputQuery.length() - 1);
               UISoundHelper.playSound("ui.search_type");
               return true;
            } else if (keyCode == 257 && !this.friendInputQuery.trim().isEmpty()) {
               FriendManager.addFriend(this.friendInputQuery.trim());
               this.friendInputQuery = "";
               UISoundHelper.playSound("ui.favorite_add");
               return true;
            } else {
               return true;
            }
          } else if (this.bindingMenuKey) {
            if (keyCode == 256) {
               menuKeyCode = 344;
               menuKeyName = "RSHFT";
            } else {
               menuKeyCode = keyCode;
               menuKeyName = KeyUtils.getShortKeyName(keyCode);
            }

            this.bindingMenuKey = false;
            UISoundHelper.playSound("ui.keybind_set");
            return true;
         } else if (this.bindingModule == null) {
            if (keyCode == 259 && !this.searchQuery.isEmpty()) {
               this.searchQuery = this.searchQuery.substring(0, this.searchQuery.length() - 1);
               UISoundHelper.playSound("ui.search_type");
               return true;
            } else {
               return super.keyPressed(input);
            }
         } else {
            if (keyCode != 256 && keyCode != 259 && keyCode != 261) {
               this.bindingModule.setKey(keyCode);
            } else {
               this.bindingModule.setKey(0);
            }

            this.bindingModule = null;
            UISoundHelper.playSound("ui.keybind_set");
            return true;
         }
      }
   }

   public void close() {
      ConfigManager.saveConfig();
      super.close();
   }

   public boolean isPauseScreen() {
      return false;
   }

   private void renderColorPickerPopup(DrawContext context, int mouseX, int mouseY) {
      int popupW = 172;
      int popupH = 186;
      int popupX = this.colorPickerX();
      int popupY = this.colorPickerY();
      RenderUtils.drawSingleRoundedRect(context, popupX - 1, popupY - 1, popupW + 2, popupH + 2, 10.0F, accentColor);
      RenderUtils.drawRoundedRect(context, popupX, popupY, popupW, popupH, 10.0F, -15461353);
      CustomFont mainFont = FontManager.getMainFont();
      CustomFont subFont = FontManager.getSubFont();
      mainFont.drawString(context, "Выбор цвета", popupX + 14, popupY + 10, -1);
      String targetName = "accent".equals(this.activeColorPicker) ? "Акцентный цвет" : ("client".equals(this.activeColorPicker) ? "Цвет клиента" : "Кастомный цвет");
      subFont.drawString(context, targetName, popupX + 14, popupY + 25, -8750459);
      RenderUtils.drawRect(context, popupX + 10, popupY + 37, popupW - 20, 1.0F, -14935006);
      int svX = popupX + 12;
      int svY = popupY + 46;
      int svW = popupW - 24;
      int svH = 88;
      int currentColor = "accent".equals(this.activeColorPicker) ? accentColor : ("client".equals(this.activeColorPicker) ? clientColor : (this.colorEditModule != null ? this.colorEditModule.customColor : -1));
      RenderUtils.drawSingleRoundedRect(context, svX - 1.5F, svY - 1.5F, svW + 3.0F, svH + 3.0F, 6.0F, -14079702);
      RenderUtils.drawColorBox(context, svX, svY, svW, svH, this.pickerHue, 6.0F);
      int indX = svX + (int)(svW * this.pickerSat);
      int indY = svY + (int)(svH * (1.0F - this.pickerVal));
      RenderUtils.drawCircle(context, indX, indY, 4.5F, -16777216);
      RenderUtils.drawCircle(context, indX, indY, 3.5F, -1);
      RenderUtils.drawCircle(context, indX, indY, 1.5F, currentColor);
      int hueY = popupY + 148;
      int hueW = svW;
      float hueH = 8.0F;
      float hueCenterY = hueY + hueH / 2.0F;
      RenderUtils.drawSingleRoundedRect(context, svX - 1.5F, hueY - 1.5F, hueW + 3.0F, hueH + 3.0F, 6.0F, -14079702);
      RenderUtils.drawHueBar(context, svX, hueY, hueW, hueH, 4.0F);
      int thumbX = svX + (int)(hueW * this.pickerHue);
      RenderUtils.drawCircle(context, thumbX, hueCenterY, 5.5F, -16777216);
      RenderUtils.drawCircle(context, thumbX, hueCenterY, 4.0F, -1);
      String hexStr = String.format("#%06X", currentColor & 16777215);
      RenderUtils.drawSingleRoundedRect(context, popupX + 12, popupY + 164, 18.0F, 18.0F, 5.0F, -14079702);
      RenderUtils.drawSingleRoundedRect(context, popupX + 13, popupY + 165, 16.0F, 16.0F, 4.5F, currentColor);
      subFont.drawString(context, hexStr, popupX + 38, popupY + 170, -1);
   }

   private int colorPickerX() {
      return this.guiLeft + 484 - 172 - 10;
   }

   private int colorPickerY() {
      return this.guiTop + 36;
   }

   private int pickerSvX() {
      return this.colorPickerX() + 12;
   }

   private int pickerSvY() {
      return this.colorPickerY() + 46;
   }

   private int pickerSvW() {
      return 172 - 24;
   }

   private int pickerSvH() {
      return 88;
   }

   private int pickerHueY() {
      return this.colorPickerY() + 148;
   }

   private int pickerHueH() {
      return 8;
   }

   private void syncPickerFromColor(int color) {
      float[] hsv = Color.RGBtoHSB((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, null);
      this.pickerHue = hsv[0];
      this.pickerSat = hsv[1];
      this.pickerVal = hsv[2];
   }

   private void applyPickerColor() {
      int color = 0xFF000000 | (Color.HSBtoRGB(this.pickerHue, this.pickerSat, this.pickerVal) & 0xFFFFFF);
      if ("accent".equals(this.activeColorPicker)) {
         accentColor = color;
      } else if ("client".equals(this.activeColorPicker)) {
         clientColor = color;
      } else {
         if (this.colorEditModule != null) {
            this.colorEditModule.customColor = color;
         }
      }
   }

   private void updatePickerFromMouse(double mouseX, double mouseY) {
      int svX = this.pickerSvX();
      int svY = this.pickerSvY();
      int svW = this.pickerSvW();
      int svH = this.pickerSvH();
      if (this.pickerDragging == 1) {
         this.pickerSat = (float)Math.max(0.0, Math.min(1.0, (mouseX - svX) / svW));
         this.pickerVal = (float)Math.max(0.0, Math.min(1.0, 1.0 - (mouseY - svY) / svH));
      } else if (this.pickerDragging == 2) {
         this.pickerHue = (float)Math.max(0.0, Math.min(1.0, (mouseX - svX) / svW));
      }

      this.applyPickerColor();
   }
}
