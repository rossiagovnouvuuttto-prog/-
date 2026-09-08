package dev.m3odbydr.client.mixin;

import dev.m3odbydr.client.ClientState;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightmapTextureManager.class)
public abstract class LightmapTextureManagerMixin {
    @Inject(method = "getBrightness(FI)F", at = @At("HEAD"), cancellable = true)
    private static void m3odbydr$brightnessAmbient(float ambientLight, int lightLevel, CallbackInfoReturnable<Float> cir) {
        if (ClientState.fullbright) cir.setReturnValue(1.0F);
    }

    @Inject(method = "getBrightness(Lnet/minecraft/world/dimension/DimensionType;I)F", at = @At("HEAD"), cancellable = true)
    private static void m3odbydr$brightnessDimension(DimensionType type, int lightLevel, CallbackInfoReturnable<Float> cir) {
        if (ClientState.fullbright) cir.setReturnValue(1.0F);
    }
}
