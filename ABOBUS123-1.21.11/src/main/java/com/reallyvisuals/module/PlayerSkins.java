package com.reallyvisuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Client-side only player appearance overrides.
 * Nothing is sent to the server, so other players keep seeing the normal skin/model.
 */
public class PlayerSkins extends Module {
   public final ModeSetting skin = new ModeSetting(
      "Скин",
      new String[]{"Tung Sahur", "Bombardiro", "Tralalero", "Shadow"},
      "Tung Sahur"
   );
   public final BooleanSetting onlySelf = new BooleanSetting("Только на себе", true);
   public final BooleanSetting morph = new BooleanSetting("Форма персонажа", true);
   public final NumberSetting size = new NumberSetting("Размер", 1.0, 0.70, 1.40, 0.05);

   private static final Identifier TUNG = Identifier.of("abobus123", "textures/skins/tung_sahur.png");
   private static final Identifier BOMBARDIRO = Identifier.of("abobus123", "textures/skins/bombardiro.png");
   private static final Identifier TRALALERO = Identifier.of("abobus123", "textures/skins/tralalero.png");
   private static final Identifier SHADOW = Identifier.of("abobus123", "textures/skins/shadow.png");

   public PlayerSkins() {
      super(
         "Player Skins",
         "Локальные скины и формы игрока — видны только на этом клиенте",
         Category.VISUALS,
         false,
         true
      );
      this.addSetting(this.skin);
      this.addSetting(this.onlySelf);
      this.addSetting(this.morph);
      this.addSetting(this.size);
   }

   public boolean appliesTo(AbstractClientPlayerEntity player) {
      if (!this.isEnabled() || player == null) return false;
      if (!this.onlySelf.value) return true;
      MinecraftClient mc = MinecraftClient.getInstance();
      return mc.player != null && player == mc.player;
   }

   public Identifier getTexture() {
      switch (this.skin.value) {
         case "Bombardiro":
            return BOMBARDIRO;
         case "Tralalero":
            return TRALALERO;
         case "Shadow":
            return SHADOW;
         case "Tung Sahur":
         default:
            return TUNG;
      }
   }

   public float getScaleX() {
      float base = (float)this.size.value;
      if (!this.morph.value) return base;
      switch (this.skin.value) {
         case "Tung Sahur": return base * 0.72F;
         case "Bombardiro": return base * 1.18F;
         case "Tralalero": return base * 0.88F;
         case "Shadow": return base * 0.96F;
         default: return base;
      }
   }

   public float getScaleY() {
      float base = (float)this.size.value;
      if (!this.morph.value) return base;
      switch (this.skin.value) {
         case "Tung Sahur": return base * 1.42F;
         case "Bombardiro": return base * 0.92F;
         case "Tralalero": return base * 1.08F;
         case "Shadow": return base * 1.05F;
         default: return base;
      }
   }

   public float getScaleZ() {
      float base = (float)this.size.value;
      if (!this.morph.value) return base;
      switch (this.skin.value) {
         case "Tung Sahur": return base * 0.72F;
         case "Bombardiro": return base * 1.22F;
         case "Tralalero": return base * 0.88F;
         case "Shadow": return base * 0.96F;
         default: return base;
      }
   }
}
