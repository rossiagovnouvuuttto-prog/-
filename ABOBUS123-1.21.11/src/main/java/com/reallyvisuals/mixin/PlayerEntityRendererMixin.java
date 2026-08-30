package com.reallyvisuals.mixin;

import com.reallyvisuals.module.ModuleManager;
import com.reallyvisuals.module.PlayerSkins;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.21.11: PlayerEntityRenderer no longer declares render(); the inherited
 * LivingEntityRenderer.render() takes a render state and an
 * OrderedRenderCommandQueue, so China Hat is drawn from WorldRenderHandler
 * (WorldRenderEvents.AFTER_ENTITIES) instead. getTexture/scale still exist
 * here, but keyed on PlayerEntityRenderState rather than the entity.
 */
@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {

   @Inject(
      method = "getTexture(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)Lnet/minecraft/util/Identifier;",
      at = @At("HEAD"),
      cancellable = true
   )
   private void abobus123$localSkin(PlayerEntityRenderState state, CallbackInfoReturnable<Identifier> cir) {
      PlayerSkins skins = (PlayerSkins) ModuleManager.getInstance().getModule("Player Skins");
      if (skins != null && skins.appliesTo(state.id)) {
         cir.setReturnValue(skins.getTexture());
      }
   }

   @Inject(
      method = "scale(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;)V",
      at = @At("TAIL")
   )
   private void abobus123$localMorph(PlayerEntityRenderState state, MatrixStack matrices, CallbackInfo ci) {
      PlayerSkins skins = (PlayerSkins) ModuleManager.getInstance().getModule("Player Skins");
      if (skins != null && skins.appliesTo(state.id)) {
         matrices.scale(skins.getScaleX(), skins.getScaleY(), skins.getScaleZ());
      }
   }
}
