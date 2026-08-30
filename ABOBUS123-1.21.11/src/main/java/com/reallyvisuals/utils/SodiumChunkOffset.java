package com.reallyvisuals.utils;

public final class SodiumChunkOffset {
   private static final ThreadLocal<float[]> CURRENT = new ThreadLocal<>();
   private static final ThreadLocal<int[]> AXIS = new ThreadLocal<>();

   private SodiumChunkOffset() {
   }

   public static void set(float[] offset) {
      CURRENT.set(offset);
      int[] axis = AXIS.get();
      if (axis == null) {
         AXIS.set(new int[]{0});
      } else {
         axis[0] = 0;
      }
   }

   public static void clear() {
      CURRENT.set(null);
      int[] axis = AXIS.get();
      if (axis != null) {
         axis[0] = 0;
      }
   }

   public static float nextAxisOffset() {
      float[] off = CURRENT.get();
      if (off == null) {
         return 0.0F;
      } else {
         int[] axis = AXIS.get();
         if (axis == null) {
            return 0.0F;
         } else {
            int i = axis[0];
            axis[0] = i + 1;
            return i >= 0 && i < off.length ? off[i] : 0.0F;
         }
      }
   }
}
