package org.example2.playerrng.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public final class PlayerRngHudOverlay {
    private PlayerRngHudOverlay() {}

    public static void init() {
        HudRenderCallback.EVENT.register(PlayerRngHudOverlay::render);
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) {
            return;
        }

        int x = 8;
        int y = 8;
        int lineHeight = 10;

        String line1 = "RNG:  " + PlayerRngDiffTracker.formatSeed(PlayerRngDiffTracker.getCurrentSeed());
        String line2 = "Prev: " + PlayerRngDiffTracker.formatSeed(PlayerRngDiffTracker.getPreviousSeed());

        int steps = PlayerRngDiffTracker.getLastDiffSteps();
        String stepsText = steps >= 0 ? "+" + steps : "unknown";
        String line3 = "Diff: " + stepsText;

        String line4 = "Cause: " + PlayerRngDiffTracker.getLastCause();

        context.drawTextWithShadow(client.textRenderer, line1, x, y, 0xFFFFFF);
        context.drawTextWithShadow(client.textRenderer, line2, x, y + lineHeight, 0xAAAAAA);
        context.drawTextWithShadow(client.textRenderer, line3, x, y + lineHeight * 2, 0xFFFF55);
        context.drawTextWithShadow(client.textRenderer, line4, x, y + lineHeight * 3, 0x55FFFF);
    }
}
