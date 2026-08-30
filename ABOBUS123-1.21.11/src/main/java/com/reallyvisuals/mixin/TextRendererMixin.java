package com.reallyvisuals.mixin;

import com.reallyvisuals.module.ModuleManager;
import com.reallyvisuals.module.StreamerMode;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TextRenderer.class)
public abstract class TextRendererMixin {
   private static final ThreadLocal<Boolean> RECURSION_GUARD = ThreadLocal.withInitial(() -> false);

   @ModifyVariable(
      method = "draw(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)V",
      at = @At("HEAD"),
      ordinal = 0,
      argsOnly = true
   )
   private String modifyDrawString(String text) {
      if (Boolean.TRUE.equals(RECURSION_GUARD.get())) {
         return text;
      }

      try {
         RECURSION_GUARD.set(true);
         return StreamerMode.processText(text);
      } finally {
         RECURSION_GUARD.set(false);
      }
   }

   @ModifyVariable(
      method = "draw(Lnet/minecraft/text/OrderedText;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)V",
      at = @At("HEAD"),
      ordinal = 0,
      argsOnly = true
   )
   private OrderedText modifyDrawOrderedText(OrderedText text) {
      if (text != null && !Boolean.TRUE.equals(RECURSION_GUARD.get())) {
         StreamerMode mode = (StreamerMode)ModuleManager.getInstance().getModule("Streamer Mode");
         if (mode != null && mode.isEnabled()) {
            try {
               RECURSION_GUARD.set(true);
               StringBuilder sb = new StringBuilder();
               text.accept((index, style, codePoint) -> {
                  sb.appendCodePoint(codePoint);
                  return true;
               });
               String original = sb.toString();
               String processed = StreamerMode.processText(original);
               return original.equals(processed) ? text : visitor -> {
                  int idx = 0;
                  int i = 0;

                  while (i < processed.length()) {
                     int cp = processed.codePointAt(i);
                     if (!visitor.accept(idx++, Style.EMPTY, cp)) {
                        return false;
                     }

                     i += Character.charCount(cp);
                  }

                  return true;
               };
            } finally {
               RECURSION_GUARD.set(false);
            }
         } else {
            return text;
         }
      } else {
         return text;
      }
   }
}
