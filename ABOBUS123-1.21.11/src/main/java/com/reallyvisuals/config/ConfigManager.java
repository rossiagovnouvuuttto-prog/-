package com.reallyvisuals.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.reallyvisuals.gui.HUDManager;
import com.reallyvisuals.gui.ReallyVisualsScreen;
import com.reallyvisuals.module.Module;
import com.reallyvisuals.module.ModuleManager;
import com.reallyvisuals.module.FriendManager;
import com.reallyvisuals.module.WaypointManager;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

public class ConfigManager {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

   private static File getConfigFile() {
      File dir = new File(MinecraftClient.getInstance().runDirectory, "abobus123");
      if (!dir.exists()) {
         dir.mkdirs();
      }

      return new File(dir, "default_config.json");
   }

   public static void saveConfig() {
      try {
         JsonObject root = new JsonObject();
         JsonObject modulesObj = new JsonObject();

         for (Module m : ModuleManager.getInstance().getModules()) {
            JsonObject mObj = new JsonObject();
            mObj.addProperty("enabled", m.isEnabled());
            mObj.addProperty("favorite", m.isFavorite());
            mObj.addProperty("key", m.getKey());
            mObj.addProperty("customColor", m.customColor);
            JsonObject settingsObj = new JsonObject();

            for (Module.Setting s : m.getSettings()) {
               if (s instanceof Module.BooleanSetting) {
                  settingsObj.addProperty(s.name, ((Module.BooleanSetting)s).value);
               } else if (s instanceof Module.NumberSetting) {
                  settingsObj.addProperty(s.name, ((Module.NumberSetting)s).value);
               } else if (s instanceof Module.ModeSetting) {
                  settingsObj.addProperty(s.name, ((Module.ModeSetting)s).value);
               } else if (!(s instanceof Module.MultiSelectSetting)) {
                  if (s instanceof Module.KeySetting) {
                     settingsObj.addProperty(s.name, ((Module.KeySetting)s).key);
                  } else if (s instanceof Module.TextSetting) {
                     settingsObj.addProperty(s.name, ((Module.TextSetting)s).value);
                  } else if (s instanceof Module.ColorSetting) {
                     settingsObj.addProperty(s.name, ((Module.ColorSetting)s).color);
                  }
               } else {
                  JsonArray arr = new JsonArray();

                  for (String opt : ((Module.MultiSelectSetting)s).selected) {
                     arr.add(opt);
                  }

                  settingsObj.add(s.name, arr);
               }
            }

            mObj.add("settings", settingsObj);
            modulesObj.add(m.getName(), mObj);
         }

         root.add("modules", modulesObj);
         JsonArray hudArray = new JsonArray();

         for (HUDManager.HUDElement elem : HUDManager.getAllElements()) {
            JsonObject elemObj = new JsonObject();
            elemObj.addProperty("name", elem.name);
            elemObj.addProperty("x", elem.x);
            elemObj.addProperty("y", elem.y);
            elemObj.addProperty("width", elem.width);
            elemObj.addProperty("height", elem.height);
            elemObj.addProperty("enabled", elem.enabled);
            elemObj.addProperty("scale", elem.scale);
            hudArray.add(elemObj);
         }

         root.add("hud", hudArray);

         JsonArray friendsArray = new JsonArray();
         for (FriendManager.Friend friend : FriendManager.getFriends()) {
            JsonObject friendObj = new JsonObject();
            friendObj.addProperty("name", friend.name);
            friendObj.addProperty("dateAdded", friend.dateAdded);
            friendsArray.add(friendObj);
         }
         root.add("friends", friendsArray);

         JsonArray waypointArray = new JsonArray();
         for (WaypointManager.Waypoint waypoint : WaypointManager.getWaypoints()) {
            JsonObject waypointObj = new JsonObject();
            waypointObj.addProperty("name", waypoint.name);
            waypointObj.addProperty("x", waypoint.pos.x);
            waypointObj.addProperty("y", waypoint.pos.y);
            waypointObj.addProperty("z", waypoint.pos.z);
            waypointObj.addProperty("color", waypoint.color);
            waypointObj.addProperty("iconIndex", waypoint.iconIndex);
            waypointObj.addProperty("visible", waypoint.visible);
            waypointObj.addProperty("createdText", waypoint.createdText);
            waypointArray.add(waypointObj);
         }
         root.add("waypoints", waypointArray);

         JsonObject clientObj = new JsonObject();
         clientObj.addProperty("accentColor", ReallyVisualsScreen.accentColor);
         clientObj.addProperty("clientColor", ReallyVisualsScreen.clientColor);
         clientObj.addProperty("uiScale", ReallyVisualsScreen.uiScale);
         clientObj.addProperty("soundEnabled", ReallyVisualsScreen.soundEnabled);
         clientObj.addProperty("soundVolume", ReallyVisualsScreen.soundVolume);
         clientObj.addProperty("moduleSoundsEnabled", ReallyVisualsScreen.moduleSoundsEnabled);
         clientObj.addProperty("menuKeyName", ReallyVisualsScreen.menuKeyName);
         clientObj.addProperty("menuKeyCode", ReallyVisualsScreen.menuKeyCode);
         root.add("clientSettings", clientObj);
         File configFile = getConfigFile();
         FileWriter writer = new FileWriter(configFile);

         try {
            GSON.toJson(root, writer);
         } catch (Throwable var12) {
            try {
               writer.close();
            } catch (Throwable var11) {
               var12.addSuppressed(var11);
            }

            throw var12;
         }

         writer.close();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   public static void loadConfig() {
      Map<Module, Boolean> pendingEnabled = new LinkedHashMap<>();
      try {
         File configFile = getConfigFile();
         if (!configFile.exists()) {
            return;
         }

         JsonParser parser = new JsonParser();
         FileReader reader = new FileReader(configFile);

         JsonObject root;
         try {
            root = parser.parse(reader).getAsJsonObject();
         } catch (Throwable var13) {
            try {
               reader.close();
            } catch (Throwable var12) {
               var13.addSuppressed(var12);
            }

            throw var13;
         }

         reader.close();
         if (root.has("modules")) {
            JsonObject modulesObj = root.getAsJsonObject("modules");

            for (Module m : ModuleManager.getInstance().getModules()) {
               if (modulesObj.has(m.getName())) {
                  JsonObject mObj = modulesObj.getAsJsonObject(m.getName());
                  if (mObj.has("enabled")) {
                     pendingEnabled.put(m, mObj.get("enabled").getAsBoolean());
                  }

                  if (mObj.has("favorite")) {
                     m.setFavorite(mObj.get("favorite").getAsBoolean());
                  }

                  if (mObj.has("key")) {
                     m.setKey(mObj.get("key").getAsInt());
                  }

                  if (mObj.has("customColor")) {
                     m.customColor = mObj.get("customColor").getAsInt();
                  }

                  if (mObj.has("settings")) {
                     JsonObject settingsObj = mObj.getAsJsonObject("settings");

                     for (Module.Setting s : m.getSettings()) {
                        if (settingsObj.has(s.name)) {
                           if (s instanceof Module.BooleanSetting) {
                              ((Module.BooleanSetting)s).value = settingsObj.get(s.name).getAsBoolean();
                           } else if (s instanceof Module.NumberSetting) {
                              Module.NumberSetting number = (Module.NumberSetting)s;
                              double saved = settingsObj.get(s.name).getAsDouble();
                              number.value = Math.max(number.min, Math.min(number.max, saved));
                           } else if (s instanceof Module.ModeSetting) {
                              Module.ModeSetting mode = (Module.ModeSetting)s;
                              String saved = settingsObj.get(s.name).getAsString();
                              for (String option : mode.modes) {
                                 if (option.equals(saved)) {
                                    mode.value = saved;
                                    break;
                                 }
                              }
                           } else if (s instanceof Module.MultiSelectSetting) {
                              JsonArray arr = settingsObj.getAsJsonArray(s.name);
                              ((Module.MultiSelectSetting)s).selected.clear();

                              for (int i = 0; i < arr.size(); i++) {
                                 ((Module.MultiSelectSetting)s).selected.add(arr.get(i).getAsString());
                              }
                           } else if (s instanceof Module.KeySetting) {
                              ((Module.KeySetting)s).setKey(settingsObj.get(s.name).getAsInt());
                           } else if (s instanceof Module.TextSetting) {
                              ((Module.TextSetting)s).value = settingsObj.get(s.name).getAsString();
                           } else if (s instanceof Module.ColorSetting) {
                              ((Module.ColorSetting)s).color = settingsObj.get(s.name).getAsInt();
                           }
                        }
                     }
                  }
               }
            }
         }

         if (root.has("hud")) {
            JsonArray hudArray = root.getAsJsonArray("hud");

            for (int i = 0; i < hudArray.size(); i++) {
               JsonObject elemObj = hudArray.get(i).getAsJsonObject();
               String name = elemObj.get("name").getAsString();
               HUDManager.HUDElement elem = HUDManager.getElement(name);
               if (elem != null) {
                  if (elemObj.has("x")) {
                     elem.x = elemObj.get("x").getAsFloat();
                  }

                  if (elemObj.has("y")) {
                     elem.y = elemObj.get("y").getAsFloat();
                  }

                  if (elemObj.has("width")) {
                     elem.width = elemObj.get("width").getAsFloat();
                  }

                  if (elemObj.has("height")) {
                     elem.height = elemObj.get("height").getAsFloat();
                  }

                  if (elemObj.has("enabled")) {
                     elem.enabled = elemObj.get("enabled").getAsBoolean();
                  }

                  if (elemObj.has("scale")) {
                     String scale = elemObj.get("scale").getAsString();
                     if ("Small".equals(scale) || "Medium".equals(scale) || "Large".equals(scale)) {
                        elem.scale = scale;
                     }
                  }
               }
            }
         }

         if (root.has("friends")) {
            FriendManager.getFriends().clear();
            JsonArray friendsArray = root.getAsJsonArray("friends");
            for (int i = 0; i < friendsArray.size(); i++) {
               JsonObject friendObj = friendsArray.get(i).getAsJsonObject();
               if (friendObj.has("name")) {
                  String name = friendObj.get("name").getAsString();
                  if (name != null && !name.trim().isEmpty() && !FriendManager.isFriend(name)) {
                     String dateAdded = friendObj.has("dateAdded") ? friendObj.get("dateAdded").getAsString() : "ранее";
                     FriendManager.getFriends().add(new FriendManager.Friend(name.trim(), false, dateAdded));
                  }
               }
            }
         }

         if (root.has("waypoints")) {
            WaypointManager.getWaypoints().clear();
            JsonArray waypointArray = root.getAsJsonArray("waypoints");
            for (int i = 0; i < waypointArray.size(); i++) {
               JsonObject waypointObj = waypointArray.get(i).getAsJsonObject();
               if (waypointObj.has("name") && waypointObj.has("x") && waypointObj.has("y") && waypointObj.has("z")) {
                  WaypointManager.Waypoint waypoint = new WaypointManager.Waypoint(
                     waypointObj.get("name").getAsString(),
                     new Vec3d(waypointObj.get("x").getAsDouble(), waypointObj.get("y").getAsDouble(), waypointObj.get("z").getAsDouble()),
                     waypointObj.has("color") ? waypointObj.get("color").getAsInt() : -34019,
                     waypointObj.has("iconIndex") ? waypointObj.get("iconIndex").getAsInt() : 6,
                     waypointObj.has("createdText") ? waypointObj.get("createdText").getAsString() : "ранее"
                  );
                  if (waypointObj.has("visible")) {
                     waypoint.visible = waypointObj.get("visible").getAsBoolean();
                  }
                  WaypointManager.getWaypoints().add(waypoint);
               }
            }
         }

         if (root.has("clientSettings")) {
            JsonObject clientObj = root.getAsJsonObject("clientSettings");
            if (clientObj.has("accentColor")) {
               ReallyVisualsScreen.accentColor = clientObj.get("accentColor").getAsInt();
            }

            if (clientObj.has("clientColor")) {
               ReallyVisualsScreen.clientColor = clientObj.get("clientColor").getAsInt();
            }

            if (clientObj.has("uiScale")) {
               ReallyVisualsScreen.uiScale = Math.max(0.5F, Math.min(2.0F, clientObj.get("uiScale").getAsFloat()));
            }

            if (clientObj.has("soundEnabled")) {
               ReallyVisualsScreen.soundEnabled = clientObj.get("soundEnabled").getAsBoolean();
            }

            if (clientObj.has("soundVolume")) {
               ReallyVisualsScreen.soundVolume = clientObj.get("soundVolume").getAsFloat();
            }

            if (clientObj.has("moduleSoundsEnabled")) {
               ReallyVisualsScreen.moduleSoundsEnabled = clientObj.get("moduleSoundsEnabled").getAsBoolean();
            }

            if (clientObj.has("menuKeyName")) {
               ReallyVisualsScreen.menuKeyName = clientObj.get("menuKeyName").getAsString();
            }

            if (clientObj.has("menuKeyCode")) {
               ReallyVisualsScreen.menuKeyCode = clientObj.get("menuKeyCode").getAsInt();
            }
         }

         // Enable modules only after every saved setting/HUD/client option is restored.
         // This prevents onEnable() from running once with stale defaults during startup.
         for (Map.Entry<Module, Boolean> entry : pendingEnabled.entrySet()) {
            entry.getKey().setEnabled(entry.getValue());
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}
