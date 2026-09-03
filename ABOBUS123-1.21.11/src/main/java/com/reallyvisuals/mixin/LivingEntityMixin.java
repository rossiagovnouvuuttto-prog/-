package com.reallyvisuals.mixin;

import com.reallyvisuals.module.ModuleManager;
import com.reallyvisuals.module.NoJumpDelay;
import com.reallyvisuals.module.JumpCircles;
import net.minecraft.util.math.Vec3d;
import com.reallyvisuals.module.RenderTweaks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
   @Shadow
   protected int jumpingCooldown;

   @Inject(method = "jump", at = @At("HEAD"))
   private void abobus123$onJump(CallbackInfo ci) {
      if ((Object)this == MinecraftClient.getInstance().player) {
         JumpCircles jumpCircles = (JumpCircles)ModuleManager.getInstance().getModule("Jump Circles");
         if (jumpCircles != null && jumpCircles.isEnabled()) {
            LivingEntity e = (LivingEntity)(Object)this;
            jumpCircles.onJump(new Vec3d(e.getX(), e.getY(), e.getZ()));
         }
      }
   }

   @Inject(method = "tickMovement", at = @At("HEAD"))
   private void abobus123$onTickMovement(CallbackInfo ci) {
      NoJumpDelay noJumpDelay = (NoJumpDelay) ModuleManager.getInstance().getModule("No Jump Delay");
      if (noJumpDelay != null && noJumpDelay.isEnabled() && (Object)this == MinecraftClient.getInstance().player) {
         this.jumpingCooldown = 0;
      }
   }

   // 1.21.11: hasStatusEffect takes a RegistryEntry, not the effect itself
   @Inject(method = "hasStatusEffect", at = @At("HEAD"), cancellable = true)
   private void abobus123$onHasStatusEffect(RegistryEntry<StatusEffect> effect, CallbackInfoReturnable<Boolean> cir) {
      if (!((Object) this instanceof PlayerEntity)) return;

      RenderTweaks tweaks = (RenderTweaks) ModuleManager.getInstance().getModule("Render Tweaks");
      if (tweaks != null
         && tweaks.isEnabled()
         && tweaks.tweaks.isSelected("Черные сердца")
         && effect != null
         && effect.value() == StatusEffects.WITHER.value()) {
         cir.setReturnValue(false);
      }
   }
}
