package com.reallyvisuals.mixin;

import net.minecraft.client.gui.DrawContext;

import com.reallyvisuals.module.Animations;
import com.reallyvisuals.module.ModuleManager;
import com.reallyvisuals.module.FriendManager;
import com.reallyvisuals.module.FriendSystem;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.reallyvisuals.utils.AnimationUtils;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {
   @Unique
   private final AnimationUtils.Animation tabAnim = new AnimationUtils.Animation(0.0F);
   @Unique
   private long lastRenderTime = 0L;

   @Unique
   private boolean isTabAnimEnabled() {
      Animations anim = (Animations)ModuleManager.getInstance().getModule("Animations");
      return anim != null && anim.isEnabled() && anim.tabAnim.value;
   }


   @Inject(method = "getPlayerName", at = @At("HEAD"), cancellable = true, require = 0)
   private void abobus123$friendName(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
      FriendSystem fs = (FriendSystem)ModuleManager.getInstance().getModule("Настройки друзей");
      if (entry != null && entry.getProfile() != null && fs != null && fs.isEnabled() && fs.tabHighlight.value && FriendManager.isFriend(entry.getProfile().name())) {
         cir.setReturnValue(net.minecraft.text.Text.literal("§a" + entry.getProfile().name()));
      }
   }

   @Inject(method = "render", at = @At("HEAD"))
   private void onRenderHead(DrawContext context, int windowWidth, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo ci) {
      if (this.isTabAnimEnabled()) {
         Animations animModule = (Animations)ModuleManager.getInstance().getModule("Animations");
         long duration = (long)animModule.tabDuration.value;
         long now = System.currentTimeMillis();
         if (now - this.lastRenderTime > 250L) {
            this.tabAnim.force(-250.0F);
            this.tabAnim.animateTo(0.0F, duration);
         }

         this.lastRenderTime = now;
         context.getMatrices().pushMatrix();
         context.getMatrices().translate(0.0F, (float) this.tabAnim.getValue());
      }
   }

   @Inject(method = "render", at = @At("TAIL"))
   private void onRenderTail(DrawContext context, int windowWidth, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo ci) {
      if (this.isTabAnimEnabled()) {
         context.getMatrices().popMatrix();
      }
   }
}
