package dev.m3odbydr.client.mixin;

import dev.m3odbydr.client.ClientState;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void m3odbydr$zoom(Camera camera, float tickProgress, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        if (ClientState.zoom) {
            cir.setReturnValue(Math.max(12.0F, cir.getReturnValue() * 0.35F));
        }
    }
}
