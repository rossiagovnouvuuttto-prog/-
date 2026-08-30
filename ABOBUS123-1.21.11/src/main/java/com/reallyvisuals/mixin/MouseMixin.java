package com.reallyvisuals.mixin;

import com.reallyvisuals.gui.HudEditor;
import com.reallyvisuals.gui.ReallyVisualsScreen;
import com.reallyvisuals.module.Module;
import com.reallyvisuals.module.ModuleManager;
import com.reallyvisuals.module.Zoom;
import com.reallyvisuals.utils.KeyUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
   @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
   private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.currentScreen instanceof ChatScreen) {
         double sx = getScaledMouseX(mc, mc.mouse.getX());
         double sy = getScaledMouseY(mc, mc.mouse.getY());
         if (HudEditor.mouseScrolled(sx, sy, vertical)) {
            ci.cancel();
            return;
         }
      }

      Zoom zoom;
      if (mc.currentScreen == null && (zoom = (Zoom)ModuleManager.getInstance().getModule("Zoom")) != null && zoom.isZooming()) {
         zoom.onMouseScroll(vertical);
         ci.cancel();
      }
   }

   @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
   private void onMouseButton(long window, net.minecraft.client.input.MouseInput input, int action, CallbackInfo ci) {
      int button = input.button();
      int mods = input.modifiers();
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.currentScreen instanceof ChatScreen) {
         double sx = getScaledMouseX(mc, mc.mouse.getX());
         double sy = getScaledMouseY(mc, mc.mouse.getY());
         if (action == 1 && HudEditor.mousePressed(sx, sy, button)) {
            ci.cancel();
            return;
         }
         if (action == 0 && HudEditor.mouseReleased(button)) {
            ci.cancel();
            return;
         }
      }

      if (mc.currentScreen == null && action == 1 && button >= 0) {
         int bind = KeyUtils.toMouseBind(button);
         if (ReallyVisualsScreen.menuKeyCode == bind && mc.currentScreen == null) {
            mc.setScreen(new ReallyVisualsScreen());
            ci.cancel();
            return;
         }

         for (Module m : ModuleManager.getInstance().getModules()) {
            if (m.getKey() != 0 && m.getKey() == bind) {
               m.toggle();
            }

            m.onKeyPressed(bind);
         }
      }
   }

   @Inject(method = "onCursorPos", at = @At("TAIL"))
   private void onCursorPos(long window, double x, double y, CallbackInfo ci) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.currentScreen instanceof ChatScreen && HudEditor.isDragging()) {
         HudEditor.mouseMoved(getScaledMouseX(mc, x), getScaledMouseY(mc, y));
      }
   }

   private static double getScaledMouseX(MinecraftClient mc, double rawX) {
      if (mc.getWindow() == null || mc.getWindow().getWidth() <= 0) return rawX;
      return rawX * (double)mc.getWindow().getScaledWidth() / (double)mc.getWindow().getWidth();
   }

   private static double getScaledMouseY(MinecraftClient mc, double rawY) {
      if (mc.getWindow() == null || mc.getWindow().getHeight() <= 0) return rawY;
      return rawY * (double)mc.getWindow().getScaledHeight() / (double)mc.getWindow().getHeight();
   }
}
