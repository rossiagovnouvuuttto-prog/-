package com.reallyvisuals;

import com.reallyvisuals.config.ConfigManager;
import com.reallyvisuals.gui.ReallyVisualsScreen;
import com.reallyvisuals.module.Module;
import com.reallyvisuals.module.ModuleManager;
import com.reallyvisuals.module.SwordBat;
import com.reallyvisuals.utils.KeyUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.StartTick;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil.Type;

public class ReallyVisualsMod implements ClientModInitializer {
   public static final String MOD_ID = "abobus123";
   private static KeyBinding openGuiKey;
   private static boolean wasKeyPressed = false;

   public void onInitializeClient() {
      WorldRenderHandler.register();
      System.out.println("[ABOBUS123] Initializing 1.21.11 Fabric Mod...");
      openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.abobus123.open_gui", Type.KEYSYM, 344, net.minecraft.client.option.KeyBinding.Category.create(net.minecraft.util.Identifier.of("abobus123", "gui"))));
      ConfigManager.loadConfig();
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
         System.out.println("[ABOBUS123] Auto-saving configuration on client shutdown...");
         ConfigManager.saveConfig();
      }));
      ClientTickEvents.START_CLIENT_TICK.register((StartTick)client -> {
         for (Module module : ModuleManager.getInstance().getModules()) {
            try {
               module.onTick();
            } catch (Throwable t) {
               System.err.println("[ABOBUS123] Module tick failed: " + module.getName() + " -> " + t);
               t.printStackTrace();
               try {
                  module.setEnabled(false);
               } catch (Throwable ignored) {
               }
            }
         }
         int targetKey = ReallyVisualsScreen.menuKeyCode;
         boolean isDefaultKey = targetKey == 344;
         if (isDefaultKey) {
            while (openGuiKey.wasPressed()) {
               if (client.currentScreen == null) {
                  client.setScreen(new ReallyVisualsScreen());
               }
            }
         } else {
            while (openGuiKey.wasPressed()) {
            }

            if (client.getWindow() != null && targetKey > 0) {
               boolean pressed = KeyUtils.isKeyPressedSafe(client.getWindow().getHandle(), targetKey);
               if (pressed && !wasKeyPressed && client.currentScreen == null) {
                  client.setScreen(new ReallyVisualsScreen());
               }

               wasKeyPressed = pressed;
            }
         }
      });
   }
}
