package com.reallyvisuals.mixin.sodium;

import com.reallyvisuals.utils.SodiumChunkAnimator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState", remap = false)
public abstract class ChunkGraphicsStateMixin {
   @Shadow
   @Final
   private int x;

   @Shadow
   @Final
   private int y;

   @Shadow
   @Final
   private int z;

   @Inject(method = "getX", at = @At("HEAD"), require = 0, expect = 0)
   private void reallyvisuals$captureChunk(CallbackInfo ci) {
      SodiumChunkAnimator.hook(this.x + 8, this.y + 8, this.z + 8);
   }
}
