package com.reallyvisuals.module;

import net.minecraft.util.math.RotationAxis;

import com.reallyvisuals.gui.ReallyVisualsScreen;
import com.reallyvisuals.gui.render.RenderUtils3D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

public class ChinaHat extends Module {
   public Module.NumberSetting height = new Module.NumberSetting("Высота", 0.3, 0.1, 1.0, 0.05);
   public Module.NumberSetting opacity = new Module.NumberSetting("Прозрачность", 0.5, 0.1, 1.0, 0.05);
   public Module.BooleanSetting outline = new Module.BooleanSetting("Обводка", true);
   public Module.BooleanSetting clientColor = new Module.BooleanSetting("Цвет клиента", true);

   public ChinaHat() {
      super("China Hat", "Отображение китайской шляпы над головой", Category.VISUALS, false, true);
      this.customColor = -13310721;
      this.addSetting(this.height);
      this.addSetting(this.opacity);
      this.addSetting(this.outline);
      this.addSetting(this.clientColor);
   }

   public void render3D(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Camera camera, float tickDelta) {
      if (this.isEnabled()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.player != null && mc.world != null) {
            ClientPlayerEntity player = mc.player;
            double camX = camera.getCameraPos().x;
            double camY = camera.getCameraPos().y;
            double camZ = camera.getCameraPos().z;
            double playerX = MathHelper.lerp(tickDelta, (float) player.getLastRenderPos().x, (float) player.getX());
            double playerY = MathHelper.lerp(tickDelta, (float) player.getLastRenderPos().y, (float) player.getY());
            double playerZ = MathHelper.lerp(tickDelta, (float) player.getLastRenderPos().z, (float) player.getZ());
            double relX = playerX - camX;
            double relY = playerY + player.getHeight() + 0.02 - camY;
            double relZ = playerZ - camZ;
            float yaw = player.getBodyYaw();
            float pitch = player.getLerpedPitch(tickDelta);
            matrices.push();
            matrices.translate(relX, relY, relZ);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch * 0.15F));
            RenderUtils3D.drawChinaHat(matrices, vertexConsumers, (float)this.height.value, (float)this.opacity.value, this.outline.value, this.clientColor.value ? ReallyVisualsScreen.clientColor : this.customColor);
            matrices.pop();
         }
      }
   }
}
