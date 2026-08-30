package com.reallyvisuals.module;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;

public class FakePlayer extends Module {
   private static final int FAKE_ID = -1337;
   public Module.BooleanSetting copySkin = new Module.BooleanSetting("Скин игрока", true);
   public Module.BooleanSetting copyGear = new Module.BooleanSetting("Копировать броню", true);
   public Module.TextSetting nameSetting = new Module.TextSetting("Ник", "FakePlayer");
   private OtherClientPlayerEntity fakePlayer;
   private ClientWorld fakeWorld;

   public FakePlayer() {
      super("Fake Player", "Создание локального фейкового игрока", Category.UTILITIES, false, true);
      this.addSetting(this.copySkin);
      this.addSetting(this.copyGear);
      this.addSetting(this.nameSetting);
   }

   @Override
   public void onEnable() {
      this.spawnIfPossible();
   }

   @Override
   public void onTick() {
      if (this.isEnabled()) {
         this.spawnIfPossible();
      }
   }

   private void spawnIfPossible() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.world == null || mc.player == null) {
         return;
      }
      if (this.fakePlayer != null && this.fakeWorld == mc.world) {
         return;
      }

      this.fakePlayer = null;
      this.fakeWorld = null;
      try {
         GameProfile profile;
         if (this.copySkin.value) {
            GameProfile own = mc.player.getGameProfile();
            profile = new GameProfile(own.id(), own.name());
            profile.properties().putAll(own.properties());
         } else {
            String name = this.nameSetting.value == null || this.nameSetting.value.trim().isEmpty()
               ? "FakePlayer"
               : this.nameSetting.value.trim();
            if (name.length() > 16) {
               name = name.substring(0, 16);
            }

            profile = new GameProfile(java.util.UUID.randomUUID(), name);
         }

         this.fakePlayer = new OtherClientPlayerEntity(mc.world, profile);
         this.fakePlayer.copyPositionAndRotation(mc.player);
         this.fakePlayer.headYaw = mc.player.headYaw;
         this.fakePlayer.bodyYaw = mc.player.bodyYaw;
         this.fakePlayer.setYaw(mc.player.getYaw());
         this.fakePlayer.setPitch(mc.player.getPitch());
         if (this.copyGear.value) {
            this.fakePlayer.getInventory().clone(mc.player.getInventory());
         }

         mc.world.addEntity(this.fakePlayer);
         this.fakeWorld = mc.world;
      } catch (Throwable var5) {
         this.fakePlayer = null;
         this.fakeWorld = null;
      }
   }

   @Override
   public void onDisable() {
      MinecraftClient mc = MinecraftClient.getInstance();

      try {
         if (this.fakeWorld != null && this.fakePlayer != null) {
            this.fakeWorld.removeEntity(FAKE_ID, net.minecraft.entity.Entity.RemovalReason.DISCARDED);
         }
      } catch (Throwable var3) {
      }

      this.fakePlayer = null;
      this.fakeWorld = null;
   }
}
