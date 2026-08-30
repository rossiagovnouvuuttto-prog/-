package com.reallyvisuals.module;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.client.gui.DrawContext;
import com.reallyvisuals.utils.KeyUtils;

public class Module {
   private final String name;
   private final String description;
   private final Category category;
   private boolean enabled;
   private boolean favorite;
   private boolean hasDropdown;
   private String tag;
   private boolean expanded = false;
   private float expandProgress = 0.0F;
   private float toggleProgress = 0.0F;
    private final List<Module.Setting> settings = new ArrayList<>();
    private int key = 0;
    public int customColor = -1;

   public Module(String name, String description, Category category, boolean enabled, boolean hasDropdown) {
      this.name = name;
      this.description = description;
      this.category = category;
      this.enabled = enabled;
      this.favorite = false;
      this.hasDropdown = hasDropdown;
      this.tag = "None";
      this.toggleProgress = enabled ? 1.0F : 0.0F;
   }

   public int getKey() {
      return this.key;
   }

   public void setKey(int key) {
      this.key = key;
      this.tag = key <= 0 ? "None" : KeyUtils.getShortKeyName(key);
   }

   public void addSetting(Module.Setting s) {
      this.settings.add(s);
   }

   public List<Module.Setting> getSettings() {
      return this.settings;
   }

   public boolean isExpanded() {
      return this.expanded;
   }

   public void setExpanded(boolean expanded) {
      this.expanded = expanded;
   }

   public float getExpandProgress() {
      return this.expandProgress;
   }

   public void setExpandProgress(float progress) {
      this.expandProgress = progress;
   }

   public float getToggleProgress() {
      return this.toggleProgress;
   }

   public void setToggleProgress(float progress) {
      this.toggleProgress = progress;
   }

   public void onTick() {
   }

   public void onKeyPressed(int key) {
   }

   public void onRenderHUD(DrawContext context) {
   }

   public String getName() {
      return this.name;
   }

   public String getDescription() {
      return this.description;
   }

   public Category getCategory() {
      return this.category;
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public void setEnabled(boolean enabled) {
      if (this.enabled != enabled) {
         this.enabled = enabled;
         if (enabled) {
            this.onEnable();
         } else {
            this.onDisable();
         }
      }
   }

   public void toggle() {
      this.setEnabled(!this.enabled);
   }

   public void onEnable() {
   }

   public void onDisable() {
   }

   public boolean isFavorite() {
      return this.favorite;
   }

   public void setFavorite(boolean favorite) {
      this.favorite = favorite;
   }

   public boolean hasDropdown() {
      return this.hasDropdown;
   }

   public String getTag() {
      return this.tag;
   }

   public void setTag(String tag) {
      this.tag = tag;
   }

   public static class BooleanSetting extends Module.Setting {
      public boolean value;

      public BooleanSetting(String name, boolean val) {
         super(name);
         this.value = val;
      }
   }

   public static class ColorSetting extends Module.Setting {
      public int color;

      public ColorSetting(String name, int color) {
         super(name);
         this.color = color;
      }
   }

   public static class KeySetting extends Module.Setting {
      public int key = 0;
      public String keyName = "None";
      public boolean listening = false;

      public KeySetting(String name, int defaultKey) {
         super(name);
         this.setKey(defaultKey);
      }

      public void setKey(int k) {
         this.key = k;
         this.keyName = k <= 0 ? "None" : KeyUtils.getShortKeyName(k);
      }
   }

    public static class ModeSetting extends Module.Setting {
       public String[] modes;
       public String value;
       public boolean open = false;
       public float dropProgress = 0.0F;

      public ModeSetting(String name, String[] modes, String val) {
         super(name);
         this.modes = modes;
         this.value = val;
      }
   }

    public static class MultiSelectSetting extends Module.Setting {
       public String[] options;
       public Set<String> selected = new HashSet<>();
       public boolean open = false;
       public float dropProgress = 0.0F;

      public MultiSelectSetting(String name, String[] options, String[] defaultSelected) {
         super(name);
         this.options = options;
         if (defaultSelected != null) {
            for (String s : defaultSelected) {
               this.selected.add(s);
            }
         }
      }

      public boolean isSelected(String option) {
         return this.selected.contains(option);
      }

      public void toggle(String option) {
         if (this.selected.contains(option)) {
            this.selected.remove(option);
         } else {
            this.selected.add(option);
         }
      }
   }

   public static class NumberSetting extends Module.Setting {
      public double value;
      public double min;
      public double max;
      public double inc;

      public NumberSetting(String name, double val, double min, double max, double inc) {
         super(name);
         this.value = val;
         this.min = min;
         this.max = max;
         this.inc = inc;
      }
   }

   public abstract static class Setting {
      public String name;
      public Supplier<Boolean> visibleCondition = null;

      public Setting(String name) {
         this.name = name;
      }

      public Module.Setting visible(Supplier<Boolean> condition) {
         this.visibleCondition = condition;
         return this;
      }

      public boolean isVisible() {
         return this.visibleCondition == null || this.visibleCondition.get();
      }
   }

   public static class TextSetting extends Module.Setting {
      public String value;
      public boolean focused = false;

      public TextSetting(String name, String val) {
         super(name);
         this.value = val;
      }
   }
}
