package dev.m3odbydr.client;

import dev.m3odbydr.client.ui.LiveGuiScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

public final class M3odbydrClient implements ClientModInitializer {
    public static final String MOD_ID = "m3odbydr";

    private static KeyBinding liveGuiKey;
    private static KeyBinding zoomKey;
    private static KeyBinding hudKey;
    private static KeyBinding boostKey;
    private static KeyBinding freelookKey;
    private static KeyBinding fullbrightKey;

    @Override
    public void onInitializeClient() {
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "general"));
        liveGuiKey = key("key.m3odbydr.livegui", GLFW.GLFW_KEY_RIGHT_SHIFT, category);
        zoomKey = key("key.m3odbydr.zoom", GLFW.GLFW_KEY_C, category);
        hudKey = key("key.m3odbydr.hud", GLFW.GLFW_KEY_F7, category);
        boostKey = key("key.m3odbydr.superres", GLFW.GLFW_KEY_F8, category);
        freelookKey = key("key.m3odbydr.freelook", GLFW.GLFW_KEY_LEFT_ALT, category);
        fullbrightKey = key("key.m3odbydr.fullbright", GLFW.GLFW_KEY_B, category);

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        HudRenderCallback.EVENT.register((context, tickCounter) -> renderHud(context));
        System.out.println("[m3odbydr!!] Clean Fabric 1.21.11 client loaded");
    }

    private static KeyBinding key(String translation, int code, KeyBinding.Category category) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(translation, InputUtil.Type.KEYSYM, code, category));
    }

    private void tick(MinecraftClient client) {
        while (liveGuiKey.wasPressed()) {
            if (client.currentScreen == null) client.setScreen(new LiveGuiScreen());
        }
        while (zoomKey.wasPressed()) ClientState.zoom = !ClientState.zoom;
        while (hudKey.wasPressed()) ClientState.hud = !ClientState.hud;
        while (boostKey.wasPressed()) ClientState.setSuperResLite(client, !ClientState.superResLite);
        while (freelookKey.wasPressed()) ClientState.setFreelook(client, !ClientState.freelook);
        while (fullbrightKey.wasPressed()) ClientState.fullbright = !ClientState.fullbright;

        if (client.player == null) {
            if (ClientState.freelook) ClientState.setFreelook(client, false);
            return;
        }

        if (ClientState.sprint && client.currentScreen == null && client.options.forwardKey.isPressed()) {
            client.player.setSprinting(true);
        }
    }

    private void renderHud(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!ClientState.hud || client.player == null || client.options.hudHidden) return;

        int x = 8;
        int y = 8;
        int accent = ClientState.accent();
        BlockPos pos = client.player.getBlockPos();
        int ping = -1;
        if (client.getNetworkHandler() != null) {
            PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
            if (entry != null) ping = entry.getLatency();
        }

        String line1 = "m3odbydr!!  " + client.getCurrentFps() + " FPS";
        String line2 = "XYZ " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + (ping >= 0 ? "  •  " + ping + " ms" : "");
        int width = Math.max(client.textRenderer.getWidth(line1), client.textRenderer.getWidth(line2)) + 16;

        context.fill(x, y, x + width, y + 31, 0xB814151A);
        context.fill(x, y, x + 3, y + 31, accent);
        context.drawTextWithShadow(client.textRenderer, Text.literal(line1), x + 9, y + 5, 0xFFFFFFFF);
        context.drawTextWithShadow(client.textRenderer, Text.literal(line2), x + 9, y + 17, 0xFFB9BBC5);
    }
}
