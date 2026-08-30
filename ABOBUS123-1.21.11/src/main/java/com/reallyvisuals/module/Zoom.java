package com.reallyvisuals.module;

import com.reallyvisuals.utils.AnimationUtils;
import net.minecraft.client.MinecraftClient;
import com.reallyvisuals.utils.KeyUtils;

public class Zoom extends Module {
   public Module.KeySetting keySetting = new Module.KeySetting("Кнопка зума", 67);
   public Module.NumberSetting startFovSetting = new Module.NumberSetting("Стартовый FOV", 50.0, 10.0, 100.0, 1.0);
   private final AnimationUtils.Animation fovAnimation = new AnimationUtils.Animation(70.0F);
   private float currentTargetFov = 50.0F;
   private boolean zooming = false;

   public Zoom() {
      super("Zoom", "Плавный зум с регулировкой колесиком мыши", Category.UTILITIES, false, true);
      this.addSetting(this.keySetting);
      this.addSetting(this.startFovSetting);
   }

   public boolean isZooming() {
      return this.isEnabled() && this.zooming;
   }

   public float getZoomFov() {
      return this.fovAnimation.getValue();
   }

   public void onMouseScroll(double amount) {
      if (this.isZooming()) {
         if (amount > 0.0) {
            this.currentTargetFov = Math.max(5.0F, this.currentTargetFov - 5.0F);
         } else if (amount < 0.0) {
            this.currentTargetFov = Math.min(110.0F, this.currentTargetFov + 5.0F);
         }

         this.fovAnimation.animateTo(this.currentTargetFov, 100L);
      }
   }

   @Override
   public void onTick() {
      if (this.isEnabled()) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client.player != null && client.getWindow() != null) {
            if (this.keySetting.key > 0) {
               long window = client.getWindow().getHandle();
               boolean isPressed;
               if (KeyUtils.isMouseBind(this.keySetting.key)) {
                  isPressed = KeyUtils.isMouseButtonPressedSafe(window, KeyUtils.getMouseButton(this.keySetting.key));
               } else {
                  isPressed = KeyUtils.isKeyPressedSafe(window, this.keySetting.key);
               }

               if (isPressed && !this.zooming) {
                  this.zooming = true;
                  this.currentTargetFov = (float)this.startFovSetting.value;
                  this.fovAnimation.force((float)client.options.getFov().getValue());
                  this.fovAnimation.animateTo(this.currentTargetFov, 120L);
               } else if (!isPressed && this.zooming) {
                  this.zooming = false;
                  this.fovAnimation.animateTo((float)client.options.getFov().getValue(), 120L);
               }
            }
         }
      }
   }
}
