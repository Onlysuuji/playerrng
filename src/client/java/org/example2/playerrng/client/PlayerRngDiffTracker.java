package org.example2.playerrng.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.Nullable;

public final class PlayerRngDiffTracker {
    private static final long MASK_48 = (1L << 48) - 1;
    private static final int MAX_PREVIOUS_SEEDS = 10;
    private static final int MAX_DIFF_SEARCH = 512;
    private static final Object PREDICTION_LOCK = new Object();

    public record PreviousSeedEntry(long seed, int stepsFromCurrent) {}

    private static volatile @Nullable Long currentSeed = null;
    private static volatile @Nullable Long previousSeed = null;
    private static final ArrayDeque<PreviousSeedEntry> previousSeeds = new ArrayDeque<>();
    private static int lastDiffSteps = -1;
    private static String lastCause = "unknown";
    private static String displayCause = "unknown";
    private static boolean changedThisTick = false;
    private static @Nullable Long predictionBaseSeed = null;
    private static @Nullable Long predictedSeed = null;
    private static @Nullable Long currentStepsFromBase = null;
    private static long predictedSteps = 0L;
    private static String lastPredictedCause = "unknown";
    private static int lastPredictedDelta = 0;

    private PlayerRngDiffTracker() {}

    public static void init() {
        PlayerRngFileLogger.init();
        PlayerRngFileLogger.log("tracker initialized");
        ClientTickEvents.END_CLIENT_TICK.register(PlayerRngDiffTracker::onEndTick);
    }

    public static void markCause(String cause) {
        lastCause = cause;
        PlayerRngFileLogger.log("cause_marked cause=" + cause);
    }

    public static void recordPredictedSteps(String cause, int steps) {
        if (steps <= 0) {
            return;
        }

        markCause(cause);
        Long seedSnapshot = currentSeed;
        if (seedSnapshot == null) {
            PlayerRngFileLogger.log("predicted_ignored cause=" + cause + " delta=+" + steps + " reason=no_current_seed");
            return;
        }

        synchronized (PREDICTION_LOCK) {
            if (predictionBaseSeed == null) {
                predictionBaseSeed = seedSnapshot;
                predictedSeed = seedSnapshot;
                predictedSteps = 0L;
            }

            if (predictedSeed == null) {
                predictedSeed = predictionBaseSeed;
            }

            Long baseSeed = predictionBaseSeed;
            Long predictedBefore = predictedSeed;
            predictedSteps += steps;
            predictedSeed = advanceSeed(predictedSeed, steps);
            lastPredictedCause = cause;
            lastPredictedDelta = steps;

            PlayerRngFileLogger.log(String.format(
                    "predicted cause=%s delta=+%d current=%s base=%s predicted_before=%s predicted_after=%s predicted_steps=+%d",
                    cause,
                    steps,
                    formatSeed(seedSnapshot),
                    formatSeed(baseSeed),
                    formatSeed(predictedBefore),
                    formatSeed(predictedSeed),
                    predictedSteps
            ));
        }
    }

    private static void onEndTick(MinecraftClient client) {
        if (client.player == null) {
            reset("player_missing");
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
            previousSeeds.clear();
            lastDiffSteps = -1;
            changedThisTick = false;
            displayCause = "unknown";
            currentStepsFromBase = 0L;
            initializePrediction(now);
            PlayerRngFileLogger.log("baseline current=" + formatSeed(now));
            return;
        }

        if (!currentSeed.equals(now)) {
            previousSeed = currentSeed;
            currentSeed = now;
            lastDiffSteps = findForwardDistance(previousSeed, currentSeed, MAX_DIFF_SEARCH);
            if (currentStepsFromBase != null && lastDiffSteps >= 0) {
                currentStepsFromBase += lastDiffSteps;
            } else {
                currentStepsFromBase = null;
            }
            shiftPreviousSeedSteps(lastDiffSteps);
            previousSeeds.addFirst(new PreviousSeedEntry(previousSeed, lastDiffSteps));
            while (previousSeeds.size() > MAX_PREVIOUS_SEEDS) {
                previousSeeds.removeLast();
            }
            changedThisTick = true;
            displayCause = lastCause;

            PlayerRngFileLogger.log(String.format(
                    "observed cause=%s old=%s new=%s diff_steps=%s current_steps=%s predicted=%s predicted_steps=+%d compare=%s",
                    displayCause,
                    formatSeed(previousSeed),
                    formatSeed(currentSeed),
                    lastDiffSteps >= 0 ? "+" + lastDiffSteps : "unknown",
                    currentStepsFromBase == null ? "unknown" : "+" + currentStepsFromBase,
                    formatSeed(getPredictedSeed()),
                    getPredictedSteps(),
                    predictionMatchesCurrent() ? "match" : "mismatch"
            ));

            lastCause = "unknown";
        } else {
            changedThisTick = false;
        }
    }

