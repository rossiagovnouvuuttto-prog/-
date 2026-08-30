package com.reallyvisuals.module;

import com.reallyvisuals.audio.SoundPlayer;

public class HitSounds extends Module {
   public Module.ModeSetting sound = new Module.ModeSetting(
      "Звук", new String[]{"Bell", "UwU", "Bonk", "Bubble", "Pop", "Moan", "Hit1", "Hit2", "Hit3"}, "Bell"
   );
   public Module.NumberSetting volume = new Module.NumberSetting("Громкость", 1.0, 0.1, 1.0, 0.1);
   public Module.BooleanSetting onlyCrit = new Module.BooleanSetting("Только при крите", false);
   public Module.BooleanSetting removeVanillaCrit = new Module.BooleanSetting("Убрать ванильный крит", false);

   public HitSounds() {
      super("Hit Sounds", "Воспроизведение звуков при ударе по противнику", Category.VISUALS, false, true);
      this.addSetting(this.sound);
      this.addSetting(this.volume);
      this.addSetting(this.onlyCrit);
      this.addSetting(this.removeVanillaCrit);
   }

   public void onAttack(boolean isCrit) {
      if (this.isEnabled()) {
         if (!this.onlyCrit.value || isCrit) {
            String selected = this.sound.value;
            if ("Moan".equalsIgnoreCase(selected)) {
               int randomIdx = 1 + (int)(Math.random() * 4.0);
               SoundPlayer.playSound("moan" + randomIdx, (float)this.volume.value);
            } else {
               SoundPlayer.playSound(selected, (float)this.volume.value);
            }
         }
      }
   }
}
