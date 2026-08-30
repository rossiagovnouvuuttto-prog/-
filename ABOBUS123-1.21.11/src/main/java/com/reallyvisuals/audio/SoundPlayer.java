package com.reallyvisuals.audio;

import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/** Uses Minecraft's own sound engine so hit sounds also work on Android/Pojav/FCL. */
public final class SoundPlayer {
   private SoundPlayer() {
   }

   public static void playSound(String soundName, float volume) {
      if (soundName == null || soundName.isEmpty()) {
         return;
      }

      try {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client == null || client.getSoundManager() == null) {
            return;
         }

         String id = "hit." + soundName.toLowerCase(Locale.ROOT);
         SoundEvent event = SoundEvent.of(Identifier.of("really", id));
         float safeVolume = Math.max(0.0F, Math.min(1.0F, volume));
         client.getSoundManager().play(PositionedSoundInstance.ui(event, safeVolume));
      } catch (Throwable ignored) {
      }
   }
}
