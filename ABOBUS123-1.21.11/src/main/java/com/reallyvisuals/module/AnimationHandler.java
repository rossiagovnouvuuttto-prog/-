package com.reallyvisuals.module;

import java.util.WeakHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.chunk.ChunkBuilder.BuiltChunk;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

public class AnimationHandler {
   private final WeakHashMap<BuiltChunk, AnimationHandler.AnimationData> timeStamps = new WeakHashMap<>();
   private final WeakHashMap<BuiltChunk, BlockPos> completedOrigins = new WeakHashMap<>();
   private final java.util.Map<Long, AnimationHandler.AnimationData> byChunkPos = new java.util.concurrent.ConcurrentHashMap<>();

   private static long packKey(int blockX, int blockY, int blockZ) {
      int cx = blockX >> 4;
      int cy = blockY >> 4;
      int cz = blockZ >> 4;
      return ((long)cx & 4194303L) << 42 | ((long)cy & 1048575L) << 22 | (long)cz & 4194303L;
   }

   public float[] getOffsetForPos(int blockX, int blockY, int blockZ) {
      ChunkAnimator animator = (ChunkAnimator)ModuleManager.getInstance().getModule("Chunk Animator");
      if (animator == null || !animator.isEnabled()) {
         return null;
      } else {
         long key = packKey(blockX, blockY, blockZ);
         AnimationHandler.AnimationData data = this.byChunkPos.get(key);
         if (data == null) {
            return null;
         } else {
            int duration = (int)animator.duration.value;
            if (duration <= 0) {
               this.byChunkPos.remove(key);
               return null;
            } else {
               long time = data.timeStamp;
               if (time == -1L) {
                  data.timeStamp = time = System.currentTimeMillis();
                  MinecraftClient mc = MinecraftClient.getInstance();
                  if (("Сбоку".equals(animator.mode.value) || "Сбоку (альт.)".equals(animator.mode.value)) && mc.player != null) {
                     int px = MathHelper.floor(mc.player.getX());
                     int pz = MathHelper.floor(mc.player.getZ());
                     BlockPos playerPos = new BlockPos(px, 0, pz);
                     BlockPos centeredChunkPos = new BlockPos(data.origin.getX() + 8, 0, data.origin.getZ() + 8);
                     data.chunkFacing = getChunkFacing(playerPos, centeredChunkPos);
                  }
               }

               long elapsed = System.currentTimeMillis() - time;
               if (elapsed >= (long)duration) {
                  this.byChunkPos.remove(key);
                  return null;
               } else {
                  return this.calculateOffset(animator, data, elapsed, duration);
               }
            }
         }
      }
   }

   public void setOrigin(BuiltChunk builtChunk, BlockPos position) {
      if (builtChunk != null && position != null) {
         this.completedOrigins.remove(builtChunk);
         MinecraftClient mc = MinecraftClient.getInstance();
         ClientPlayerEntity player = mc.player;
         Direction facing = null;
         boolean nearPlayer = false;
         if (player != null) {
            int px = MathHelper.floor(player.getX());
            int pz = MathHelper.floor(player.getZ());
            BlockPos playerPos = new BlockPos(px, 0, pz);
            BlockPos centeredChunkPos = new BlockPos(position.getX() + 8, 0, position.getZ() + 8);
            long distanceX = (long)playerPos.getX() - centeredChunkPos.getX();
            long distanceZ;
            nearPlayer = distanceX * distanceX + (distanceZ = (long)playerPos.getZ() - centeredChunkPos.getZ()) * distanceZ <= 4096L;
            ChunkAnimator animator2 = (ChunkAnimator)ModuleManager.getInstance().getModule("Chunk Animator");
            if (animator2 != null && ("Сбоку".equals(animator2.mode.value) || "Сбоку (альт.)".equals(animator2.mode.value))) {
               facing = getChunkFacing(playerPos, centeredChunkPos);
            }
         }

         ChunkAnimator animator;
         boolean disableAroundPlayer = (animator = (ChunkAnimator)ModuleManager.getInstance().getModule("Chunk Animator")) != null
            && animator.dontAnimateNear.value;
         long key = packKey(position.getX(), position.getY(), position.getZ());
         if (disableAroundPlayer && nearPlayer) {
            this.timeStamps.remove(builtChunk);
            this.completedOrigins.put(builtChunk, position);
            this.byChunkPos.remove(key);
         } else {
            this.timeStamps.put(builtChunk, new AnimationHandler.AnimationData(-1L, facing, position));
            this.byChunkPos.put(key, new AnimationHandler.AnimationData(-1L, facing, position));
         }
      }
   }

