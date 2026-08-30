package com.reallyvisuals.gui;

public class HUDManager {
   public static HUDManager.HUDElement potions = new HUDManager.HUDElement("Potions", 15.0F, 60.0F, 115.0F, 60.0F);
   public static HUDManager.HUDElement hotkeys = new HUDManager.HUDElement("Hot Keys", 140.0F, 60.0F, 110.0F, 50.0F);
   public static HUDManager.HUDElement cooldowns = new HUDManager.HUDElement("Cooldowns", 140.0F, 120.0F, 110.0F, 45.0F);
   public static HUDManager.HUDElement targetHud = new HUDManager.HUDElement("Target HUD", 80.0F, 120.0F, 108.0F, 40.0F);
   public static HUDManager.HUDElement watermark = new HUDManager.HUDElement("Watermark", 260.0F, 10.0F, 140.0F, 20.0F);
   public static HUDManager.HUDElement notifications = new HUDManager.HUDElement("Notification Preview", 260.0F, 35.0F, 120.0F, 20.0F);
   public static HUDManager.HUDElement bossbar = new HUDManager.HUDElement("Bossbar", 180.0F, 10.0F, 180.0F, 18.0F);
   public static HUDManager.HUDElement scoreboard = new HUDManager.HUDElement("Scoreboard", 320.0F, 100.0F, 100.0F, 100.0F);
   public static HUDManager.HUDElement inventoryHud = new HUDManager.HUDElement("Inventory HUD", 10.0F, 175.0F, 188.0F, 68.0F);
   public static HUDManager.HUDElement betterNear = new HUDManager.HUDElement("Better Near", 15.0F, 140.0F, 145.0F, 136.0F);
   public static HUDManager.HUDElement keystrokes = new HUDManager.HUDElement("Keystrokes", 10.0F, 110.0F, 66.0F, 68.0F);
   public static HUDManager.HUDElement performanceHud = new HUDManager.HUDElement("Performance HUD", 10.0F, 12.0F, 118.0F, 34.0F);
   public static HUDManager.HUDElement coordinatesHud = new HUDManager.HUDElement("Coordinates HUD", 10.0F, 50.0F, 130.0F, 20.0F);
   public static HUDManager.HUDElement speedHud = new HUDManager.HUDElement("Speed HUD", 10.0F, 74.0F, 74.0F, 20.0F);
   public static HUDManager.HUDElement cpsHud = new HUDManager.HUDElement("CPS Counter", 88.0F, 74.0F, 90.0F, 20.0F);
   public static HUDManager.HUDElement comboHud = new HUDManager.HUDElement("Combo Counter", 182.0F, 74.0F, 78.0F, 20.0F);

   public static HUDManager.HUDElement getElement(String name) {
      for (HUDManager.HUDElement e : getAllElements()) {
         if (e.name.equalsIgnoreCase(name)) {
            return e;
         }
      }

      return null;
   }

   public static HUDManager.HUDElement[] getAllElements() {
      return new HUDManager.HUDElement[]{
         potions, hotkeys, cooldowns, targetHud, watermark, notifications, bossbar, scoreboard, inventoryHud, betterNear, keystrokes, performanceHud,
         coordinatesHud, speedHud, cpsHud, comboHud
      };
   }

   public static void clampAll(int screenWidth, int screenHeight) {
      for (HUDManager.HUDElement e : getAllElements()) {
         e.clamp(screenWidth, screenHeight);
      }
   }

   public static class HUDElement {
      public String name;
      public float x;
      public float y;
      public float defaultX;
      public float defaultY;
      public float width;
      public float height;
      public String scale = "Medium";
      public boolean enabled = true;

      public HUDElement(String name, float defaultX, float defaultY, float width, float height) {
         this.name = name;
         this.defaultX = defaultX;
         this.defaultY = defaultY;
         this.x = defaultX;
         this.y = defaultY;
         this.width = width;
         this.height = height;
      }

      public float getScaleFactor() {
         if ("Small".equals(this.scale)) {
            return 0.85F;
         } else {
            return "Large".equals(this.scale) ? 1.15F : 1.0F;
         }
      }

      public void setContentSize(float contentWidth, float contentHeight) {
         float s = this.getScaleFactor();
         this.width = contentWidth * s;
         this.height = contentHeight * s;
      }

      public void beginScale(net.minecraft.client.gui.DrawContext context) {
         float s = this.getScaleFactor();
         context.getMatrices().pushMatrix();
         context.getMatrices().translate((float) (this.x), (float) (this.y));
         context.getMatrices().scale((float) (s), (float) (s));
         context.getMatrices().translate((float) (-this.x), (float) (-this.y));
      }

      public void endScale(net.minecraft.client.gui.DrawContext context) {
         context.getMatrices().popMatrix();
      }

      public boolean contains(float mouseX, float mouseY) {
         float w = Math.max(8.0F, this.width);
         float h = Math.max(8.0F, this.height);
         return mouseX >= this.x && mouseX <= this.x + w && mouseY >= this.y && mouseY <= this.y + h;
      }

      public void clamp(int screenWidth, int screenHeight) {
         float w = Math.max(8.0F, this.width);
         float h = Math.max(8.0F, this.height);
         this.x = Math.max(0.0F, Math.min(this.x, Math.max(0.0F, screenWidth - w)));
         this.y = Math.max(0.0F, Math.min(this.y, Math.max(0.0F, screenHeight - h)));
      }
   }
}
