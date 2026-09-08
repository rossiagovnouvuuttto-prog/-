package dev.m3odbydr.client.mixin;

import dev.m3odbydr.client.ClientState;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @ModifyArgs(
        method = "update",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V", ordinal = 0)
    )
    private void m3odbydr$cameraRotation(Args args) {
        if (ClientState.freelook) {
            args.set(0, ClientState.cameraYaw);
            args.set(1, ClientState.cameraPitch);
        }
    }
}