   public void setOriginByPos(BlockPos position) {
      if (position != null) {
         MinecraftClient mc = MinecraftClient.getInstance();
         ClientPlayerEntity player = mc.player;
         Direction facing = null;
         boolean nearPlayer = false;
         if (player != null) {
            int px = MathHelper.floor(player.getX());
            int pz = MathHelper.floor(player.getZ());
            BlockPos playerPos = new BlockPos(px, 0, pz);
            BlockPos centeredChunkPos = new BlockPos(position.getX() + 8, 0, position.getZ() + 8);
            long distanceX = (long)playerPos.getX() - centeredChunkPos.getX();
            long distanceZ = (long)playerPos.getZ() - centeredChunkPos.getZ();
            nearPlayer = distanceX * distanceX + distanceZ * distanceZ <= 4096L;
            ChunkAnimator animator2 = (ChunkAnimator)ModuleManager.getInstance().getModule("Chunk Animator");
            if (animator2 != null && ("Сбоку".equals(animator2.mode.value) || "Сбоку (альт.)".equals(animator2.mode.value))) {
               facing = getChunkFacing(playerPos, centeredChunkPos);
            }
         }

         ChunkAnimator animator;
         boolean disableAroundPlayer = (animator = (ChunkAnimator)ModuleManager.getInstance().getModule("Chunk Animator")) != null
            && animator.dontAnimateNear.value;
         long key = packKey(position.getX(), position.getY(), position.getZ());
         if (disableAroundPlayer && nearPlayer) {
            this.byChunkPos.remove(key);
         } else {
            this.byChunkPos.put(key, new AnimationHandler.AnimationData(-1L, facing, position));
         }
      }
   }

   public float[] getOffsetFor(BuiltChunk builtChunk) {
      if (builtChunk == null) {
         return new float[]{0.0F, 0.0F, 0.0F};
      }

      AnimationHandler.AnimationData animationData = this.timeStamps.get(builtChunk);
      if (animationData == null) {
         return new float[]{0.0F, 0.0F, 0.0F};
      }

      ChunkAnimator animator = (ChunkAnimator)ModuleManager.getInstance().getModule("Chunk Animator");
      if (animator != null && animator.isEnabled()) {
         int duration = (int)animator.duration.value;
         if (duration <= 0) {
            this.timeStamps.remove(builtChunk);
            return new float[]{0.0F, 0.0F, 0.0F};
         }

         long time = animationData.timeStamp;
         if (time == -1L) {
            animationData.timeStamp = time = System.currentTimeMillis();
            MinecraftClient mc = MinecraftClient.getInstance();
            if (("Сбоку".equals(animator.mode.value) || "Сбоку (альт.)".equals(animator.mode.value)) && mc.player != null) {
               int px = MathHelper.floor(mc.player.getX());
               int pz = MathHelper.floor(mc.player.getZ());
               BlockPos playerPos = new BlockPos(px, 0, pz);
               BlockPos centeredChunkPos = new BlockPos(animationData.origin.getX() + 8, 0, animationData.origin.getZ() + 8);
               animationData.chunkFacing = getChunkFacing(playerPos, centeredChunkPos);
            }
         }

         long elapsed;
         if ((elapsed = System.currentTimeMillis() - time) >= duration) {
            this.timeStamps.remove(builtChunk);
            this.completedOrigins.put(builtChunk, animationData.origin);
            return new float[]{0.0F, 0.0F, 0.0F};
         } else {
            return this.calculateOffset(animator, animationData, elapsed, duration);
         }
      } else {
         return new float[]{0.0F, 0.0F, 0.0F};
      }
   }

