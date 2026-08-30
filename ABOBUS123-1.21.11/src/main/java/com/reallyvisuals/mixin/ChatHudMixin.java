package com.reallyvisuals.mixin;

import net.minecraft.client.gui.DrawContext;

import com.reallyvisuals.module.Animations;
import com.reallyvisuals.module.AutoDuel;
import com.reallyvisuals.module.AutoMarkers;
import com.reallyvisuals.module.ModuleManager;
import com.reallyvisuals.utils.AnimationUtils;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin {
   @Shadow
   @Final
   private List<ChatHudLine> visibleMessages;
   @Shadow
   private int scrolledLines;
   @Unique
   private final Map<ChatHudLine, AnimationUtils.Animation> messageAnimators = new IdentityHashMap<>();
   @Unique
   private int currentRenderIndex;

   @Shadow
   public abstract int getWidth();

   @Unique
   private boolean isChatAnimEnabled() {
      Animations anim = (Animations)ModuleManager.getInstance().getModule("Animations");
      return anim != null && anim.isEnabled() && anim.chatAnim.value;
   }

   @Inject(method = "render", at = @At("HEAD"))
   private void onRenderHead(DrawContext context, net.minecraft.client.font.TextRenderer textRenderer,
         int currentTick, int mouseX, int mouseY, boolean focused, boolean hovered, CallbackInfo ci) {
      if (this.isChatAnimEnabled() && !this.visibleMessages.isEmpty()) {
         AnimationUtils.Animation anim = this.messageAnimators.get(this.visibleMessages.get(0));
         if (anim != null) {
            context.getMatrices().translate(anim.getValue(), 0.0F);
         }
      }
      if (this.isChatAnimEnabled()) {
         this.messageAnimators.entrySet().removeIf(entry -> {
            AnimationUtils.Animation anim = entry.getValue();
            return anim.getValue() == anim.getTarget();
         });
         this.currentRenderIndex = 0;
      }
   }

   // 1.21.11: TextRenderer.drawWithShadow is gone; the slide is applied to the
   // whole chat matrix in onRenderHead instead of per-line redirection.


   @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("TAIL"))
   private void onAddMessage(Text message, CallbackInfo ci) {
      if (message != null) {
         String raw = message.getString();
         AutoDuel.onChatMessage(raw);
         AutoMarkers.onChatMessage(raw);
      }

      if (this.isChatAnimEnabled() && !this.visibleMessages.isEmpty()) {
         Animations animModule = (Animations)ModuleManager.getInstance().getModule("Animations");
         long duration = (long)animModule.chatDuration.value;
         ChatHudLine latest = this.visibleMessages.get(0);
         AnimationUtils.Animation anim = new AnimationUtils.Animation(-this.getWidth());
         anim.animateTo(0.0F, duration);
         this.messageAnimators.put(latest, anim);
      }
   }

   @Inject(method = "clear", at = @At("HEAD"))
   private void onClear(boolean clearHistory, CallbackInfo ci) {
      this.messageAnimators.clear();
   }
}
