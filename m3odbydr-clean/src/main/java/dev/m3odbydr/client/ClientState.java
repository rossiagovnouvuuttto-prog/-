package dev.m3odbydr.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.Perspective;
import net.minecraft.particle.ParticlesMode;

public final class ClientState {
    private ClientState() {}

    public static boolean hud = true;
    public static boolean zoom = false;
    public static boolean freelook = false;
    public static boolean fullbright = false;
    public static boolean sprint = true;
    public static boolean superResLite = false;

    public static float cameraYaw;
    public static float cameraPitch;
    public static Perspective previousPerspective = Perspective.FIRST_PERSON;

    public static int panelX = 18;
    public static int panelY = 32;
    public static int theme = 0;

    public static final String[] THEMES = {"Crystall Glow", "Neon Cyan", "Sunset Gradient", "Dark Gray"};
    public static final int[] ACCENTS = {0xFF7787FF, 0xFF00E5FF, 0xFFFF7A59, 0xFFA5A6AD};

    private static Integer oldViewDistance;
    private static Double oldEntityDistance;
    private static Boolean oldEntityShadows;
    private static CloudRenderMode oldClouds;
    private static ParticlesMode oldParticles;
    private static Boolean oldCutoutLeaves;

    public static int accent() {
        return ACCENTS[Math.floorMod(theme, ACCENTS.length)];
    }

    public static int activeCount() {
        int n = 0;
        if (fullbright) n++;
        if (zoom) n++;
        if (freelook) n++;
        if (superResLite) n++;
        if (sprint) n++;
        return n;
    }

    public static void setFreelook(MinecraftClient client, boolean enabled) {
        if (freelook == enabled) return;
        if (enabled) {
            if (client.player == null) return;
            previousPerspective = client.options.getPerspective();
            cameraYaw = client.player.getYaw();
            cameraPitch = client.player.getPitch();
            client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
            freelook = true;
        } else {
            freelook = false;
            client.options.setPerspective(previousPerspective);
        }
    }

    public static void setSuperResLite(MinecraftClient client, boolean enabled) {
        if (superResLite == enabled) return;
        if (enabled) {
            oldViewDistance = client.options.getViewDistance().getValue();
            oldEntityDistance = client.options.getEntityDistanceScaling().getValue();
            oldEntityShadows = client.options.getEntityShadows().getValue();
            oldClouds = client.options.getCloudRenderMode().getValue();
            oldParticles = client.options.getParticles().getValue();
            oldCutoutLeaves = client.options.getCutoutLeaves().getValue();

            client.options.getViewDistance().setValue(Math.min(oldViewDistance, 8));
            client.options.getEntityDistanceScaling().setValue(Math.min(oldEntityDistance, 0.75D));
            client.options.getEntityShadows().setValue(false);
            client.options.getCloudRenderMode().setValue(CloudRenderMode.OFF);
            client.options.getParticles().setValue(ParticlesMode.MINIMAL);
            client.options.getCutoutLeaves().setValue(false);
            superResLite = true;
        } else {
            superResLite = false;
            if (oldViewDistance != null) client.options.getViewDistance().setValue(oldViewDistance);
            if (oldEntityDistance != null) client.options.getEntityDistanceScaling().setValue(oldEntityDistance);
            if (oldEntityShadows != null) client.options.getEntityShadows().setValue(oldEntityShadows);
            if (oldClouds != null) client.options.getCloudRenderMode().setValue(oldClouds);
            if (oldParticles != null) client.options.getParticles().setValue(oldParticles);
            if (oldCutoutLeaves != null) client.options.getCutoutLeaves().setValue(oldCutoutLeaves);
        }
    }
}
