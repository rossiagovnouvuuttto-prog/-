package com.reallyvisuals.gui;

import com.reallyvisuals.utils.AnimationUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class ReportConfirmationScreen extends Screen {
   private final String targetPlayer;
    private final AnimationUtils.Animation openYAnim = new AnimationUtils.Animation(24.0F);

   public ReportConfirmationScreen(String targetPlayer) {
      super(Text.literal("Подтверждение репорта"));
      this.targetPlayer = targetPlayer;
   }

   protected void init() {
      super.init();
      this.openYAnim.force(24.0F);
      this.openYAnim.setEasing(1);
      this.openYAnim.animateTo(0.0F, 350L);
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      // 1.21.11 allows exactly one blur per frame and vanilla already spends it
      // before render() is called; renderBackground() would blur a second time and
      // throw "Can only blur once per frame". renderInGameBackground is the
      // darkening half without the blur.
      this.renderInGameBackground(context);
      int w = 260;
      int h = 140;
      int x = (this.width - w) / 2;
      int y = (this.height - h) / 2;
      context.getMatrices().pushMatrix();
      context.getMatrices().translate((float) (0.0), (float) (0.0));
      context.getMatrices().translate((float) (0.0), (float) (this.openYAnim.getValue()));
      context.fill( x, y, x + w, y + h, -300542437);
      context.fill( x - 1, y - 1, x + w + 1, y + h + 1, 1157627903);
      String title = "Подтверждение репорта";
      int tw = this.textRenderer.getWidth(title);
      context.drawTextWithShadow(this.textRenderer, title, (int) ((int) (x + (w - tw) / 2.0F)), y + 16, -1);
      int nw = this.textRenderer.getWidth(this.targetPlayer);
      context.drawTextWithShadow(this.textRenderer, this.targetPlayer, (int) ((int) (x + (w - nw) / 2.0F)), y + 42, -35072);
      String sub = "Отправить репорт за читы?";
      int sw = this.textRenderer.getWidth(sub);
      context.drawTextWithShadow(this.textRenderer, sub, (int) ((int) (x + (w - sw) / 2.0F)), y + 62, -1996488705);
      int bw = 100;
      int bh = 26;
      int bx1 = x + 20;
      int bx2 = x + w - 20 - bw;
      int by = y + 90;
      boolean hoverYes = mouseX >= bx1 && mouseX <= bx1 + bw && mouseY >= by && mouseY <= by + bh;
      boolean hoverNo = mouseX >= bx2 && mouseX <= bx2 + bw && mouseY >= by && mouseY <= by + bh;
      context.fill( bx1, by, bx1 + bw, by + bh, hoverYes ? -13421762 : -14540246);
      int yw = this.textRenderer.getWidth("Да");
      context.drawTextWithShadow(this.textRenderer, "Да", (int) ((int) (bx1 + (bw - yw) / 2.0F)), by + 9, -1);
      context.fill( bx2, by, bx2 + bw, by + bh, hoverNo ? -13421762 : -14540246);
      int nwBtn = this.textRenderer.getWidth("Нет");
      context.drawTextWithShadow(this.textRenderer, "Нет", (int) ((int) (bx2 + (bw - nwBtn) / 2.0F)), by + 9, -1);
      context.getMatrices().popMatrix();

      super.render(context, mouseX, mouseY, delta);
   }

   public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
      double mouseX = click.x(); double mouseY = click.y(); int button = click.button();
      float openSlide = this.openYAnim.getValue();
      if (openSlide > 0.5F) {
         mouseY -= openSlide;
      }

      int w = 260;
      int h = 140;
      int x = (this.width - w) / 2;
      int y = (this.height - h) / 2;
      int bw = 100;
      int bh = 26;
      int bx1 = x + 20;
      int bx2 = x + w - 20 - bw;
      int by = y + 90;
      if (mouseX >= bx1 && mouseX <= bx1 + bw && mouseY >= by && mouseY <= by + bh) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.player != null) {
            mc.player.networkHandler.sendChatMessage("/report " + this.targetPlayer + " cheat");
         }

         this.close();
         return true;
      } else if (mouseX >= bx2 && mouseX <= bx2 + bw && mouseY >= by && mouseY <= by + bh) {
         this.close();
         return true;
      } else {
         return super.mouseClicked(click, doubled);
      }
   }

   public boolean isPauseScreen() {
      return false;
   }
}
