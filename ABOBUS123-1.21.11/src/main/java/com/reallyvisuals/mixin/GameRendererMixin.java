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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
   @Redirect(
      method = "getBasicProjectionMatrix",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Matrix4f;viewboxMatrix(DFFF)Lnet/minecraft/util/math/Matrix4f;")
   )
   private Matrix4f redirectViewboxMatrix(double fov, float aspectRatio, float cameraDepth, float viewDistance) {
      AspectRatio aspectModule = (AspectRatio)ModuleManager.getInstance().getModule("Aspect Ratio");
      if (aspectModule != null && aspectModule.isEnabled()) {
         aspectRatio = aspectModule.getRatio();
      }

      return new Matrix4f().perspective((float) Math.toRadians(fov), aspectRatio, cameraDepth, viewDistance);
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
