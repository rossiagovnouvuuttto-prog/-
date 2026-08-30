package com.reallyvisuals.mixin;

import com.reallyvisuals.module.CustomWorld;
import com.reallyvisuals.module.ModuleManager;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.world.ClientWorld;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.21.11: BackgroundRenderer is gone and fog lives in FogRenderer. The colour is no longer
 * held in static red/green/blue fields but returned as a Vector4f, so the tint is applied by
 * rewriting the return value instead of writing shadowed fields.
 */
@Mixin(FogRenderer.class)
public class BackgroundRendererMixin {

   @Inject(method = "getFogColor", at = @At("RETURN"), cancellable = true, require = 0)
   private void onGetFogColor(Camera camera, float tickDelta, ClientWorld world, int viewDistance,
                              float skyDarkness, CallbackInfoReturnable<Vector4f> cir) {
      CustomWorld customWorld = (CustomWorld) ModuleManager.getInstance().getModule("Custom World");
      if (customWorld == null || !customWorld.isEnabled()) {
         return;
      }
      CameraSubmersionType submersion = camera.getSubmersionType();
      if (submersion == CameraSubmersionType.WATER || submersion == CameraSubmersionType.LAVA) {
         return;
      }
      int color = customWorld.getColor();
      Vector4f out = cir.getReturnValue();
      float a = out == null ? 1.0F : out.w;
      cir.setReturnValue(new Vector4f(
         (color >> 16 & 0xFF) / 255.0F,
         (color >> 8 & 0xFF) / 255.0F,
         (color & 0xFF) / 255.0F,
         a
      ));
   }
}
