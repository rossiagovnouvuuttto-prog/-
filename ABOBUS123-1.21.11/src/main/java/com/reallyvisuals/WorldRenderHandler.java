package com.reallyvisuals;

import com.reallyvisuals.gui.render.RenderUtils3D;
import com.reallyvisuals.module.*;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Replaces the old WorldRenderer render() injection: same logic, official entry point. */
public final class WorldRenderHandler {

   public static void register() {
      WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.world == null || mc.player == null) return;

         MatrixStack matrices = ctx.matrices();
         VertexConsumerProvider vc = ctx.consumers();
         Camera camera = mc.gameRenderer.getCamera();
         float tickDelta = mc.getRenderTickCounter().getTickProgress(false);
         Vec3d camPos = camera.getCameraPos();
         ModuleManager mm = ModuleManager.getInstance();

         EntityBoxes eb = (EntityBoxes) mm.getModule("Entity Boxes");
         if (eb != null && eb.isEnabled()) {
            int color = eb.getColor();
            float r = (color >> 16 & 0xFF) / 255.0F;
            float g = (color >> 8 & 0xFF) / 255.0F;
            float b = (color & 0xFF) / 255.0F;
            float fillAlpha = (float) eb.fillOpacity.value;
            boolean firstPerson = mc.options.getPerspective().isFirstPerson();

            matrices.push();
            matrices.translate(-camPos.x, -camPos.y, -camPos.z);
            double cullSq = PerformanceBoost.smallEntityCullDistanceSquared();
            for (Entity entity : mc.world.getEntities()) {
               if (entity != mc.player && mc.player.squaredDistanceTo(entity) > cullSq) continue;
               if ((entity != mc.player || !firstPerson) && entity instanceof LivingEntity) {
                  Box box = entity.getBoundingBox();
                  if (eb.fill.value) RenderUtils3D.drawFilledBox(matrices, vc, box, r, g, b, fillAlpha);
                  if ("\u0423\u0433\u043e\u043b\u043a\u0438".equals(eb.style.value)) {
                     RenderUtils3D.drawCornerBox(matrices, vc, box, r, g, b, 1.0F, 0.25F);
                  } else {
                     RenderUtils3D.drawOutlineBox(matrices, vc, box, r, g, b, 1.0F);
                  }
                  if (eb.lookVector.value) {
                     Vec3d eye = new Vec3d(entity.getX(), entity.getEyeY(), entity.getZ());
                     Vec3d end = eye.add(entity.getRotationVec(tickDelta).multiply(eb.rayLength.value));
                     RenderUtils3D.draw3DLine(matrices, vc, eye, end, r, g, b, 1.0F);
                  }
               }
            }
            matrices.pop();
         }

         JumpCircles jc = (JumpCircles) mm.getModule("Jump Circles");
         if (jc != null && jc.isEnabled()) jc.renderWorld(matrices, vc, camera);

         Slipstream sl = (Slipstream) mm.getModule("Slipstream");
         if (sl != null && sl.isEnabled()) {
            matrices.push();
            matrices.translate(-camPos.x, -camPos.y, -camPos.z);
            sl.onRender3D(matrices, vc, tickDelta);
            matrices.pop();
         }

         WorldParticles wp = (WorldParticles) mm.getModule("World Particles");
         if (wp != null && wp.isEnabled()) {
            matrices.push();
            matrices.translate(-camPos.x, -camPos.y, -camPos.z);
            wp.onRender3D(matrices, vc, tickDelta);
            matrices.pop();
         }

         Trails tr = (Trails) mm.getModule("Trails");
         if (tr != null && tr.isEnabled()) tr.onRender3D(matrices, vc, camera, tickDelta);

         FriendESP fe = (FriendESP) mm.getModule("Friend ESP");
         if (fe != null && fe.isEnabled()) fe.onRender3D(matrices, vc, camera, tickDelta);

         TargetESP te = (TargetESP) mm.getModule("Target ESP");
         if (te != null && te.isEnabled()) te.onRender3D(matrices, vc, camera, tickDelta);

         Waypoints wpt = (Waypoints) mm.getModule("Waypoints");
         if (wpt != null && wpt.isEnabled()) wpt.onRender3D(matrices, vc, camera, tickDelta);
      });
   }
}
