package com.reallyvisuals.mixin;

import com.reallyvisuals.module.HideACBot;
import com.reallyvisuals.module.PerformanceBoost;
import com.reallyvisuals.utils.RenderCullingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderManager.class)
public abstract class EntityRenderDispatcherMixin {
   @Inject(method = "render", at = @At("HEAD"), cancellable = true)
   private void onRenderEntityHead(
      Entity entity,
      double x,
      double y,
      double z,
      float yaw,
      float tickDelta,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      int light,
      CallbackInfo ci
   ) {
      if (HideACBot.shouldHideEntity(entity)) {
         ci.cancel();
      } else {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.player != null && entity != mc.player && entity != mc.getCameraEntity()) {
            if (RenderCullingHelper.activeFrustum != null) {
               try {
                  if (!RenderCullingHelper.activeFrustum.isVisible(entity.getBoundingBox().expand(0.5))) {
                     ci.cancel();
                     return;
                  }
               } catch (Throwable var18) {
               }
            }

            if ((entity instanceof ItemEntity || entity instanceof ArmorStandEntity || entity instanceof ItemFrameEntity)
               && mc.player.squaredDistanceTo(entity) > PerformanceBoost.smallEntityCullDistanceSquared()) {
               ci.cancel();
               return;
            }
         }
      }
   }
}
