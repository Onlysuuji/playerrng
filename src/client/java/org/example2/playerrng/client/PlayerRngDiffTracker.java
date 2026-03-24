package org.example2.playerrng.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.Nullable;

public final class PlayerRngDiffTracker {
    private static final long MASK_48 = (1L << 48) - 1;

    private static @Nullable Long currentSeed = null;
    private static @Nullable Long previousSeed = null;
    private static int lastDiffSteps = -1;
    private static String lastCause = "unknown";
    private static String displayCause = "unknown";
    private static boolean changedThisTick = false;

    private PlayerRngDiffTracker() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(PlayerRngDiffTracker::onEndTick);
    }

    public static void markCause(String cause) {
        lastCause = cause;
    }


    private static void onEndTick(MinecraftClient client) {
        if (client.player == null) {
            reset();
            return;
        }

        Long now = PlayerRngReader.readCurrent48();
        if (now == null) {
            changedThisTick = false;
            return;
        }

        if (currentSeed == null) {
            currentSeed = now;
            previousSeed = null;
            lastDiffSteps = -1;
            changedThisTick = false;
            displayCause = "unknown";
            return;
        }

        if (!currentSeed.equals(now)) {
            previousSeed = currentSeed;
            currentSeed = now;
            lastDiffSteps = findForwardDistance(previousSeed, currentSeed, 128);
            changedThisTick = true;
            displayCause = lastCause;

            System.out.printf(
                "[player-rng] cause=%s old=0x%012X new=0x%012X steps=%s%n",
                displayCause,
                previousSeed,
                currentSeed,
                lastDiffSteps >= 0 ? Integer.toString(lastDiffSteps) : "unknown"
            );

            lastCause = "unknown";
        } else {
            changedThisTick = false;
        }
    }

    public static void reset() {
        currentSeed = null;
        previousSeed = null;
        lastDiffSteps = -1;
        lastCause = "manual_reset";
        changedThisTick = false;
    }

    private static long nextSeed(long seed) {
        return (seed * 0x5DEECE66DL + 0xBL) & MASK_48;
    }

    private static int findForwardDistance(long from, long to, int maxSteps) {
        long s = from;
        for (int i = 1; i <= maxSteps; i++) {
            s = nextSeed(s);
            if (s == to) {
                return i;
            }
        }
        return -1;
    }

    public static @Nullable Long getCurrentSeed() {
        return currentSeed;
    }

    public static @Nullable Long getPreviousSeed() {
        return previousSeed;
    }

    public static int getLastDiffSteps() {
        return lastDiffSteps;
    }

    public static String getLastCause() {
        return displayCause;
    }

    public static boolean hasChangedThisTick() {
        return changedThisTick;
    }

    public static String formatSeed(@Nullable Long seed) {
        return seed == null ? "N/A" : String.format("0x%012X", seed);
    }
}
