package com.reallyvisuals.module;

import net.minecraft.client.MinecraftClient;

public class FullBright extends Module {
   private double prevGamma = 1.0;

   public FullBright() {
      super("Full Bright", "Увеличение яркости мира", Category.VISUALS, false, false);
   }

   @Override
   public void onEnable() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.options != null) {
         this.prevGamma = mc.options.getGamma().getValue();
         mc.options.getGamma().setValue(100.0);
      }
   }

   @Override
   public void onDisable() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.options != null) {
         mc.options.getGamma().setValue(this.prevGamma);
      }
   }

   public void tick() {
      if (this.isEnabled()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.options != null && mc.options.getGamma().getValue() < 10.0) {
            mc.options.getGamma().setValue(100.0);
         }
      }
   }
}
