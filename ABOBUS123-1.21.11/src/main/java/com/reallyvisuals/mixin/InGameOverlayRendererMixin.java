package com.reallyvisuals.mixin;

import com.reallyvisuals.module.ModuleManager;
import com.reallyvisuals.module.NoFluid;
import com.reallyvisuals.module.RenderTweaks;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.21.11: both overlays take the sprite and a VertexConsumerProvider instead
 * of a MinecraftClient, and the two methods do not agree on parameter order --
 * renderFireOverlay is (MatrixStack, VertexConsumerProvider, Sprite) while
 * renderInWallOverlay is (Sprite, MatrixStack, VertexConsumerProvider).
 */
@Mixin(InGameOverlayRenderer.class)
public class InGameOverlayRendererMixin {

   @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
   private static void abobus123$onRenderFireOverlay(
      MatrixStack matrices, VertexConsumerProvider vertexConsumers, Sprite sprite, CallbackInfo ci
   ) {
      RenderTweaks tweaks = (RenderTweaks) ModuleManager.getInstance().getModule("Render Tweaks");
      if (tweaks != null && tweaks.isEnabled() && tweaks.tweaks.isSelected("Оверлей огня")) {
         ci.cancel();
      }
   }

   @Inject(method = "renderInWallOverlay", at = @At("HEAD"), cancellable = true)
   private static void abobus123$onRenderUnderwaterOverlay(
      Sprite sprite, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci
   ) {
      NoFluid noFluid = (NoFluid) ModuleManager.getInstance().getModule("No Fluid");
      if (noFluid != null && noFluid.isEnabled()) {
         ci.cancel();
      }
   }
}
