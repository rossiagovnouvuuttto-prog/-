package dev.m3odbydr.client.ui;

import dev.m3odbydr.client.ClientState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public final class LiveGuiScreen extends Screen {
    private static final String[] LABELS = {"Fullbright", "Zoom", "Freelook", "SuperRes Lite", "Sprint"};
    private static final int WIDTH = 205;
    private static final int HEADER = 30;
    private static final int ROW = 25;

    private boolean dragging;
    private int dragX;
    private int dragY;

    public LiveGuiScreen() {
        super(Text.literal("m3odbydr!! LiveGUI"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = ClientState.panelX;
        int y = ClientState.panelY;
        int accent = ClientState.accent();
        int height = HEADER + 8 + LABELS.length * ROW + 46;
        MinecraftClient client = MinecraftClient.getInstance();

        context.fill(x, y, x + WIDTH, y + height, 0xDB15161B);
        context.fill(x, y, x + WIDTH, y + 2, accent);
        context.fill(x, y + HEADER, x + WIDTH, y + HEADER + 1, 0x55484A55);

        String title = "m3odbydr!! LiveGUI";
        context.drawTextWithShadow(this.textRenderer, Text.literal(title), x + 10, y + 8, 0xFFFFFFFF);
        String stats = client.getCurrentFps() + " FPS  " + ClientState.activeCount() + "/5";
        context.drawTextWithShadow(this.textRenderer, Text.literal(stats), x + WIDTH - this.textRenderer.getWidth(stats) - 10, y + 8, accent);

        int rowY = y + HEADER + 8;
        for (int i = 0; i < LABELS.length; i++) {
            boolean enabled = enabled(i);
            boolean hover = inside(mouseX, mouseY, x + 8, rowY, WIDTH - 16, ROW - 4);
            context.fill(x + 8, rowY, x + WIDTH - 8, rowY + ROW - 4, hover ? 0xD12B2D35 : 0xB8212229);
            context.fill(x + 8, rowY, x + 11, rowY + ROW - 4, enabled ? accent : 0xFF555761);
            context.drawTextWithShadow(this.textRenderer, Text.literal(LABELS[i]), x + 18, rowY + 6, enabled ? 0xFFFFFFFF : 0xFFAFB1BA);
            String state = enabled ? "ON" : "OFF";
            context.drawTextWithShadow(this.textRenderer, Text.literal(state), x + WIDTH - this.textRenderer.getWidth(state) - 16, rowY + 6, enabled ? accent : 0xFF787A84);
            rowY += ROW;
        }

        boolean themeHover = inside(mouseX, mouseY, x + 8, rowY + 1, WIDTH - 16, 20);
        context.fill(x + 8, rowY + 1, x + WIDTH - 8, rowY + 21, themeHover ? 0xD12B2D35 : 0xB8212229);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Theme: " + ClientState.THEMES[ClientState.theme]), x + 14, rowY + 7, accent);

        context.drawTextWithShadow(this.textRenderer, Text.literal("Drag header • Esc closes"), x + 14, rowY + 30, 0xFF8E9099);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x();
        double my = click.y();
        int button = click.button();
        int x = ClientState.panelX;
        int y = ClientState.panelY;

        if (button == 0 && inside(mx, my, x, y, WIDTH, HEADER)) {
            dragging = true;
            dragX = (int) mx - x;
            dragY = (int) my - y;
            return true;
        }

        if (button == 0) {
            int rowY = y + HEADER + 8;
            for (int i = 0; i < LABELS.length; i++) {
                if (inside(mx, my, x + 8, rowY, WIDTH - 16, ROW - 4)) {
                    toggle(i);
                    return true;
                }
                rowY += ROW;
            }
            if (inside(mx, my, x + 8, rowY + 1, WIDTH - 16, 20)) {
                ClientState.theme = (ClientState.theme + 1) % ClientState.THEMES.length;
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (dragging && click.button() == 0) {
            ClientState.panelX = clamp((int) click.x() - dragX, 2, Math.max(2, this.width - WIDTH - 2));
            ClientState.panelY = clamp((int) click.y() - dragY, 2, Math.max(2, this.height - 58));
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        dragging = false;
        return super.mouseReleased(click);
    }

    private static boolean enabled(int i) {
        return switch (i) {
            case 0 -> ClientState.fullbright;
            case 1 -> ClientState.zoom;
            case 2 -> ClientState.freelook;
            case 3 -> ClientState.superResLite;
            case 4 -> ClientState.sprint;
            default -> false;
        };
    }

    private static void toggle(int i) {
        MinecraftClient client = MinecraftClient.getInstance();
        switch (i) {
            case 0 -> ClientState.fullbright = !ClientState.fullbright;
            case 1 -> ClientState.zoom = !ClientState.zoom;
            case 2 -> ClientState.setFreelook(client, !ClientState.freelook);
            case 3 -> ClientState.setSuperResLite(client, !ClientState.superResLite);
            case 4 -> ClientState.sprint = !ClientState.sprint;
            default -> { }
        }
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
