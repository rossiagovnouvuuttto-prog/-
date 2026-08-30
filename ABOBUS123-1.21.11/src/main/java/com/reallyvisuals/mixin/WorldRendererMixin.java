package com.reallyvisuals.mixin;

import com.reallyvisuals.gui.ReallyVisualsScreen;
import com.reallyvisuals.module.BlockOverlay;
import com.reallyvisuals.module.ChunkAnimator;
import com.reallyvisuals.module.ModuleManager;
import com.reallyvisuals.module.PerformanceBoost;
import com.reallyvisuals.utils.RenderCullingHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder.BuiltChunk;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
   /**
    * 1.21.11: setupTerrain is gone; setupFrustum returns the frustum directly,
    * so the culling helper is fed from the return value instead of an argument.
    */
   @Inject(method = "setupFrustum", at = @At("RETURN"), require = 0)
   private void onSetupFrustum(CallbackInfoReturnable<Frustum> cir) {
      RenderCullingHelper.activeFrustum = cir.getReturnValue();
   }

   /**
    * 1.21.11: the outline target is now described by OutlineRenderState.
    * renderWeather and renderSky no longer exist on WorldRenderer; weather is
    * suppressed through World.setRainGradient in RenderTweaks instead, and the
    * CustomWorld skybox has no cancellable hook on this version.
    */
   @Inject(method = "drawBlockOutline", at = @At("HEAD"), cancellable = true, require = 0)
   private void onDrawBlockOutline(
      net.minecraft.client.util.math.MatrixStack matrices,
      net.minecraft.client.render.VertexConsumer vertexConsumer,
      double cameraX, double cameraY, double cameraZ,
      net.minecraft.client.render.state.OutlineRenderState state,
      int color, float alpha, CallbackInfo ci
   ) {
      BlockOverlay overlay = (BlockOverlay) ModuleManager.getInstance().getModule("Block Overlay");
      if (overlay != null && overlay.isEnabled()) {
         ci.cancel();
      }
   }
}