   private float[] calculateOffset(ChunkAnimator animator, AnimationHandler.AnimationData animationData, long elapsed, int duration) {
      BlockPos origin = animationData.origin;
      String mode = animator.mode.value;
      String easing = animator.easing.value;
      if ("Снизу".equals(mode)) {
         float distance = Math.abs(origin.getY());
         if (distance < 16.0F) {
            distance = 16.0F;
         }

         float yOffset = -distance + this.calculateEasing(easing, (float)elapsed, 0.0F, distance, duration);
         return new float[]{0.0F, yOffset, 0.0F};
      } else if ("Сверху".equals(mode)) {
         float distance = Math.max(0, 256 - origin.getY());
         if (distance < 16.0F) {
            distance = 16.0F;
         }

         float yOffset = distance - this.calculateEasing(easing, (float)elapsed, 0.0F, distance, duration);
         return new float[]{0.0F, yOffset, 0.0F};
      } else {
         if ("От горизонта".equals(mode)) {
            float distance = 100.0F;
            float yOffset = -distance + this.calculateEasing(easing, (float)elapsed, 0.0F, distance, duration);
            return new float[]{0.0F, yOffset, 0.0F};
         }

         if ("Сбоку".equals(mode) || "Сбоку (альт.)".equals(mode)) {
            Direction facing = animationData.chunkFacing;
            if (animationData.chunkFacing != null) {
               float distance = -(100.0F - this.calculateEasing(easing, (float)elapsed, 0.0F, 100.0F, duration));
               if ("Сбоку (альт.)".equals(mode)) {
                  distance = -distance;
               }

               return new float[]{facing.getOffsetX() * distance, 0.0F, facing.getOffsetZ() * distance};
            }
         }

         return new float[]{0.0F, 0.0F, 0.0F};
      }
   }

   private float calculateEasing(String easingMode, float t, float b, float c, float d) {
      if (!(d <= 0.0F) && !(t >= d)) {
         t = Math.max(0.0F, t);
         switch (easingMode) {
            case "Linear":
               return c * t / d + b;
            case "Quad":
               float var11;
               return -c * (var11 = t / d) * (var11 - 2.0F) + b;
            case "Cubic":
               t = t / d - 1.0F;
               return c * (t * t * t + 1.0F) + b;
            case "Quart":
               t = t / d - 1.0F;
               return -c * (t * t * t * t - 1.0F) + b;
            default:
               return (float)(c * Math.sin(t / d * (Math.PI / 2)) + b);
         }
      } else {
         return b + c;
      }
   }

   public void clear() {
      this.timeStamps.clear();
      this.completedOrigins.clear();
      this.byChunkPos.clear();
   }

   private static Direction getChunkFacing(BlockPos from, BlockPos to) {
      int diffX = from.getX() - to.getX();
      int diffZ = from.getZ() - to.getZ();
      int absX = Math.abs(diffX);
      if (absX > Math.abs(diffZ)) {
         return diffX > 0 ? Direction.WEST : Direction.EAST;
      } else {
         return diffZ > 0 ? Direction.NORTH : Direction.SOUTH;
      }
   }

   public static class AnimationData {
      public long timeStamp;
      public Direction chunkFacing;
      public BlockPos origin;

      public AnimationData(long timeStamp, Direction chunkFacing, BlockPos origin) {
         this.timeStamp = timeStamp;
         this.chunkFacing = chunkFacing;
         this.origin = origin;
      }
   }
}
