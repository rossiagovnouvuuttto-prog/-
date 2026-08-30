package com.reallyvisuals.mixin.sodium;

import com.reallyvisuals.utils.SodiumChunkAnimator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderManager", remap = false)
public abstract class ChunkRenderManagerMixin {
   @Inject(method = "onChunkAdded", at = @At("TAIL"), require = 0, expect = 0)
   private void reallyvisuals$onChunkAdded(int chunkX, int chunkZ, CallbackInfo ci) {
      SodiumChunkAnimator.registerColumn(chunkX, chunkZ);
   }
}
