package com.reallyvisuals.mixin;

import com.reallyvisuals.gui.ReallyVisualsScreen;
import com.reallyvisuals.gui.render.RenderUtils3D;
import com.reallyvisuals.module.ChinaHat;
import com.reallyvisuals.module.ModuleManager;
import com.reallyvisuals.module.PlayerSkins;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {
   @Inject(
      method = "render",
      at = @At("TAIL")
   )
   private void onRenderPlayer(
      AbstractClientPlayerEntity player, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci
   ) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (player != mc.player || !mc.options.getPerspective().isFirstPerson()) {
         ChinaHat chinaHat = (ChinaHat)ModuleManager.getInstance().getModule("China Hat");
         if (chinaHat != null && chinaHat.isEnabled()) {
            matrices.push();
            matrices.translate(0.0, player.getHeight() + 0.05, 0.0);
            RenderUtils3D.drawChinaHat(
               matrices, vertexConsumers, (float)chinaHat.height.value, (float)chinaHat.opacity.value, chinaHat.outline.value, chinaHat.clientColor.value ? ReallyVisualsScreen.clientColor : chinaHat.customColor
            );
            matrices.pop();
         }
      }
   }

   @Inject(
      method = "getTexture",
      at = @At("HEAD"),
      cancellable = true,
      require = 0
   )
   private void abobus123$localSkin(AbstractClientPlayerEntity player, CallbackInfoReturnable<Identifier> cir) {
      PlayerSkins skins = (PlayerSkins)ModuleManager.getInstance().getModule("Player Skins");
      if (skins != null && skins.appliesTo(player)) {
         cir.setReturnValue(skins.getTexture());
      }
   }

   @Inject(
      method = "scale",
      at = @At("TAIL"),
      require = 0
   )
   private void abobus123$localMorph(AbstractClientPlayerEntity player, MatrixStack matrices, float tickDelta, CallbackInfo ci) {
      PlayerSkins skins = (PlayerSkins)ModuleManager.getInstance().getModule("Player Skins");
      if (skins != null && skins.appliesTo(player)) {
         matrices.scale(skins.getScaleX(), skins.getScaleY(), skins.getScaleZ());
      }
   }

}
