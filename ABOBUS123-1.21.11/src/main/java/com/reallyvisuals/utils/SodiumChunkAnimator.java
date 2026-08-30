package com.reallyvisuals.utils;

import com.reallyvisuals.module.ChunkAnimator;
import com.reallyvisuals.module.ModuleManager;
import net.minecraft.util.math.BlockPos;

public final class SodiumChunkAnimator {
   private SodiumChunkAnimator() {
   }

   public static void hook(int x, int y, int z) {
      ChunkAnimator animator = (ChunkAnimator)ModuleManager.getInstance().getModule("Chunk Animator");
      if (animator != null && animator.isEnabled()) {
         SodiumChunkOffset.set(ChunkAnimator.animationHandler.getOffsetForPos(x, y, z));
      } else {
         SodiumChunkOffset.clear();
      }
   }

   public static void registerColumn(int chunkX, int chunkZ) {
      for (int cy = 0; cy < 16; cy++) {
         ChunkAnimator.animationHandler.setOriginByPos(new BlockPos(chunkX << 4, cy << 4, chunkZ << 4));
      }
   }
}
