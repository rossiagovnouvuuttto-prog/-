package com.reallyvisuals.mixin;

import com.reallyvisuals.module.LockSlot;
import com.reallyvisuals.module.Module;
import com.reallyvisuals.module.ModuleManager;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {
   @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
   private void onKey(long window, int action, net.minecraft.client.input.KeyInput input, CallbackInfo ci) {
      int key = input.key();
      int scancode = input.scancode();
      int modifiers = input.modifiers();
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.currentScreen == null) {
         if (action == 1 && key > 0) {
            LockSlot lockSlot = (LockSlot)ModuleManager.getInstance().getModule("Lock Slot");
            if (lockSlot != null
               && lockSlot.isEnabled()
               && mc.player != null
               && (key == 81 || mc.options != null && mc.options.dropKey.matchesKey(new net.minecraft.client.input.KeyInput(key, scancode, 0)))
               && lockSlot.isSlotLocked(mc.player.getInventory().getSelectedSlot())) {
               ci.cancel();
               return;
            }

            for (Module m : ModuleManager.getInstance().getModules()) {
               if (m.getKey() != 0 && m.getKey() == key) {
                  m.toggle();
               }

               m.onKeyPressed(key);
            }
         }
      }
   }
}
