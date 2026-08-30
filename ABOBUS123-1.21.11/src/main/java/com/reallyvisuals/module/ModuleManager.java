package com.reallyvisuals.module;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModuleManager {
   private static ModuleManager instance;
   private final List<Module> modules = new ArrayList<>();

   public ModuleManager() {
      this.modules.add(new PerformanceBoost());
      this.modules.add(new AutoClicker());
      this.modules.add(new AutoCommand());
      this.modules.add(new AutoDuel());
      this.modules.add(new AutoSethome());
      this.modules.add(new AutoSpec());
      this.modules.add(new BowOptimizer());
      this.modules.add(new ClickPearl());
      this.modules.add(new ConsumeNotifier());
      this.modules.add(new FastExperience());
      this.modules.add(new FreeLook());
      this.modules.add(new ItemScroller());
      this.modules.add(new LockSlot());
      this.modules.add(new NoJumpDelay());
      this.modules.add(new ShiftTab());
      this.modules.add(new Zoom());
      this.modules.add(new Sprint());
      this.modules.add(new StreamerMode());
      this.modules.add(new Pinger());
      this.modules.add(new PvpSafeModule());
      this.modules.add(new HideACBot());
      this.modules.add(new RWJoiner());
      this.modules.add(new ShulkerPreview());
      this.modules.add(new FakePlayer());
      this.modules.add(new Scanner());
      this.modules.add(new AspectRatio());
      this.modules.add(new PlayerSkins());
      this.modules.add(new SwordBat());
      this.modules.add(new Animations());
      this.modules.add(new BlockOverlay());
      this.modules.add(new ChinaHat());
      this.modules.add(new CooldownVisualizer());
      this.modules.add(new Crosshair());
      this.modules.add(new CustomHand());
      this.modules.add(new CustomWorld());
      this.modules.add(new EntityBoxes());
      this.modules.add(new FullBright());
      this.modules.add(new HitColor());
      this.modules.add(new HitSounds());
      this.modules.add(new JumpCircles());
      this.modules.add(new NoFluid());
      this.modules.add(new SelfNametag());
      this.modules.add(new RenderTweaks());
      this.modules.add(new TimeChanger());
      this.modules.add(new Slipstream());
      this.modules.add(new WorldParticles());
      this.modules.add(new Trails());
      this.modules.add(new TargetESP());
      this.modules.add(new HitMarker());
      this.modules.add(new ArmorHud());
      this.modules.add(new Keystrokes());
      this.modules.add(new PerformanceHud());
      this.modules.add(new CoordinatesHud());
      this.modules.add(new SpeedHud());
      this.modules.add(new CpsCounter());
      this.modules.add(new ComboCounter());
      this.modules.add(new BetterNear());
      this.modules.add(new Cooldowns());
      this.modules.add(new Hotkeys());
      this.modules.add(new InventoryHud());
      this.modules.add(new Watermark());
      this.modules.add(new Potions());
      this.modules.add(new TargetHud());
      this.modules.add(new MarkerSettings());
      this.modules.add(new AutoMarkers());
      this.modules.add(new CreateMarker());
      this.modules.add(new FriendSystem());
      this.modules.add(new FriendESP());
      this.modules.add(new DeathPosition());
      this.modules.add(new Waypoints());
   }

   public static ModuleManager getInstance() {
      if (instance == null) {
         instance = new ModuleManager();
      }

      return instance;
   }

   public Module getModule(String name) {
      return this.modules.stream().filter(m -> m.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
   }

   public List<Module> getModules() {
      return this.modules;
   }

   public List<Module> getModulesByCategory(Category category) {
      return this.modules
         .stream()
         .filter(m -> m.getCategory() == category)
         .sorted((a, b) -> a.isFavorite() != b.isFavorite() ? Boolean.compare(b.isFavorite(), a.isFavorite()) : 0)
         .collect(Collectors.toList());
   }

   public List<Module> searchModules(Category category, String query) {
      if (query != null && !query.trim().isEmpty()) {
         String q = query.toLowerCase().trim();
         return this.modules
            .stream()
            .filter(m -> m.getCategory() == category)
            .filter(m -> m.getName().toLowerCase().contains(q) || m.getDescription().toLowerCase().contains(q))
            .sorted((a, b) -> a.isFavorite() != b.isFavorite() ? Boolean.compare(b.isFavorite(), a.isFavorite()) : 0)
            .collect(Collectors.toList());
      } else {
         return this.getModulesByCategory(category);
      }
   }
}
