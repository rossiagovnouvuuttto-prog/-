package com.reallyvisuals.mixin;

import net.minecraft.client.gui.DrawContext;

import com.reallyvisuals.gui.HUDManager;
import com.reallyvisuals.module.ModuleManager;
import com.reallyvisuals.module.RenderTweaks;
import net.minecraft.client.gui.hud.BossBarHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BossBarHud.class)
public class BossBarHudMixin {
   @Inject(method = "render", at = @At("HEAD"), cancellable = true)
   private void onRenderBossBar(DrawContext context, CallbackInfo ci) {
      RenderTweaks tweaks = (RenderTweaks)ModuleManager.getInstance().getModule("Render Tweaks");
      if (tweaks != null && tweaks.isEnabled() && tweaks.tweaks.isSelected("Боссбар")) {
         ci.cancel();
      } else {
         float dx = HUDManager.bossbar.x - HUDManager.bossbar.defaultX;
         float dy = HUDManager.bossbar.y - HUDManager.bossbar.defaultY;
         if (dx != 0.0F || dy != 0.0F) {
            context.getMatrices().pushMatrix();
            context.getMatrices().translate((float) (dx), (float) (dy));
         }
      }
   }

   @Inject(method = "render", at = @At("RETURN"))
   private void onRenderBossBarEnd(DrawContext context, CallbackInfo ci) {
      float dx = HUDManager.bossbar.x - HUDManager.bossbar.defaultX;
      float dy = HUDManager.bossbar.y - HUDManager.bossbar.defaultY;
      if (dx != 0.0F || dy != 0.0F) {
         context.getMatrices().popMatrix();
      }
   }
}
