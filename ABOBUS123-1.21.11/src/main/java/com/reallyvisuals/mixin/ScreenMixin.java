package com.reallyvisuals.mixin;

import net.minecraft.client.gui.DrawContext;

import com.reallyvisuals.module.ModuleManager;
import com.reallyvisuals.module.ShulkerPreview;
import com.reallyvisuals.module.ShulkerPreviewRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
   @Inject(
      method = "renderTooltip",
      at = @At("HEAD"),
      cancellable = true,
      require = 0
   )
   private void onRenderTooltip(DrawContext context, ItemStack stack, int x, int y, CallbackInfo ci) {
      ShulkerPreview preview = (ShulkerPreview)ModuleManager.getInstance().getModule("Shulker Preview");
      if (preview != null && preview.isEnabled() && ShulkerPreview.isShulkerBox(stack) && (!preview.onlyShift.value || com.reallyvisuals.utils.KeyUtils.isKeyPressedSafe(net.minecraft.client.MinecraftClient.getInstance().getWindow().getHandle(), 340))) {
         ci.cancel();
         ShulkerPreviewRenderer.renderPreview(context, stack, x + 12, y - 16);
      }
   }
}
