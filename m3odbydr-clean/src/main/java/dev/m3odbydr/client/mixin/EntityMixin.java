package dev.m3odbydr.client.mixin;

import dev.m3odbydr.client.ClientState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void m3odbydr$freelook(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        Entity self = (Entity) (Object) this;
        if (ClientState.freelook && client.player != null && self == client.player) {
            ClientState.cameraYaw += (float) (cursorDeltaX * 0.15D);
            ClientState.cameraPitch = MathHelper.clamp(ClientState.cameraPitch + (float) (cursorDeltaY * 0.15D), -90.0F, 90.0F);
            ci.cancel();
        }
    }
}
