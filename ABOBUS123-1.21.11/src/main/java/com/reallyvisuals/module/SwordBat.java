package com.reallyvisuals.module;

import net.minecraft.util.Identifier;

/**
 * Purely client-side sword model replacement. The server still sees the real sword item.
 */
public class SwordBat extends Module {
   public static final Identifier BAT_MODEL_ID = Identifier.of("abobus123", "item/tung_sahur_bat");

   public SwordBat() {
      super(
         "Tung Sahur Bat",
         "Любой меч локально выглядит как бита Тунг Тунг Тунг Саура",
         Category.VISUALS,
         false,
         false
      );
   }
}
