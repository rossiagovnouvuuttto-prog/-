package com.reallyvisuals.module;

public enum Category {
   VISUALS("Visuals", "\ue91e"),
   HUD("HUD", "\ue90b"),
   UTILITIES("Utilities", "\ue926"),
   MARKERS("Markers", "\ue934"),
   FRIENDS("Friends", "\ue91c"),
   CONFIGS("Configs", "\ue905");

   private final String name;
   private final String iconGlyph;

   Category(String name, String iconGlyph) {
      this.name = name;
      this.iconGlyph = iconGlyph;
   }

   public String getName() {
      return this.name;
   }

   public String getIconGlyph() {
      return this.iconGlyph;
   }
}
