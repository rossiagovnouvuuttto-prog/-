package com.reallyvisuals.mixin;

import com.reallyvisuals.module.CustomWorld;
import com.reallyvisuals.module.HitSounds;
import com.reallyvisuals.module.ModuleManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {
   // 1.21.11: ClientWorld.getSkyColor is gone; sky colour moved into the render pipeline.

   @Inject(
      method = "playSound(DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FFZ)V",
      at = @At("HEAD"),
      cancellable = true,
      require = 0
   )
   private void abobus123$onPlaySound(
      double x, double y, double z, SoundEvent sound, SoundCategory category,
      float volume, float pitch, boolean useDistance, CallbackInfo ci
   ) {
      HitSounds hitSounds = (HitSounds) ModuleManager.getInstance().getModule("Hit Sounds");
      if (hitSounds != null
         && hitSounds.isEnabled()
         && hitSounds.removeVanillaCrit.value
         && (sound == SoundEvents.ENTITY_PLAYER_ATTACK_CRIT
            || sound == SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK
            || sound == SoundEvents.ENTITY_PLAYER_ATTACK_STRONG)) {
         ci.cancel();
      }
   }
}
