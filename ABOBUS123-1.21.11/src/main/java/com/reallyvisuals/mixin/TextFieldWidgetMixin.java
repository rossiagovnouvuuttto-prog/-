package com.reallyvisuals.mixin;

import net.minecraft.client.gui.DrawContext;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reallyvisuals.module.ModuleManager;
import com.reallyvisuals.module.StreamerMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextFieldWidget.class)
public abstract class TextFieldWidgetMixin {
   @Shadow
   private String text;
   @Shadow
   private boolean drawsBackground;

   @Inject(method = "renderWidget", at = @At("TAIL"))
   private void onRenderButtonTail(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      StreamerMode mode = (StreamerMode)ModuleManager.getInstance().getModule("Streamer Mode");
      if (mode != null && mode.isEnabled() && mode.blurPassword.value && this.text != null) {
         TextFieldWidget widget = (TextFieldWidget)(Object)this;
         String lower = this.text.toLowerCase().trim();
         String[] cmds = new String[]{"/l ", "/login ", "/reg ", "/register ", "/pas ", "/password "};
         String matchedCmd = null;

         for (String c : cmds) {
            if (lower.startsWith(c.trim() + " ")) {
               matchedCmd = c;
               break;
            }
         }

         if (matchedCmd != null && this.text.length() > matchedCmd.length()) {
            MinecraftClient mc = MinecraftClient.getInstance();
            int spaceIdx = this.text.indexOf(32);
            if (spaceIdx > 0 && spaceIdx < this.text.length() - 1) {
               String cmdPart = this.text.substring(0, spaceIdx + 1);
               String passPart = this.text.substring(spaceIdx + 1);
               int startX = widget.getX() + (this.drawsBackground ? 4 : 0);
               int startY = widget.getY() + (widget.getHeight() - 8) / 2;
               int cmdWidth = mc.textRenderer.getWidth(cmdPart);
               int passWidth = mc.textRenderer.getWidth(passPart);
               int bx1 = startX + cmdWidth - 2;
               int bx2 = bx1 + passWidth + 6;
               int by1 = widget.getY() + 2;
               int by2 = widget.getY() + widget.getHeight() - 2;
               context.getMatrices().pushMatrix();
               context.getMatrices().translate((float) (0.0), (float) (0.0));
               context.fill( bx1 - 1, by1 - 1, bx2 + 1, by2 + 1, -2075392);
               context.fill( bx1, by1, bx2, by2, -7852032);
               context.getMatrices().popMatrix();
            }
         }
      }
   }
}
