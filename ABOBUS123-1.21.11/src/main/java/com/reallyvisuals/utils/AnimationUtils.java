package com.reallyvisuals.utils;

public class AnimationUtils {
   public static float easeOutExpo(float x) {
      return x == 1.0F ? 1.0F : 1.0F - (float)Math.pow(2.0, -10.0F * x);
   }

   public static float easeOutCubic(float x) {
      return 1.0F - (float)Math.pow(1.0F - x, 3.0);
   }

   public static float easeInOutCubic(float x) {
      return x < 0.5 ? 4.0F * x * x * x : 1.0F - (float)Math.pow(-2.0F * x + 2.0F, 3.0) / 2.0F;
   }

   public static class Animation {
      private float value;
      private float startValue;
      private float targetValue;
      private long startTime;
      private long duration;
      private int easing;

      public Animation(float startValue) {
         this.value = startValue;
         this.startValue = startValue;
         this.targetValue = startValue;
      }

      public void animateTo(float target, long durationMs) {
         if (this.targetValue != target) {
            this.startValue = this.value;
            this.targetValue = target;
            this.duration = durationMs;
            this.startTime = System.currentTimeMillis();
         }
      }

      public void force(float value) {
         this.value = value;
         this.startValue = value;
         this.targetValue = value;
         this.startTime = 0L;
      }

      public void setEasing(int easing) {
         this.easing = easing;
      }

      public float getValue() {
         if (this.startTime == 0L) {
            return this.value;
         } else {
            long elapsed = System.currentTimeMillis() - this.startTime;
            if (elapsed >= this.duration) {
               this.value = this.targetValue;
               this.startTime = 0L;
               return this.value;
            } else {
               float progress = (float)elapsed / (float)this.duration;
               float eased = this.easing == 1 ? AnimationUtils.easeOutCubic(progress) : AnimationUtils.easeOutExpo(progress);
               this.value = this.startValue + (this.targetValue - this.startValue) * eased;
               return this.value;
            }
         }
      }

      public float getTarget() {
         return this.targetValue;
      }
   }
}
