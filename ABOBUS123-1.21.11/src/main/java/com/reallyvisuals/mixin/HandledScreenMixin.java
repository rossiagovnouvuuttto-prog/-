package com.reallyvisuals.mixin;

import net.minecraft.client.gui.DrawContext;

import com.reallyvisuals.utils.KeyUtils;

import com.reallyvisuals.module.ItemScroller;
import com.reallyvisuals.module.LockSlot;
import com.reallyvisuals.module.ModuleManager;
import com.reallyvisuals.module.ShulkerPreview;
import com.reallyvisuals.module.ShulkerPreviewRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
   @Shadow
   protected Slot focusedSlot;
   private long lastScrollTime = 0L;

   @Inject(method = "render", at = @At("TAIL"))
   private void onRenderTail(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      this.handleItemScroller();
   }

   @Inject(method = "drawMouseoverTooltip", at = @At("HEAD"), cancellable = true)
   private void onDrawMouseoverTooltip(DrawContext context, int x, int y, CallbackInfo ci) {
      ShulkerPreview preview = (ShulkerPreview)ModuleManager.getInstance().getModule("Shulker Preview");
      ItemStack stack;
      if (preview != null
         && preview.isEnabled()
         && this.focusedSlot != null
         && this.focusedSlot.hasStack()
         && ShulkerPreview.isShulkerBox(stack = this.focusedSlot.getStack())
         && (!preview.onlyShift.value || com.reallyvisuals.utils.KeyUtils.isKeyPressedSafe(net.minecraft.client.MinecraftClient.getInstance().getWindow().getHandle(), 340))) {
         ci.cancel();
         ShulkerPreviewRenderer.renderPreview(context, stack, x + 12, y - 16);
      }
   }

   @Inject(
      method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V",
      at = @At("HEAD"),
      cancellable = true,
      require = 0
   )
   private void onSlotClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
      LockSlot lockSlot = (LockSlot)ModuleManager.getInstance().getModule("Lock Slot");
      if (lockSlot != null && lockSlot.isEnabled() && (actionType == SlotActionType.THROW || slotId == -999) && slot != null && lockSlot.isSlotLocked(slot.id)) {
         ci.cancel();
      }
   }

   private void handleItemScroller() {
      ItemScroller scroller = (ItemScroller)ModuleManager.getInstance().getModule("Item Scroller");
      if (scroller != null && scroller.isEnabled()) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client.player == null
            || client.currentScreen == null
            || this.focusedSlot == null
            || !this.focusedSlot.hasStack()
            || this.focusedSlot.getStack().getItem() == Items.AIR) {
            return;
         }

         long window = client.getWindow().getHandle();
         boolean isLeftDown = KeyUtils.isMouseButtonPressedSafe(window, 0);
         boolean isShiftDown = KeyUtils.isKeyPressedSafe(window, 340) || KeyUtils.isKeyPressedSafe(window, 344) || com.reallyvisuals.utils.KeyUtils.isKeyPressedSafe(net.minecraft.client.MinecraftClient.getInstance().getWindow().getHandle(), 340);
         long delayMs;
         long now;
         if (isLeftDown && isShiftDown && (now = System.currentTimeMillis()) - this.lastScrollTime >= (delayMs = (long)scroller.delaySetting.value)) {
            client.interactionManager
               .clickSlot(((HandledScreen)(Object)this).getScreenHandler().syncId, this.focusedSlot.id, 0, SlotActionType.QUICK_MOVE, client.player);
            this.lastScrollTime = now;
         }
      }
   }
}
