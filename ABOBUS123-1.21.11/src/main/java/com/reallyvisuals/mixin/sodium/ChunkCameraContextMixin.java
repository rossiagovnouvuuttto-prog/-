package com.reallyvisuals.mixin.sodium;

import com.reallyvisuals.utils.SodiumChunkOffset;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.render.chunk.ChunkCameraContext", remap = false)
public class ChunkCameraContextMixin {
   @Inject(method = "getChunkModelOffset", at = @At("RETURN"), cancellable = true, require = 0, expect = 0)
   private void reallyvisuals$applyAnimation(int pos, int origin, float cameraOffset, CallbackInfoReturnable<Float> cir) {
      float extra = SodiumChunkOffset.nextAxisOffset();
      if (extra != 0.0F) {
         cir.setReturnValue(cir.getReturnValue() + extra);
      }
   }
}
