package com.reallyvisuals.audio;

import com.reallyvisuals.gui.ReallyVisualsScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class UISoundHelper {
   public static void playSound(String eventPath, float pitch, float volume) {
      if (ReallyVisualsScreen.soundEnabled) {
         if (!eventPath.contains("toggle") || ReallyVisualsScreen.moduleSoundsEnabled) {
            float masterVol = ReallyVisualsScreen.soundVolume / 100.0F * volume;

            try {
               MinecraftClient client = MinecraftClient.getInstance();
               if (client != null && client.getSoundManager() != null) {
                  SoundEvent event = SoundEvent.of(Identifier.of("really", eventPath));
                  client.getSoundManager().play(PositionedSoundInstance.ui(event, masterVol));
               }
            } catch (Throwable var6) {
            }
         }
      }
   }

   public static void playSound(String eventPath) {
      playSound(eventPath, 1.0F, 1.0F);
   }
}
