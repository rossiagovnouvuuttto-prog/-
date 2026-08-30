package com.reallyvisuals.module;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reallyvisuals.gui.HUDManager;
import com.reallyvisuals.gui.ReallyVisualsScreen;
import com.reallyvisuals.gui.font.CustomFont;
import com.reallyvisuals.gui.font.FontManager;
import com.reallyvisuals.gui.render.RenderUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.ModelWithHead;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

public class TargetHud extends Module {
   private float animHealth = 20.0F;

   public TargetHud() {
      super("Target HUD", "Отображение информации о текущей цели", Category.HUD, false, false);
   }

   @Override
   public void onRenderHUD(DrawContext context) {
      if (this.isEnabled()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.player != null && mc.world != null) {
            LivingEntity target = null;
            if (mc.targetedEntity instanceof LivingEntity && mc.targetedEntity.isAlive()) {
               target = (LivingEntity)mc.targetedEntity;
            }

            boolean isGuiOpen = mc.currentScreen instanceof ChatScreen || mc.currentScreen instanceof ReallyVisualsScreen;
            if (target != null || isGuiOpen) {
               String name = target != null ? this.resolveName(target) : mc.player.getDisplayName().getString();
               float health = target != null ? Math.max(0.0F, target.getHealth()) : 13.0F;
               float maxHealth = target != null ? Math.max(1.0F, target.getMaxHealth()) : 20.0F;
               this.animHealth = this.animHealth + (health - this.animHealth) * 0.15F;
               if (Math.abs(this.animHealth - health) < 0.01F) {
                  this.animHealth = health;
               }

               CustomFont mainFont = FontManager.getMainFont();
               CustomFont subFont = FontManager.getSubFont();
               int x = (int)HUDManager.targetHud.x;
               int y = (int)HUDManager.targetHud.y;
               int cardWidth = 108;
               int cardHeight = 40;
               float radius = 5.0F;
               HUDManager.targetHud.setContentSize(cardWidth, cardHeight);
               HUDManager.targetHud.beginScale(context);
               RenderUtils.drawRoundedRect(context, x, y, cardWidth, cardHeight, radius, -300871403);
               RenderUtils.drawRoundedRect(context, x + 1, y + 1, cardWidth - 2, cardHeight - 2, radius - 0.5F, -15329761);
               int avatarX = x + 4;
               int avatarY = y + 4;
               int avatarSize = 28;
               RenderUtils.drawRoundedRect(context, avatarX, avatarY, avatarSize, avatarSize, 3.5F, -15461351);
               boolean showHeadFace = target instanceof PlayerEntity || target == null && isGuiOpen;
               if (showHeadFace) {
                  Identifier skinTexture = target instanceof PlayerEntity ? this.getPlayerSkin((PlayerEntity)target) : this.getPlayerSkin(mc.player);
                  this.drawPlayerHeadFace(context, skinTexture, avatarX, avatarY, avatarSize);
                } else if (target != null) {
                   if (PerformanceBoost.throttleHeavyVisuals()) {
                      this.drawFallbackFace(context, target, avatarX, avatarY, avatarSize);
                   } else {
                      this.drawMobFace(context, target, avatarX, avatarY, avatarSize);
                   }
                }

               String displayName = name;
               int maxNameW = cardWidth - 4 - avatarSize - 6 - 6;
               if (subFont.getStringWidth(displayName) > maxNameW) {
                  while (displayName.length() > 1 && subFont.getStringWidth(displayName + "...") > maxNameW) {
                     displayName = displayName.substring(0, displayName.length() - 1);
                  }

                  displayName = displayName + "...";
               }

               subFont.drawString(context, displayName, avatarX + avatarSize + 6, avatarY, -1);
               int textX = avatarX + avatarSize + 6;
               int textY = avatarY + 13;
               context.getMatrices().pushMatrix();
               context.getMatrices().translate((float) (textX), (float) (textY));
               context.getMatrices().scale((float) (0.85F), (float) (0.85F));
               subFont.drawString(context, "HP", 0.0F, 0.0F, -4802881);
               int hpW = subFont.getStringWidth("HP");
               RenderUtils.drawRoundedRect(context, hpW + 4, 3.5F, 2.0F, 2.0F, 1.0F, -8750459);
               subFont.drawString(context, String.valueOf((int)Math.ceil(health)), hpW + 10, 0.0F, -4802881);
               context.getMatrices().popMatrix();
               int barX = avatarX + avatarSize + 6;
               int barY = y + cardHeight - 10;
               int barWidth = cardWidth - 10 - avatarSize - 6;
               int barHeight = 3;
               RenderUtils.drawRoundedRect(context, barX, barY, barWidth, barHeight, 1.5F, -15658731);
               float hpPercent = Math.max(0.02F, Math.min(1.0F, this.animHealth / maxHealth));
               int filledWidth = (int)(barWidth * hpPercent);
               RenderUtils.drawRoundedRect(context, barX, barY, filledWidth, barHeight, 1.5F, ReallyVisualsScreen.clientColor);
               HUDManager.targetHud.endScale(context);
            }
         }
      }
   }

   private String resolveName(LivingEntity target) {
      String custom = null;

      try {
         if (target.getCustomName() != null) {
            custom = target.getCustomName().getString();
         }
      } catch (Throwable var4) {
      }

      String display;
      try {
         display = target.getDisplayName().getString();
      } catch (Throwable var5) {
         display = "Unknown";
      }

      String result = custom != null && !custom.trim().isEmpty() ? custom : display;
      result = CustomFont.stripFormatting(result);
      StringBuilder clean = new StringBuilder();
      for (int i = 0; i < result.length(); ) {
         int cp = result.codePointAt(i);
         i += Character.charCount(cp);
         if (Character.isISOControl(cp)) continue;
         if (cp >= 0xE000 && cp <= 0xF8FF) continue;
         if (cp > 0xFFFF) continue;
         clean.append((char)cp);
      }
      String sanitized = clean.toString().trim();
      if (sanitized.startsWith("??")) {
         sanitized = sanitized.substring(2).trim();
      }
      return sanitized.isEmpty() ? target.getName().getString() : sanitized;
   }

   private void drawMobFace(DrawContext context, LivingEntity entity, int x, int y, int size) {
      try {
         MinecraftClient mc = MinecraftClient.getInstance();
         EntityRenderer<?, ?> renderer = mc.getEntityRenderDispatcher().getRenderer(entity);
         if (!(renderer instanceof LivingEntityRenderer)) {
            this.drawFallbackFace(context, entity, x, y, size);
            return;
         }

         LivingEntityRenderer<?, ?, ?> livingRenderer = (LivingEntityRenderer<?, ?, ?>) renderer;
         EntityModel<?> model = livingRenderer.getModel();
         Object headPart = this.findHeadPart(model);
         if (headPart == null) {
            this.drawFallbackFace(context, entity, x, y, size);
            return;
         }

         Object cuboids = getFieldValue(headPart, "cuboids", "field_3663");
         if (!(cuboids instanceof List) || ((List)cuboids).isEmpty()) {
            this.drawFallbackFace(context, entity, x, y, size);
            return;
         }

         Object cuboid = this.findHeadCuboid((List)cuboids);
         Object sides = cuboid == null ? null : getFieldValue(cuboid, "sides", "field_3649");
         if (!(sides instanceof Object[]) || ((Object[])sides).length < 6) {
            this.drawFallbackFace(context, entity, x, y, size);
            return;
         }

         Object front = this.findFrontQuad((Object[])sides);
         if (front == null) {
            this.drawFallbackFace(context, entity, x, y, size);
            return;
         }

         Object vertices = getFieldValue(front, "vertices", "field_3502");
         if (!(vertices instanceof Object[]) || ((Object[])vertices).length < 4) {
            this.drawFallbackFace(context, entity, x, y, size);
            return;
         }

         float minU = 1000.0F;
         float minV = 1000.0F;
         float maxU = -1000.0F;
         float maxV = -1000.0F;

         for (Object vertex : (Object[])vertices) {
            Number u = (Number)getFieldValue(vertex, "u", "field_3604");
            Number v = (Number)getFieldValue(vertex, "v", "field_3603");
            float vu = u == null ? 0.0F : u.floatValue();
            float vv = v == null ? 0.0F : v.floatValue();
            if (vu < minU) {
               minU = vu;
            }

            if (vv < minV) {
               minV = vv;
            }

            if (vu > maxU) {
               maxU = vu;
            }

            if (vv > maxV) {
               maxV = vv;
            }
         }

         
            Identifier tex = entity instanceof net.minecraft.client.network.AbstractClientPlayerEntity a1 ? a1.getSkin().body().texturePath() : null;
         if (tex == null || maxU - minU <= 0.0F || maxV - minV <= 0.0F) {
            this.drawFallbackFace(context, entity, x, y, size);
            return;
         }

         float faceW = Math.abs(getFloatField(cuboid, "maxX") - getFloatField(cuboid, "minX"));
         float faceH = Math.abs(getFloatField(cuboid, "maxY") - getFloatField(cuboid, "minY"));
         float aspect = faceW > 0.0F && faceH > 0.0F ? faceW / faceH : (maxU - minU) / (maxV - minV);
         int drawW;
         int drawH;
         if (aspect >= 1.0F) {
            drawW = size;
            drawH = Math.max(1, (int)(size / aspect));
         } else {
            drawH = size;
            drawW = Math.max(1, (int)(size * aspect));
         }

         int drawX = x + (size - drawW) / 2;
         int drawY = y + (size - drawH) / 2;
         RenderUtils.drawTextureUV(context, tex, drawX, drawY, drawW, drawH, minU, minV, maxU, maxV);
      } catch (Throwable var20) {
         this.drawFallbackFace(context, entity, x, y, size);
      }
   }

   private Object findHeadCuboid(List cuboids) {
      Object best = null;
      float bestArea = -1.0F;

      for (Object c : cuboids) {
         if (c != null) {
            float w = Math.abs(getFloatField(c, "maxX") - getFloatField(c, "minX"));
            float h = Math.abs(getFloatField(c, "maxY") - getFloatField(c, "minY"));
            float area = w * h;
            if (area > bestArea) {
               bestArea = area;
               best = c;
            }
         }
      }

      return best;
   }

   private static float getFloatField(Object obj, String name) {
      Number n = (Number)getFieldValue(obj, name, name);
      return n == null ? 0.0F : n.floatValue();
   }

   private Object findFrontQuad(Object[] sides) {
      for (Object quad : sides) {
         if (quad != null) {
            Object dir = getFieldValue(quad, "direction", "field_21618");
            if (dir instanceof org.joml.Vector3f && ((org.joml.Vector3f)dir).z() < -0.5F) {
               return quad;
            }
         }
      }

      return null;
   }

   private Object findHeadPart(EntityModel<?> model) {
      try {
         if (model instanceof ModelWithHead) {
            Object head = ((ModelWithHead)model).getHead();
            if (head != null) {
               return head;
            }
         }
      } catch (Throwable var3) {
      }

      try {
         Object headParts = invokeMethod(model, "getHeadParts", "method_22946");
         if (headParts instanceof Iterable) {
            for (Object part : (Iterable)headParts) {
               if (part != null) {
                  return part;
               }
            }
         }
      } catch (Throwable var4) {
      }

      return null;
   }

   private static Object getFieldValue(Object obj, String yarnName, String obfName) {
      for (Class<?> c = obj.getClass(); c != null; c = c.getSuperclass()) {
         try {
            Field f = c.getDeclaredField(yarnName);
            f.setAccessible(true);
            return f.get(obj);
         } catch (Throwable var6) {
         }

         try {
            Field f = c.getDeclaredField(obfName);
            f.setAccessible(true);
            return f.get(obj);
         } catch (Throwable var7) {
         }
      }

      return null;
   }

   private static Object invokeMethod(Object obj, String yarnName, String obfName) {
      for (Class<?> c = obj.getClass(); c != null; c = c.getSuperclass()) {
         try {
            Method m = c.getDeclaredMethod(yarnName);
            m.setAccessible(true);
            return m.invoke(obj);
         } catch (Throwable var5) {
         }

         try {
            Method m = c.getDeclaredMethod(obfName);
            m.setAccessible(true);
            return m.invoke(obj);
         } catch (Throwable var6) {
         }
      }

      return null;
   }

   private void drawFallbackFace(DrawContext context, LivingEntity entity, int x, int y, int size) {
      try {
         Identifier tex = DefaultSkinHelper.getTexture();
         EntityRenderer<?, ?> renderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(entity);
         if (renderer instanceof LivingEntityRenderer) {
            
            if (entity instanceof net.minecraft.client.network.AbstractClientPlayerEntity a2) { tex = a2.getSkin().body().texturePath(); }
         }

         if (tex == null) {
            tex = DefaultSkinHelper.getTexture();
         }
         context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, tex, x, y, 0.0F, 0.0F, 16, 16, 64, 64);
      } catch (Throwable var7) {
      }
   }

   private void drawPlayerHeadFace(DrawContext context, Identifier skinTexture, int x, int y, int size) {
      try {
         if (skinTexture == null) {
            skinTexture = DefaultSkinHelper.getTexture();
         }
         context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, skinTexture, x, y, 8.0F, 8.0F, size, size, 8, 8, 64, 64);
         context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, skinTexture, x, y, 40.0F, 8.0F, size, size, 8, 8, 64, 64);
      } catch (Throwable var7) {
      }
   }

   private Identifier getPlayerSkin(PlayerEntity player) {
      try {
         if (player instanceof AbstractClientPlayerEntity) {
            return ((AbstractClientPlayerEntity)player).getSkin().body().texturePath();
         }
      } catch (Throwable var3) {
      }

      return DefaultSkinHelper.getTexture();
   }
}
