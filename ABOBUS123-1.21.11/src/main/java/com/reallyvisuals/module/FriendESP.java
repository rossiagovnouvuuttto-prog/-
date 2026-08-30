package com.reallyvisuals.module;

import com.reallyvisuals.gui.render.RenderUtils3D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class FriendESP extends Module {
   public Module.BooleanSetting tracers = new Module.BooleanSetting("Трасера к друзьям", true);
   public Module.BooleanSetting boxes = new Module.BooleanSetting("Боксы", true);

   public FriendESP() {
      super("Friend ESP", "Выделение друзей", Category.FRIENDS, false, true);
      this.addSetting(this.tracers);
      this.addSetting(this.boxes);
   }

   public void onRender3D(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Camera camera, float tickDelta) {
      if (!this.isEnabled()) return;
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.world == null || mc.player == null) return;
      Vec3d cam = camera.getCameraPos();
      matrices.push();
      matrices.translate(-cam.x, -cam.y, -cam.z);
      for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
         if (p == mc.player || !FriendManager.isFriend(p.getNameForScoreboard())) continue;
         if (this.boxes.value) {
            Box b = p.getBoundingBox().expand(0.04);
            RenderUtils3D.drawOutlineBox(matrices, vertexConsumers, b, 0.2F, 1.0F, 0.35F, 1.0F);
         }
         if (this.tracers.value) {
            Vec3d start = mc.player.getCameraPosVec(tickDelta);
            Vec3d end = new Vec3d(p.getX(), p.getBodyY(0.5), p.getZ());
            RenderUtils3D.draw3DLine(matrices, vertexConsumers, start, end, 0.2F, 1.0F, 0.35F, 0.9F);
         }
      }
      matrices.pop();
   }
}
