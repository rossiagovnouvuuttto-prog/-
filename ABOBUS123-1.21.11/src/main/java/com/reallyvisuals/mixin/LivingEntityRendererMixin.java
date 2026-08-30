package com.reallyvisuals.mixin;

import com.reallyvisuals.module.HitColor;
import com.reallyvisuals.module.ModuleManager;
import com.reallyvisuals.module.SelfNametag;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {

   /**
    * 1.21.11: rendering goes through an OrderedRenderCommandQueue, so the old
    * @Redirect on Model.render had no injection point at all and hard-crashed
    * the game. The hurt tint is now a single ARGB value from getMixColor, so
    * Hit Color recolours that instead. Vanilla's alpha is kept so the blend
    * strength is unchanged -- only the hue is ours.
    */
   @Inject(
      method = "getMixColor(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;)I",
      at = @At("RETURN"),
      cancellable = true
   )
   private void abobus123$hitColor(LivingEntityRenderState state, CallbackInfoReturnable<Integer> cir) {
      HitColor hitColor = (HitColor) ModuleManager.getInstance().getModule("Hit Color");
      if (hitColor == null || !hitColor.isEnabled() || !state.hurt) {
         return;
      }

      int color = hitColor.getColor();
      int r = color >> 16 & 0xFF;
      int g = color >> 8 & 0xFF;
      int b = color & 0xFF;
      // Near-black would swallow the model entirely; the 1.18.2 mixin nudged it too.
      if (color == -16448251 || color == -16777216) {
         r = g = b = 13;
      }

      int alpha = cir.getReturnValueI() >>> 24;
      cir.setReturnValue((alpha << 24) | (r << 16) | (g << 8) | b);
   }

   @Inject(
      method = "hasLabel(Lnet/minecraft/entity/LivingEntity;D)Z",
      at = @At("HEAD"),
      cancellable = true
   )
   private void onHasLabel(net.minecraft.entity.LivingEntity livingEntity, double d, CallbackInfoReturnable<Boolean> cir) {
      SelfNametag selfNametag = (SelfNametag) ModuleManager.getInstance().getModule("Self Nametag");
      if (selfNametag != null && selfNametag.isEnabled()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (livingEntity == mc.player && !mc.options.getPerspective().isFirstPerson()) {
            cir.setReturnValue(true);
         }
      }
   }
}
