package org.example2.playerrng.client;

import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public final class PlayerRngHudOverlay {
    private static final String[] MEMO_LINES = {
            "\u30E1\u30E2(1.21.1):",
            "\u30C9\u30ED\u30C3\u30D7 +4",
            "\u98DF\u3079\u308B +\u53EF\u5909 / \u8FFD\u52A0\u52B9\u679C\u3076\u3093\u52A0\u7B97",
            "\u30A8\u30F3\u30C1\u30E3\u30F3\u30C8 +1 / \u91D1\u5E8A +1",
            "\u88C5\u5099\u5909\u66F4 +2 / \u30AF\u30ED\u30B9\u30DC\u30A6 +1",
            "XP\u30AA\u30FC\u30D6\u9078\u629E +1/tick",
            "XP\u4FEE\u7E55\u9078\u629E +1",
            "\u8D70\u308B(\u7C92\u5B50) +4",
            "\u30A8\u30D5\u30A7\u30AF\u30C8 +1/tick",
            "\u30A8\u30D5\u30A7\u30AF\u30C8\u7C92\u5B50\u767A\u751F\u6642 +7",
            "\u6C34\u7A81\u5165 +132(\u901A\u5E38\u5E45)",
            "\u6C34\u4E2D\u79FB\u52D5\u97F3 +2",
            "\u6EBA\u308C\u6CE1 +96",
            "\u88AB\u30C0\u30E1\u30FC\u30B8\u97F3 +2"
    };

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
        Long predictedSeed = PlayerRngDiffTracker.getPredictedSeed();
        Long baseSeed = PlayerRngDiffTracker.getPredictionBaseSeed();
        boolean predictionReady = predictedSeed != null && PlayerRngDiffTracker.getCurrentSeed() != null;
        boolean predictionMatches = predictionReady && PlayerRngDiffTracker.predictionMatchesCurrent();

        context.drawTextWithShadow(
                client.textRenderer,
                "RNG:  " + PlayerRngDiffTracker.formatSeed(PlayerRngDiffTracker.getCurrentSeed()),
                x,
                y,
                0xFFFFFF
        );
        context.drawTextWithShadow(
                client.textRenderer,
                "Base: " + PlayerRngDiffTracker.formatSeed(baseSeed),
                x,
                y + lineHeight,
                0xCCCCCC
        );
        context.drawTextWithShadow(
                client.textRenderer,
                "Pred: " + PlayerRngDiffTracker.formatSeed(predictedSeed),
                x,
                y + lineHeight * 2,
                !predictionReady ? 0xAAAAAA : predictionMatches ? 0x55FF55 : 0xFF7777
        );
        context.drawTextWithShadow(
                client.textRenderer,
                "Compare: " + formatComparisonText(predictionReady, predictionMatches),
                x,
                y + lineHeight * 3,
                predictionMatches ? 0x55FF55 : predictionReady ? 0xFF5555 : 0xAAAAAA
        );
        context.drawTextWithShadow(
                client.textRenderer,
                "Current Steps: " + formatTotalSteps(PlayerRngDiffTracker.getCurrentStepsFromBase()),
                x,
                y + lineHeight * 4,
                0xFFFF55
        );
        context.drawTextWithShadow(
                client.textRenderer,
                "Pred Steps: " + formatPredictedSteps(PlayerRngDiffTracker.getPredictedSteps()),
                x,
                y + lineHeight * 5,
                0xFFAA55
        );

        List<PlayerRngDiffTracker.PreviousSeedEntry> previousSeeds = PlayerRngDiffTracker.getPreviousSeeds();
        int previousLines = Math.max(1, previousSeeds.size());
        for (int i = 0; i < previousLines; i++) {
            PlayerRngDiffTracker.PreviousSeedEntry entry = i < previousSeeds.size() ? previousSeeds.get(i) : null;
            Long seed = entry != null ? entry.seed() : null;
            String stepText = entry != null ? formatStepDistance(entry.stepsFromCurrent()) : "unknown";
            String line = String.format("Prev %d: %s (%s)", i + 1, PlayerRngDiffTracker.formatSeed(seed), stepText);
            int color = i == 0 ? 0xAAAAAA : 0x888888;
            context.drawTextWithShadow(client.textRenderer, line, x, y + lineHeight * (i + 6), color);
        }

        int steps = PlayerRngDiffTracker.getLastDiffSteps();
        String stepsText = steps >= 0 ? "+" + steps : "unknown";
        int detailStartY = y + lineHeight * (previousLines + 6);

        context.drawTextWithShadow(client.textRenderer, "Diff: " + stepsText, x, detailStartY, 0xFFFF55);
        context.drawTextWithShadow(
                client.textRenderer,
                "Cause: " + PlayerRngDiffTracker.getLastCause(),
                x,
                detailStartY + lineHeight,
                0x55FFFF
        );
        context.drawTextWithShadow(
                client.textRenderer,
                "Pred Cause: " + PlayerRngDiffTracker.getLastPredictedCause() + " (" + formatPredictedDelta(PlayerRngDiffTracker.getLastPredictedDelta()) + ")",
                x,
                detailStartY + lineHeight * 2,
                0xFFAA55
        );

        int memoStartY = detailStartY + lineHeight * 4;
        for (int i = 0; i < MEMO_LINES.length; i++) {
            int color = i == 0 ? 0xFFAA55 : 0xAAFFAA;
            context.drawTextWithShadow(client.textRenderer, MEMO_LINES[i], x, memoStartY + lineHeight * i, color);
        }
    }

    private static String formatStepDistance(int steps) {
        return steps >= 0 ? "+" + steps + " steps" : "unknown";
    }

    private static String formatPredictedSteps(long steps) {
        return "+" + steps;
    }

    private static String formatTotalSteps(Long steps) {
        return steps == null ? "unknown" : "+" + steps;
    }

    private static String formatPredictedDelta(int steps) {
        return steps > 0 ? "+" + steps : "unknown";
    }

    private static String formatComparisonText(boolean predictionReady, boolean predictionMatches) {
        if (!predictionReady) {
            return "unknown";
        }
        return predictionMatches ? "match" : "mismatch";
    }
}