    public static void reset() {
        reset("manual_reset");
    }

    public static void reset(String reason) {
        currentSeed = null;
        previousSeed = null;
        previousSeeds.clear();
        lastDiffSteps = -1;
        lastCause = reason;
        displayCause = "unknown";
        changedThisTick = false;
        currentStepsFromBase = null;
        synchronized (PREDICTION_LOCK) {
            predictionBaseSeed = null;
            predictedSeed = null;
            predictedSteps = 0L;
            lastPredictedCause = "unknown";
            lastPredictedDelta = 0;
        }
        PlayerRngFileLogger.log("reset reason=" + reason);
    }

    private static long nextSeed(long seed) {
        return (seed * 0x5DEECE66DL + 0xBL) & MASK_48;
    }

    private static long advanceSeed(long seed, long steps) {
        long result = seed;
        for (long i = 0; i < steps; i++) {
            result = nextSeed(result);
        }
        return result;
    }

    private static void initializePrediction(long seed) {
        synchronized (PREDICTION_LOCK) {
            if (predictionBaseSeed != null) {
                return;
            }
            predictionBaseSeed = seed;
            predictedSeed = seed;
            predictedSteps = 0L;
            lastPredictedCause = "baseline";
            lastPredictedDelta = 0;
            PlayerRngFileLogger.log("prediction_initialized base=" + formatSeed(seed));
        }
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

    private static void shiftPreviousSeedSteps(int deltaSteps) {
        if (previousSeeds.isEmpty()) {
            return;
        }

        List<PreviousSeedEntry> shiftedEntries = new ArrayList<>(previousSeeds.size());
        for (PreviousSeedEntry entry : previousSeeds) {
            int stepsFromCurrent = deltaSteps >= 0 && entry.stepsFromCurrent() >= 0
                    ? entry.stepsFromCurrent() + deltaSteps
                    : -1;
            shiftedEntries.add(new PreviousSeedEntry(entry.seed(), stepsFromCurrent));
        }

        previousSeeds.clear();
        previousSeeds.addAll(shiftedEntries);
    }

    public static @Nullable Long getCurrentSeed() {
        return currentSeed;
    }

    public static @Nullable Long getPreviousSeed() {
        return previousSeed;
    }

    public static @Nullable Long getPredictionBaseSeed() {
        synchronized (PREDICTION_LOCK) {
            return predictionBaseSeed;
        }
    }

    public static @Nullable Long getPredictedSeed() {
        synchronized (PREDICTION_LOCK) {
            return predictedSeed;
        }
    }

    public static long getPredictedSteps() {
        synchronized (PREDICTION_LOCK) {
            return predictedSteps;
        }
    }

    public static @Nullable Long getCurrentStepsFromBase() {
        return currentStepsFromBase;
    }

    public static String getLastPredictedCause() {
        synchronized (PREDICTION_LOCK) {
            return lastPredictedCause;
        }
    }

    public static int getLastPredictedDelta() {
        synchronized (PREDICTION_LOCK) {
            return lastPredictedDelta;
        }
    }

    public static boolean predictionMatchesCurrent() {
        synchronized (PREDICTION_LOCK) {
            return currentSeed != null && predictedSeed != null && currentSeed.equals(predictedSeed);
        }
    }

    public static List<PreviousSeedEntry> getPreviousSeeds() {
        return new ArrayList<>(previousSeeds);
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
