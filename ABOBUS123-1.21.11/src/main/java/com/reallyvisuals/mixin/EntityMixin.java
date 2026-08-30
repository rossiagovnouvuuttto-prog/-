package com.reallyvisuals.mixin;

import com.reallyvisuals.module.FreeLook;
import com.reallyvisuals.module.FriendESP;
import com.reallyvisuals.module.FriendManager;
import com.reallyvisuals.module.ModuleManager;
import com.reallyvisuals.module.RenderTweaks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
   @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
   private void abobus123$changeLookDirection(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
      if (FreeLook.active && (Object) this instanceof ClientPlayerEntity) {
         MinecraftClient mc = MinecraftClient.getInstance();
         double f = mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2;
         double g = f * f * f * 8.0;
         FreeLook.cameraYaw += (float) (cursorDeltaX * g * 0.15);
         FreeLook.cameraPitch = MathHelper.clamp((float) (FreeLook.cameraPitch + cursorDeltaY * g * 0.15), -90.0F, 90.0F);
         ci.cancel();
      }
   }

   @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
   private void abobus123$onIsGlowing(CallbackInfoReturnable<Boolean> cir) {
      if (!((Object) this instanceof PlayerEntity)) return;
      PlayerEntity player = (PlayerEntity)(Object)this;
      FriendESP friendESP = (FriendESP)ModuleManager.getInstance().getModule("Friend ESP");
      if (friendESP != null && friendESP.isEnabled() && FriendManager.isFriend(player.getNameForScoreboard())) {
         cir.setReturnValue(true);
         return;
      }

      RenderTweaks tweaks = (RenderTweaks) ModuleManager.getInstance().getModule("Render Tweaks");
      if (tweaks != null && tweaks.isEnabled() && tweaks.tweaks.isSelected("Свечение игроков")) {
         cir.setReturnValue(true);
      }
   }
}
