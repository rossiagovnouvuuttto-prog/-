package com.reallyvisuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import com.reallyvisuals.utils.KeyUtils;

public class FreeLook extends Module {
   public static boolean active = false;
   public static float cameraYaw = 0.0F;
   public static float cameraPitch = 0.0F;
   public Module.ModeSetting modeSetting = new Module.ModeSetting("Режим", new String[]{"Hold", "Toggle"}, "Hold");
   public Module.KeySetting keySetting = new Module.KeySetting("Кнопка", 342);
   private Perspective previousPerspective = Perspective.FIRST_PERSON;
   private boolean activatedByThisModule = false;
   private boolean previousKeyPressed = false;

   public FreeLook() {
      super("Free Look", "Поворот камеры независимо от игрока", Category.UTILITIES, false, true);
      this.addSetting(this.modeSetting);
      this.addSetting(this.keySetting);
   }

   private void enableFreeLook() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player != null && mc.options != null) {
         this.previousPerspective = mc.options.getPerspective();
         cameraYaw = mc.player.getYaw();
         cameraPitch = mc.player.getPitch();
         mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
         active = true;
         this.activatedByThisModule = true;
      }
   }

   private void disableFreeLook() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc != null && mc.options != null) {
         mc.options.setPerspective(this.previousPerspective);
      }

      active = false;
      this.activatedByThisModule = false;
   }

   @Override
   public void onDisable() {
      if (active) {
         this.disableFreeLook();
      }

      this.activatedByThisModule = false;
      this.previousKeyPressed = false;
   }

   @Override
   public void onTick() {
      if (!this.isEnabled()) {
         if (active && this.activatedByThisModule) {
            this.disableFreeLook();
         }
      } else {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.player != null && mc.world != null && mc.getWindow() != null && mc.currentScreen == null) {
            int key = this.keySetting.key;
            if (key > 0) {
               boolean isPressed = KeyUtils.isMouseBind(key)
                  ? KeyUtils.isMouseButtonPressedSafe(mc.getWindow().getHandle(), KeyUtils.getMouseButton(key))
                  : KeyUtils.isKeyPressedSafe(mc.getWindow().getHandle(), key);
               if (this.modeSetting.value.equals("Toggle")) {
                  if (isPressed && !this.previousKeyPressed) {
                     if (active) {
                        this.disableFreeLook();
                     } else {
                        this.enableFreeLook();
                     }
                  }
               } else if (isPressed && !active) {
                  this.enableFreeLook();
               } else if (!isPressed && active && this.activatedByThisModule) {
                  this.disableFreeLook();
               }

               this.previousKeyPressed = isPressed;
            }
         } else {
            if (active && this.activatedByThisModule) {
               this.disableFreeLook();
            }

            this.previousKeyPressed = false;
         }
      }
   }
}
