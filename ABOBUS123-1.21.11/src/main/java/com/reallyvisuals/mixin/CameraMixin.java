package com.reallyvisuals.mixin;

import com.reallyvisuals.module.FreeLook;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
   @Shadow
   protected abstract void setRotation(float yaw, float pitch);

   @Inject(method = "update", at = @At("TAIL"))
   private void abobus123$applyFreeLook(
      World area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci
   ) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (FreeLook.active && focusedEntity == mc.player) {
         this.setRotation(FreeLook.cameraYaw, FreeLook.cameraPitch);
      }
   }
}
