package com.reallyvisuals.mixin;

import com.reallyvisuals.module.HideACBot;
import com.reallyvisuals.module.PerformanceBoost;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.21.11: render() only ever sees an EntityRenderState, so the old injection
 * could not reach the entity and was dropped from every mixin config -- which
 * silently killed Hide AC Bot and FPS Boost's entity culling. shouldRender still
 * takes the Entity and decides whether it is drawn at all, which is a better fit
 * than cancelling render() ever was.
 */
@Mixin(EntityRenderManager.class)
public abstract class EntityRenderDispatcherMixin {

   @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
   private void abobus123$hideEntity(
      Entity entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir
   ) {
      if (HideACBot.shouldHideEntity(entity)) {
         cir.setReturnValue(false);
         return;
      }

      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player == null || entity == mc.player || entity == mc.getCameraEntity()) {
         return;
      }

      // vanilla already frustum-culls here, so only the distance cut is ours
      if ((entity instanceof ItemEntity || entity instanceof ArmorStandEntity || entity instanceof ItemFrameEntity)
         && mc.player.squaredDistanceTo(entity) > PerformanceBoost.smallEntityCullDistanceSquared()) {
         cir.setReturnValue(false);
      }
   }
}
