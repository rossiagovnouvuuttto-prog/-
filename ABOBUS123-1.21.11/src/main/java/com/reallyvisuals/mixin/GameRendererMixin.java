package com.reallyvisuals.mixin;

import net.minecraft.client.gui.DrawContext;

import com.reallyvisuals.module.AspectRatio;
import com.reallyvisuals.module.FullBright;
import com.reallyvisuals.module.ModuleManager;
import com.reallyvisuals.module.RenderTweaks;
import com.reallyvisuals.module.Zoom;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
   /**
    * 1.21.11: the old @Redirect targeted net.minecraft.util.math.Matrix4f,
    * a class removed back in 1.19, so it could never bind and would crash on
    * load. getBasicProjectionMatrix now returns an org.joml.Matrix4f, and in a
    * perspective matrix m00 == m11 / aspect -- so the ratio is retuned in place
    * without having to guess the near/far planes.
    */
   @Inject(method = "getBasicProjectionMatrix", at = @At("RETURN"), cancellable = true)
   private void abobus123$aspectRatio(float fov, CallbackInfoReturnable<Matrix4f> cir) {
      AspectRatio aspectModule = (AspectRatio) ModuleManager.getInstance().getModule("Aspect Ratio");
      if (aspectModule == null || !aspectModule.isEnabled()) {
         return;
      }
      float ratio = aspectModule.getRatio();
      if (ratio <= 0.0F) {
         return;
      }
      Matrix4f matrix = new Matrix4f(cir.getReturnValue());
      matrix.m00(matrix.m11() / ratio);
      cir.setReturnValue(matrix);
   }

   @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
   private void onGetFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
      Zoom zoom = (Zoom)ModuleManager.getInstance().getModule("Zoom");
      if (zoom != null && zoom.isZooming()) {
         cir.setReturnValue((float) ((double)zoom.getZoomFov()));
      }
   }

   @Inject(method = "render", at = @At("HEAD"))
   private void onRenderHead(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
      FullBright fullBright = (FullBright)ModuleManager.getInstance().getModule("Full Bright");
      if (fullBright != null) {
         fullBright.tick();
      }
   }

   @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
   private void onTiltViewWhenHurt(net.minecraft.client.util.math.MatrixStack matrices, float tickDelta, CallbackInfo ci) {
      RenderTweaks tweaks = (RenderTweaks)ModuleManager.getInstance().getModule("Render Tweaks");
      if (tweaks != null && tweaks.isEnabled() && tweaks.tweaks.isSelected("Тряска урона")) {
         ci.cancel();
      }
   }

   @Inject(method = "showFloatingItem", at = @At("HEAD"), cancellable = true)
   private void onShowFloatingItem(ItemStack floatingItem, CallbackInfo ci) {
      RenderTweaks tweaks = (RenderTweaks)ModuleManager.getInstance().getModule("Render Tweaks");
      if (tweaks != null && tweaks.isEnabled() && tweaks.tweaks.isSelected("Анимация тотема") && floatingItem.getItem() == Items.TOTEM_OF_UNDYING) {
         ci.cancel();
      }
   }
}
