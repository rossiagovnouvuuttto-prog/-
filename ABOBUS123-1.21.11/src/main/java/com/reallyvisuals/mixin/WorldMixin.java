package com.reallyvisuals.mixin;

import com.reallyvisuals.module.ModuleManager;
import com.reallyvisuals.module.RenderTweaks;
import com.reallyvisuals.module.TimeChanger;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class WorldMixin {
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

   @Inject(method = "getRainGradient", at = @At("HEAD"), cancellable = true, require = 0)
   private void onGetRainGradient(float delta, CallbackInfoReturnable<Float> cir) {
      RenderTweaks tweaks = (RenderTweaks)ModuleManager.getInstance().getModule("Render Tweaks");
      if (tweaks != null && tweaks.isEnabled() && tweaks.tweaks.isSelected("Погода")) {
         cir.setReturnValue(0.0F);
      }
   }

   @Inject(method = "getThunderGradient", at = @At("HEAD"), cancellable = true, require = 0)
   private void onGetThunderGradient(float delta, CallbackInfoReturnable<Float> cir) {
      RenderTweaks tweaks = (RenderTweaks)ModuleManager.getInstance().getModule("Render Tweaks");
      if (tweaks != null && tweaks.isEnabled() && tweaks.tweaks.isSelected("Погода")) {
         cir.setReturnValue(0.0F);
      }
   }
}
