package com.reallyvisuals.utils;

public class MemoryOptimizer {
   private static long lastGcTime = 0L;

   public static void checkAndCleanMemory() {
      long now = System.currentTimeMillis();
      if (now - lastGcTime >= 30000L) {
         Runtime runtime = Runtime.getRuntime();
         long totalMemory = runtime.totalMemory();
         long usedMemory = totalMemory - runtime.freeMemory();
         if (usedMemory > runtime.maxMemory() * 0.75) {
            System.gc();
            lastGcTime = now;
         }
      }
   }

   public static void forceClean() {
      System.gc();
      lastGcTime = System.currentTimeMillis();
   }
}
