package com.reallyvisuals.utils;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class VanillaChunksMixinPlugin implements IMixinConfigPlugin {
   private boolean sodiumLoaded = false;

   @Override
   public void onLoad(String mixinPackage) {
      this.sodiumLoaded = isSodiumPresent();
   }

   private static boolean isSodiumPresent() {
      try {
         Class.forName("me.jellysquid.mods.sodium.client.render.chunk.ChunkCameraContext", false, VanillaChunksMixinPlugin.class.getClassLoader());
         return true;
      } catch (Throwable var1) {
         return false;
      }
   }

   @Override
   public String getRefMapperConfig() {
      return null;
   }

   @Override
   public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
      return !this.sodiumLoaded;
   }

   @Override
   public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
   }

   @Override
   public List<String> getMixins() {
      return null;
   }

   @Override
   public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }

   @Override
   public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }
}
