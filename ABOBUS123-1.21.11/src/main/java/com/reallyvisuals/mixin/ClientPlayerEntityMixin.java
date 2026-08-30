package com.reallyvisuals.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;

import com.reallyvisuals.module.ModuleManager;
import com.reallyvisuals.module.PvpSafeModule;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayerEntityMixin {
   @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
   private void onSendChatMessage(String message, CallbackInfo ci) {
      PvpSafeModule pvpSafe = (PvpSafeModule)ModuleManager.getInstance().getModule("PVP Safe");
      if (pvpSafe != null && pvpSafe.isEnabled() && pvpSafe.processCommand(message)) {
         ci.cancel();
      }
   }
}
