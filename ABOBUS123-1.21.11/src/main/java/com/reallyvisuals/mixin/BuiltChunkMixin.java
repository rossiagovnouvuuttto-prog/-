package com.reallyvisuals.mixin;

import com.reallyvisuals.module.ChunkAnimator;
import net.minecraft.client.render.chunk.ChunkBuilder.BuiltChunk;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BuiltChunk.class)
public abstract class BuiltChunkMixin {
   @Inject(method = "setOrigin", at = @At("TAIL"))
   private void onSetOrigin(int x, int y, int z, CallbackInfo ci) {
      ChunkAnimator.animationHandler.setOrigin((BuiltChunk)(Object)this, new BlockPos(x, y, z));
   }
}
