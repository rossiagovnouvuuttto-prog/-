package com.reallyvisuals.mixin;

import com.reallyvisuals.module.ModuleManager;
import com.reallyvisuals.module.StreamerMode;
import java.util.List;
import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DebugHud.class)
public abstract class DebugHudMixin {
   @Inject(method = "getLeftText", at = @At("RETURN"), cancellable = true, require = 0)
   private void onGetLeftText(CallbackInfoReturnable<List<String>> cir) {
      StreamerMode mode = (StreamerMode)ModuleManager.getInstance().getModule("Streamer Mode");
      List<String> list;
      if (mode != null && mode.isEnabled() && mode.hideCoords.value && (list = (List<String>)cir.getReturnValue()) != null) {
         list.removeIf(line -> line.startsWith("XYZ:") || line.startsWith("Block:") || line.startsWith("Chunk:") || line.startsWith("Facing:"));
      }
   }
}
