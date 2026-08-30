package com.reallyvisuals.module;

import com.reallyvisuals.gui.ReallyVisualsScreen;
import com.reallyvisuals.gui.render.RenderUtils3D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

public class BlockOverlay extends Module {
   public Module.BooleanSetting outline = new Module.BooleanSetting("Обводка", true);
   public Module.BooleanSetting fill = new Module.BooleanSetting("Заливка", true);
   public Module.BooleanSetting clientColor = new Module.BooleanSetting("Цвет клиента", true);

   public BlockOverlay() {
      super("Block Overlay", "Кастомная подсветка блока", Category.VISUALS, false, true);
      this.customColor = -13310721;
      this.addSetting(this.outline);
      this.addSetting(this.fill);
      this.addSetting(this.clientColor);
   }

   public void render3D(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Camera camera) {
      if (this.isEnabled()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.player != null && mc.world != null && mc.crosshairTarget != null) {
            if (mc.crosshairTarget.getType() == Type.BLOCK) {
               BlockHitResult blockHitResult = (BlockHitResult)mc.crosshairTarget;
               BlockPos pos = blockHitResult.getBlockPos();
               if (mc.world.getBlockState(pos).isAir()) {
                  return;
               }

               VoxelShape shape = mc.world.getBlockState(pos).getOutlineShape(mc.world, pos);
               if (shape.isEmpty()) {
                  return;
               }

               Vec3d camPos = camera.getCameraPos();
               double relX = pos.getX() - camPos.x;
               double relY = pos.getY() - camPos.y;
               double relZ = pos.getZ() - camPos.z;
               Box box = shape.getBoundingBox();
               matrices.push();
               matrices.translate(relX, relY, relZ);
               RenderUtils3D.drawBlockOverlay(matrices, vertexConsumers, box, this.outline.value, this.fill.value, this.clientColor.value ? ReallyVisualsScreen.clientColor : this.customColor);
               matrices.pop();
            }
         }
      }
   }
}
