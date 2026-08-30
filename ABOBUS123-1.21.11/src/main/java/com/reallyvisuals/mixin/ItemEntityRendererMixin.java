package com.reallyvisuals.mixin;

import com.reallyvisuals.module.ModuleManager;
import com.reallyvisuals.module.ShulkerPreview;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public class ItemEntityRendererMixin {
   @Inject(method = "render", at = @At("HEAD"))
   private void onRenderHead(
      ItemEntity itemEntity, float f, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci
   ) {
      ShulkerPreview shulkerPreview = (ShulkerPreview)ModuleManager.getInstance().getModule("Shulker Preview");
      ItemStack stack;
      if (shulkerPreview != null
         && shulkerPreview.isEnabled()
         && shulkerPreview.showInWorld.value
         && ShulkerPreview.isShulkerBox(stack = itemEntity.getStack())
         && ShulkerPreview.hasItems(stack)) {
         matrixStack.push();
         matrixStack.scale(1.8F, 1.8F, 1.8F);
      }
   }

   @Inject(method = "render", at = @At("RETURN"))
   private void onRenderReturn(
      ItemEntity itemEntity, float f, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci
   ) {
      ShulkerPreview shulkerPreview = (ShulkerPreview)ModuleManager.getInstance().getModule("Shulker Preview");
      ItemStack stack;
      if (shulkerPreview != null
         && shulkerPreview.isEnabled()
         && shulkerPreview.showInWorld.value
         && ShulkerPreview.isShulkerBox(stack = itemEntity.getStack())
         && ShulkerPreview.hasItems(stack)) {
         matrixStack.pop();
      }
   }
}
