package com.reallyvisuals.mixin;

import com.reallyvisuals.module.ModuleManager;
import com.reallyvisuals.module.NoFluid;
import com.reallyvisuals.module.RenderTweaks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameOverlayRenderer.class)
public class InGameOverlayRendererMixin {
   @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
   private static void abobus123$onRenderFireOverlay(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
      RenderTweaks tweaks = (RenderTweaks) ModuleManager.getInstance().getModule("Render Tweaks");
      if (tweaks != null && tweaks.isEnabled() && tweaks.tweaks.isSelected("Оверлей огня")) {
         ci.cancel();
      }
   }

   @Inject(method = "renderInWallOverlay", at = @At("HEAD"), cancellable = true)
   private static void abobus123$onRenderUnderwaterOverlay(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
      NoFluid noFluid = (NoFluid) ModuleManager.getInstance().getModule("No Fluid");
      if (noFluid != null && noFluid.isEnabled()) {
         ci.cancel();
      }
   }
}
