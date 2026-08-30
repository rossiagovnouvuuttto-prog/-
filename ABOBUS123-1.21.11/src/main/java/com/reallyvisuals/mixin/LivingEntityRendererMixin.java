package com.reallyvisuals.mixin;

import com.reallyvisuals.module.HitColor;
import com.reallyvisuals.module.ModuleManager;
import com.reallyvisuals.module.SelfNametag;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
   @Redirect(
      method = "render",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/model/Model;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"
      )
   )
   private void redirectModelRender(
      EntityModel model,
      MatrixStack matrices,
      VertexConsumer vertices,
      int light,
      int overlay,
      float red,
      float green,
      float blue,
      float alpha,
      net.minecraft.entity.LivingEntity livingEntity,
      float f,
      float g,
      MatrixStack matrixStack,
      VertexConsumerProvider vertexConsumerProvider,
      int i
   ) {
      HitColor hitColor = (HitColor)ModuleManager.getInstance().getModule("Hit Color");
      if (hitColor != null && hitColor.isEnabled() && livingEntity.hurtTime > 0) {
         int color = hitColor.getColor();
         red = (color >> 16 & 0xFF) / 255.0F;
         green = (color >> 8 & 0xFF) / 255.0F;
         blue = (color & 0xFF) / 255.0F;
         if (color == -16448251 || color == -16777216) {
            red = 0.05F;
            green = 0.05F;
            blue = 0.05F;
         }
      }

      int argb = ((int) (alpha * 255.0F) << 24) | ((int) (red * 255.0F) << 16)
            | ((int) (green * 255.0F) << 8) | (int) (blue * 255.0F);
      model.render(matrices, vertices, light, overlay, argb);
   }

   @Inject(method = "hasLabel", at = @At("HEAD"), cancellable = true, require = 0)
   private void onHasLabel(net.minecraft.entity.LivingEntity livingEntity, double d, CallbackInfoReturnable<Boolean> cir) {
      SelfNametag selfNametag = (SelfNametag)ModuleManager.getInstance().getModule("Self Nametag");
      if (selfNametag != null && selfNametag.isEnabled()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (livingEntity == mc.player && !mc.options.getPerspective().isFirstPerson()) {
            cir.setReturnValue(true);
         }
      }
   }
}
