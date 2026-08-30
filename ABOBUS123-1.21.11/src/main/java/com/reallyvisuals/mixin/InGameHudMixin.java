package com.reallyvisuals.mixin;

import com.reallyvisuals.gui.HUDManager;
import com.reallyvisuals.gui.ReallyVisualsScreen;
import com.reallyvisuals.gui.render.CrosshairRenderUtils;
import com.reallyvisuals.module.Animations;
import com.reallyvisuals.module.ArmorHud;
import com.reallyvisuals.module.BetterNear;
import com.reallyvisuals.module.ClickPearl;
import com.reallyvisuals.module.Cooldowns;
import com.reallyvisuals.module.HitMarker;
import com.reallyvisuals.module.ComboCounter;
import com.reallyvisuals.module.CpsCounter;
import com.reallyvisuals.module.SpeedHud;
import com.reallyvisuals.module.CoordinatesHud;
import com.reallyvisuals.module.Crosshair;
import com.reallyvisuals.module.Hotkeys;
import com.reallyvisuals.module.InventoryHud;
import com.reallyvisuals.module.Keystrokes;
import com.reallyvisuals.module.PerformanceHud;
import com.reallyvisuals.module.ModuleManager;
import com.reallyvisuals.module.Potions;
import com.reallyvisuals.module.RenderTweaks;
import com.reallyvisuals.module.TargetHud;
import com.reallyvisuals.module.Watermark;
import com.reallyvisuals.utils.AnimationUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import com.reallyvisuals.gui.render.RenderUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
   @Shadow
   private int scaledWidth;
   @Shadow
   private int scaledHeight;
   @Unique
   private static AnimationUtils.Animation hotbarAnimation = new AnimationUtils.Animation(0.0F);
   @Unique
   private int lastSlot = -1;

   @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
   private void onRenderCrosshair(DrawContext context, RenderTickCounter counter, CallbackInfo ci) {
      Crosshair crosshair = (Crosshair)ModuleManager.getInstance().getModule("Crosshair");
      if (crosshair != null && crosshair.isEnabled()) {
         MinecraftClient client = MinecraftClient.getInstance();
         float centerX = client.getWindow().getScaledWidth() / 2.0F;
         float centerY = client.getWindow().getScaledHeight() / 2.0F;
         CrosshairRenderUtils.renderCrosshair(context, crosshair, centerX, centerY);
         ci.cancel();
      }
   }

   @Inject(method = "renderHotbar", at = @At("HEAD"))
   private void onRenderHotbarHead(DrawContext context, RenderTickCounter counter, CallbackInfo ci) {
      float tickDelta = counter.getTickProgress(false);
      Animations anim = (Animations)ModuleManager.getInstance().getModule("Animations");
      if (anim != null && anim.isEnabled() && anim.hotbarAnim.value) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client.player != null) {
            int selectedSlot = client.player.getInventory().getSelectedSlot();
            float targetX = this.scaledWidth / 2 - 91 - 1 + selectedSlot * 20;
            if (this.lastSlot == -1) {
               hotbarAnimation.force(targetX);
            } else if (this.lastSlot != selectedSlot) {
               hotbarAnimation.animateTo(targetX, (long)anim.hotbarDuration.value);
            }

            this.lastSlot = selectedSlot;
         }
      } else {
         this.lastSlot = -1;
      }
   }

   /**
    * 1.21.11: InGameHud.drawTexture is gone and renderHotbar now calls one of several
    * DrawContext overloads, so redirecting a specific invocation is fragile. The
    * selection highlight is drawn here instead, keeping the animation logic intact.
    */
   @Inject(method = "renderHotbar", at = @At("TAIL"))
   private void onHotbarSelection(DrawContext context, RenderTickCounter counter, CallbackInfo ci) {
      Animations anim = (Animations) ModuleManager.getInstance().getModule("Animations");
      if (anim == null || !anim.isEnabled() || !anim.hotbarAnim.value) {
         return;
      }
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player == null) {
         return;
      }
      int slot = mc.player.getInventory().getSelectedSlot();
      int centerX = context.getScaledWindowWidth() / 2;
      int x = centerX - 91 + slot * 20;
      int y = context.getScaledWindowHeight() - 22;
      RenderUtils.drawSingleRoundedRect(context, x - 1.0F, y - 1.0F, 24.0F, 24.0F, 3.0F, 0x66FFFFFF);
   }

   private float sbShiftX = 0.0F;
   private float sbShiftY = 0.0F;

   @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V", at = @At("HEAD"), cancellable = true, require = 0)
   private void onRenderScoreboardSidebar(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
      RenderTweaks tweaks = (RenderTweaks)ModuleManager.getInstance().getModule("Render Tweaks");
      if (tweaks != null && tweaks.isEnabled() && tweaks.tweaks.isSelected("Скорборд")) {
         ci.cancel();
      } else {
         float dx = HUDManager.scoreboard.x - HUDManager.scoreboard.defaultX;
         float dy = HUDManager.scoreboard.y - HUDManager.scoreboard.defaultY;
         float maxShiftX = Math.max(0.0F, this.scaledWidth - 40.0F);
         float maxShiftY = Math.max(0.0F, this.scaledHeight - 40.0F);
         dx = Math.max(-maxShiftX, Math.min(dx, 0.0F));
         dy = Math.max(-maxShiftY, Math.min(dy, maxShiftY));
         this.sbShiftX = dx;
         this.sbShiftY = dy;
         if (dx != 0.0F || dy != 0.0F) {
            context.getMatrices().pushMatrix();
            context.getMatrices().translate((float) ((float)(dx)), (float) ((float)(dy)));
         }
      }
   }

   @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V", at = @At("RETURN"), require = 0)
   private void onRenderScoreboardSidebarEnd(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
      if (this.sbShiftX != 0.0F || this.sbShiftY != 0.0F) {
         context.getMatrices().popMatrix();
         this.sbShiftX = 0.0F;
         this.sbShiftY = 0.0F;
      }
   }

   @Inject(method = "render", at = @At("TAIL"))
   private void onRenderHUDTail(DrawContext context, RenderTickCounter counter, CallbackInfo ci) {
      float tickDelta = counter.getTickProgress(false);
      HUDManager.clampAll(this.scaledWidth, this.scaledHeight);

      ClickPearl clickPearl = (ClickPearl)ModuleManager.getInstance().getModule("Click Pearl");
      if (clickPearl != null && clickPearl.isEnabled()) {
         clickPearl.renderHotbarKeybind(context, this.scaledWidth, this.scaledHeight);
      }

      Watermark watermark;
      if ((watermark = (Watermark)ModuleManager.getInstance().getModule("Watermark")) != null && watermark.isEnabled()) {
         watermark.onRenderHUD(context);
      }

      MinecraftClient client = MinecraftClient.getInstance();
      boolean isMenuOpen = client.currentScreen instanceof ReallyVisualsScreen;
      Hotkeys hotkeys;
      if (!isMenuOpen && (hotkeys = (Hotkeys)ModuleManager.getInstance().getModule("Hot Keys")) != null && hotkeys.isEnabled()) {
         hotkeys.onRenderHUD(context);
      }

      ArmorHud armorHud;
      if ((armorHud = (ArmorHud)ModuleManager.getInstance().getModule("Armor HUD")) != null && armorHud.isEnabled()) {
         armorHud.onRenderHUD(context);
      }

      Cooldowns cooldowns;
      if (!isMenuOpen && (cooldowns = (Cooldowns)ModuleManager.getInstance().getModule("Cooldowns")) != null && cooldowns.isEnabled()) {
         cooldowns.onRenderHUD(context);
      }

      InventoryHud invHud;
      if ((invHud = (InventoryHud)ModuleManager.getInstance().getModule("Inventory HUD")) != null && invHud.isEnabled()) {
         invHud.onRenderHUD(context);
      }

      Potions potions;
      if (!isMenuOpen && (potions = (Potions)ModuleManager.getInstance().getModule("Potions")) != null && potions.isEnabled()) {
         potions.onRenderHUD(context);
      }

      TargetHud targetHud;
      if ((targetHud = (TargetHud)ModuleManager.getInstance().getModule("Target HUD")) != null && targetHud.isEnabled()) {
         targetHud.onRenderHUD(context);
      }

      BetterNear betterNear;
      if (!isMenuOpen && (betterNear = (BetterNear)ModuleManager.getInstance().getModule("Better Near")) != null && betterNear.isEnabled()) {
         betterNear.onRenderHUD(context);
      }

      Keystrokes keystrokes;
      if ((keystrokes = (Keystrokes)ModuleManager.getInstance().getModule("Keystrokes")) != null && keystrokes.isEnabled()) {
         keystrokes.onRenderHUD(context);
      }

      PerformanceHud performanceHud;
      if ((performanceHud = (PerformanceHud)ModuleManager.getInstance().getModule("Performance HUD")) != null && performanceHud.isEnabled()) {
         performanceHud.onRenderHUD(context);
      }

      CoordinatesHud coordinatesHud;
      if ((coordinatesHud = (CoordinatesHud)ModuleManager.getInstance().getModule("Coordinates HUD")) != null && coordinatesHud.isEnabled()) {
         coordinatesHud.onRenderHUD(context);
      }

      SpeedHud speedHud;
      if ((speedHud = (SpeedHud)ModuleManager.getInstance().getModule("Speed HUD")) != null && speedHud.isEnabled()) {
         speedHud.onRenderHUD(context);
      }

      CpsCounter cpsCounter;
      if ((cpsCounter = (CpsCounter)ModuleManager.getInstance().getModule("CPS Counter")) != null && cpsCounter.isEnabled()) {
         cpsCounter.onRenderHUD(context);
      }

      ComboCounter comboCounter;
      if ((comboCounter = (ComboCounter)ModuleManager.getInstance().getModule("Combo Counter")) != null && comboCounter.isEnabled()) {
         comboCounter.onRenderHUD(context);
      }

      HitMarker hitMarker;
      if ((hitMarker = (HitMarker)ModuleManager.getInstance().getModule("Hit Marker")) != null && hitMarker.isEnabled()) {
         hitMarker.onRenderHUD(context);
      }
   }
}
