package com.reallyvisuals.mixin;

import com.reallyvisuals.module.HitSounds;
import com.reallyvisuals.module.HitMarker;
import com.reallyvisuals.module.ComboCounter;
import com.reallyvisuals.module.FriendManager;
import com.reallyvisuals.module.FriendSystem;
import com.reallyvisuals.module.ModuleManager;
import com.reallyvisuals.module.ShiftTab;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
   @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
   private void onAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
      FriendSystem fs = (FriendSystem)ModuleManager.getInstance().getModule("Настройки друзей");
      if (target instanceof PlayerEntity && fs != null && fs.isEnabled() && fs.noHitFriends.value && FriendManager.isFriend(target.getNameForScoreboard())) {
         ci.cancel();
         return;
      }
      if (player != null && target != null && target instanceof LivingEntity) {
         boolean isCrit = player.fallDistance > 0.0F
            && !player.isOnGround()
            && !player.isClimbing()
            && !player.isTouchingWater()
            && !player.hasStatusEffect(StatusEffects.BLINDNESS)
            && !player.hasVehicle();
         if (ShiftTab.INSTANCE != null) {
            ShiftTab.INSTANCE.onAttack(isCrit);
         }

         HitSounds hitSounds;
         if ((hitSounds = (HitSounds)ModuleManager.getInstance().getModule("Hit Sounds")) != null && hitSounds.isEnabled()) {
            hitSounds.onAttack(isCrit);
         }

         HitMarker hitMarker = (HitMarker)ModuleManager.getInstance().getModule("Hit Marker");
         if (hitMarker != null && hitMarker.isEnabled()) {
            hitMarker.onAttack();
         }

         ComboCounter comboCounter = (ComboCounter)ModuleManager.getInstance().getModule("Combo Counter");
         if (comboCounter != null && comboCounter.isEnabled()) {
            comboCounter.onAttack((LivingEntity)target);
         }
      }
   }
}
