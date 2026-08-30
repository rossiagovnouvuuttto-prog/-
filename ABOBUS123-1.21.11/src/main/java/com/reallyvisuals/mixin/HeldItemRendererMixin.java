package com.reallyvisuals.mixin;

import net.minecraft.util.math.RotationAxis;

import com.reallyvisuals.module.CustomHand;
import com.reallyvisuals.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
   @Inject(method = "applyEquipOffset", at = @At("HEAD"), cancellable = true)
   private void onApplyEquipOffset(MatrixStack matrices, Arm arm, float equipProgress, CallbackInfo ci) {
      CustomHand customHand = (CustomHand)ModuleManager.getInstance().getModule("Custom Hand");
      if (customHand != null && customHand.isEnabled()) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client.player == null) {
            return;
         }

         int side = arm == Arm.RIGHT ? 1 : -1;
         boolean isMainHand = arm == client.player.getMainArm();
         float scale = (float)(isMainHand ? customHand.mainHandScale.value : customHand.offHandScale.value);
         float offX = (float)(isMainHand ? customHand.mainHandX.value : customHand.offHandX.value);
         float offY = (float)(isMainHand ? customHand.mainHandY.value : customHand.offHandY.value);
         float offZ = (float)(isMainHand ? customHand.mainHandZ.value : customHand.offHandZ.value);
         matrices.translate(side * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
         matrices.scale(scale, scale, scale);
         matrices.translate(offX, offY, offZ);
         ci.cancel();
      }
   }

   @Inject(method = "applySwingOffset", at = @At("HEAD"), cancellable = true)
   private void onApplySwingOffset(MatrixStack matrices, Arm arm, float swingProgress, CallbackInfo ci) {
      CustomHand customHand = (CustomHand)ModuleManager.getInstance().getModule("Custom Hand");
      if (customHand != null && customHand.isEnabled() && !"Без анимации".equals(customHand.animationMode.value)) {
         int side = arm == Arm.RIGHT ? 1 : -1;
         float swingStr = (float)customHand.swingStrength.value;
         float fSin = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
         float fSinSquare = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
         switch (customHand.animationMode.value) {
            case "Наклон":
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * fSin * swingStr * 3.5F));
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * fSinSquare * -(swingStr * 2.5F)));
               break;
            case "Взмах":
               matrices.translate(side * fSin * 0.08F, -fSinSquare * 0.08F, -fSin * 0.12F);
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-fSin * swingStr * 5.0F));
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * fSinSquare * swingStr * 3.0F));
               break;
            case "Вращение":
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * fSin * swingStr * 16.0F));
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * fSinSquare * swingStr * 10.0F));
               break;
            case "Увеличение":
               float scaleG = 1.0F + fSin * 0.18F;
               matrices.scale(scaleG, scaleG, scaleG);
               break;
            case "Уменьшение":
               float scaleS = 1.0F - fSin * 0.16F;
               matrices.scale(scaleS, scaleS, scaleS);
               break;
            case "Растяжение":
               matrices.scale(1.0F + fSin * 0.12F, 1.0F - fSin * 0.08F, 1.0F + fSinSquare * 0.18F);
               break;
            case "Пружина":
               matrices.translate(0.0, MathHelper.sin(swingProgress * (float) Math.PI * 2.0F) * 0.1F, 0.0);
               break;
            case "Удар":
               matrices.translate(0.0, 0.0, -fSin * 0.25F);
               break;
            case "Разрез":
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-fSin * 45.0F));
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * fSin * 30.0F));
               break;
            case "Рывок":
               matrices.translate(side * fSin * 0.15F, 0.0, -fSin * 0.2F);
               break;
            default:
               matrices.multiply(
                  RotationAxis.POSITIVE_Y.rotationDegrees(side * (45.0F + MathHelper.sin(swingProgress * swingProgress * (float) Math.PI) * (-swingStr * 4.0F)))
               );
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * fSin * (-swingStr * 4.0F)));
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(fSin * (-swingStr * 16.0F)));
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * -45.0F));
         }

         ci.cancel();
      }
   }
}
