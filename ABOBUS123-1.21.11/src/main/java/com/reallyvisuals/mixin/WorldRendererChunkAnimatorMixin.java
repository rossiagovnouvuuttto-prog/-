package com.reallyvisuals.mixin;

import com.reallyvisuals.module.ChunkAnimator;
import com.reallyvisuals.module.ModuleManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder.BuiltChunk;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WorldRenderer.class)
public class WorldRendererChunkAnimatorMixin {
   private static BuiltChunk reallyvisuals$currentChunk = null;

   @Redirect(
      method = "renderLayer",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/chunk/ChunkBuilder$BuiltChunk;getOrigin()Lnet/minecraft/util/math/BlockPos;"),
      require = 0
   )
   private BlockPos onGetChunkOrigin(BuiltChunk builtChunk) {
      reallyvisuals$currentChunk = builtChunk;
      return builtChunk.getOrigin();
   }

   @Redirect(
      method = "renderLayer",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(DDD)V"),
      require = 0
   )
   private void redirectChunkTranslate(MatrixStack matrices, double x, double y, double z) {
      ChunkAnimator animator = (ChunkAnimator)ModuleManager.getInstance().getModule("Chunk Animator");
      if (animator != null && animator.isEnabled() && reallyvisuals$currentChunk != null) {
         float[] offset = ChunkAnimator.animationHandler.getOffsetFor(reallyvisuals$currentChunk);
         matrices.translate(x + offset[0], y + offset[1], z + offset[2]);
      } else {
         matrices.translate(x, y, z);
      }

      reallyvisuals$currentChunk = null;
   }
}
