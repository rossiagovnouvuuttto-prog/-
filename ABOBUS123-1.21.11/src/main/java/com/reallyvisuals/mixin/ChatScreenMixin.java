package com.reallyvisuals.mixin;

import net.minecraft.client.gui.DrawContext;

import com.reallyvisuals.gui.HudEditor;
import com.reallyvisuals.module.Animations;
import com.reallyvisuals.module.ModuleManager;
import com.reallyvisuals.utils.AnimationUtils;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {
   @Shadow
   protected TextFieldWidget chatField;
   @Unique
   private final AnimationUtils.Animation chatInputAnim = new AnimationUtils.Animation(30.0F);

   protected ChatScreenMixin(Text title) {
      super(title);
   }

   @Unique
   private boolean isChatAnimEnabled() {
      Animations anim = (Animations)ModuleManager.getInstance().getModule("Animations");
      return anim != null && anim.isEnabled() && anim.chatAnim.value;
   }

   @Inject(method = "init", at = @At("TAIL"))
   private void onInit(CallbackInfo ci) {
      if (this.isChatAnimEnabled()) {
         Animations animModule = (Animations)ModuleManager.getInstance().getModule("Animations");
         long duration = (long)animModule.chatDuration.value;
         this.chatInputAnim.force(30.0F);
         this.chatInputAnim.animateTo(0.0F, duration);
      }
   }

   @Inject(method = "render", at = @At("HEAD"))
   private void onRenderHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (this.isChatAnimEnabled() && this.chatField != null) {
         this.chatField.setY((int)(this.height - 14 + this.chatInputAnim.getValue()));
      }
   }

   @Inject(method = "render", at = @At("TAIL"))
   private void onRenderTail(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      HudEditor.render(context, mouseX, mouseY);
   }

   @Inject(method = "removed", at = @At("TAIL"))
   private void onRemoved(CallbackInfo ci) {
      HudEditor.close();
   }
}
