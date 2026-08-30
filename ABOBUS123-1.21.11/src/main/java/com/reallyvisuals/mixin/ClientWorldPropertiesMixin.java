package com.reallyvisuals.mixin;

import com.reallyvisuals.module.ModuleManager;
import com.reallyvisuals.module.TimeChanger;
import net.minecraft.client.world.ClientWorld.Properties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Properties.class)
public class ClientWorldPropertiesMixin {
   @Inject(method = "getTimeOfDay", at = @At("HEAD"), cancellable = true, require = 0)
   private void onGetTimeOfDay(CallbackInfoReturnable<Long> cir) {
      TimeChanger timeChanger = (TimeChanger)ModuleManager.getInstance().getModule("Time Changer");
      if (timeChanger != null && timeChanger.isEnabled()) {
         cir.setReturnValue(timeChanger.getTime());
      }
   }

   @Inject(method = "getTime", at = @At("HEAD"), cancellable = true, require = 0)
   private void onGetTime(CallbackInfoReturnable<Long> cir) {
      TimeChanger timeChanger = (TimeChanger)ModuleManager.getInstance().getModule("Time Changer");
      if (timeChanger != null && timeChanger.isEnabled()) {
         cir.setReturnValue(timeChanger.getTime());
      }
   }
}
