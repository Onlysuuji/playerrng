package org.example2.playerrng.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.Nullable;

public final class PlayerRngDiffTracker {
    private static final long MASK_48 = (1L << 48) - 1;
    private static final int MAX_PREVIOUS_SEEDS = 10;
    private static final int MAX_DIFF_SEARCH = 512;
    private static final int UNBOUNDED_NEXT_INT_CANDIDATES = 1 << 16;
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

    private static long[] syncCandidatesAfterObservation = new long[0];
    private static long syncObservationStep = -1L;
    private static @Nullable Long syncedCurrentSeed = null;
    private static String syncStatus = "unsynced";
    private static String lastObservationSource = "unknown";
    private static int lastObservationValue = 0;
    private static boolean hasObservation = false;

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
            predictedSeed = PlayerRngMath.advance(predictedSeed, steps);
            lastPredictedCause = cause;
            lastPredictedDelta = steps;
            refreshSyncStateLocked();

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

    public static void recordUnboundedNextIntObservation(String source, int observedValue) {
        synchronized (PREDICTION_LOCK) {
            lastObservationSource = source;
            lastObservationValue = observedValue;
            hasObservation = true;

            long observationStep = predictedSteps;
            if (syncCandidatesAfterObservation.length == 0 || syncObservationStep < 0L) {
                syncCandidatesAfterObservation = createUnboundedNextIntCandidates(observedValue);
                syncObservationStep = observationStep;
            } else {
                long deltaSteps = observationStep - syncObservationStep;
                if (deltaSteps < 0L) {
                    clearSyncStateLocked("observation_step_rewind");
                    syncCandidatesAfterObservation = createUnboundedNextIntCandidates(observedValue);
                    syncObservationStep = observationStep;
                } else {
                    long[] filteredCandidates = filterObservationCandidates(syncCandidatesAfterObservation, deltaSteps, observedValue);
                    if (filteredCandidates.length == 0) {
                        PlayerRngFileLogger.log(String.format(
                                "sync_observation_mismatch source=%s observed=%s step=+%d action=discard_interval",
                                source,
                                formatObservedValue(observedValue),
                                observationStep
                        ));
                        clearSyncStateLocked("observation_mismatch");
                        syncCandidatesAfterObservation = createUnboundedNextIntCandidates(observedValue);
                        syncObservationStep = observationStep;
                    } else {
                        syncCandidatesAfterObservation = filteredCandidates;
                        syncObservationStep = observationStep;
                    }
                }
            }

            refreshSyncStateLocked();
            PlayerRngFileLogger.log(String.format(
                    "sync_observation source=%s observed=%s step=+%d candidates=%d synced_current=%s",
                    source,
                    formatObservedValue(observedValue),
                    observationStep,
                    syncCandidatesAfterObservation.length,
                    formatSeed(syncedCurrentSeed)
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

        synchronized (PREDICTION_LOCK) {
            refreshSyncStateLocked();
            validateSyncAgainstCurrentLocked();
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
            syncCandidatesAfterObservation = new long[0];
            syncObservationStep = -1L;
            syncedCurrentSeed = null;
            syncStatus = "unsynced";
            lastObservationSource = "unknown";
            lastObservationValue = 0;
            hasObservation = false;
        }
        PlayerRngFileLogger.log("reset reason=" + reason);
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
            s = PlayerRngMath.nextSeed(s);
            if (s == to) {
                return i;
            }
        }
        return -1;
    }

    private static long[] createUnboundedNextIntCandidates(int observedValue) {
        long[] candidates = new long[UNBOUNDED_NEXT_INT_CANDIDATES];
        for (int lowBits = 0; lowBits < UNBOUNDED_NEXT_INT_CANDIDATES; lowBits++) {
            candidates[lowBits] = PlayerRngMath.seedAfterUnboundedNextInt(observedValue, lowBits);
        }
        return candidates;
    }

    private static long[] filterObservationCandidates(long[] candidates, long deltaSteps, int observedValue) {
        long[] filteredCandidates = new long[candidates.length];
        int filteredCount = 0;
        for (long candidate : candidates) {
            long advanced = PlayerRngMath.advance(candidate, deltaSteps);
            if (PlayerRngMath.matchesUnboundedNextIntOutput(advanced, observedValue)) {
                filteredCandidates[filteredCount++] = advanced;
            }
        }
        return Arrays.copyOf(filteredCandidates, filteredCount);
    }

    private static void refreshSyncStateLocked() {
        if (syncCandidatesAfterObservation.length == 0 || syncObservationStep < 0L) {
            syncedCurrentSeed = null;
            syncStatus = hasObservation ? "unsynced" : "waiting_observation";
            return;
        }

        if (syncCandidatesAfterObservation.length == 1) {
            long deltaFromObservation = predictedSteps - syncObservationStep;
            if (deltaFromObservation < 0L) {
                syncedCurrentSeed = null;
                syncStatus = "unsynced";
                return;
            }
            syncedCurrentSeed = PlayerRngMath.advance(syncCandidatesAfterObservation[0], deltaFromObservation);
            syncStatus = "resolved";
            return;
        }

        syncedCurrentSeed = null;
        syncStatus = "candidates=" + syncCandidatesAfterObservation.length;
    }

    private static void validateSyncAgainstCurrentLocked() {
        if (syncedCurrentSeed == null || currentSeed == null) {
            return;
        }

        if (!syncedCurrentSeed.equals(currentSeed)) {
            PlayerRngFileLogger.log(String.format(
                    "sync_current_mismatch actual=%s synced=%s predicted_steps=+%d action=discard_interval",
                    formatSeed(currentSeed),
                    formatSeed(syncedCurrentSeed),
                    predictedSteps
            ));
            clearSyncStateLocked("current_seed_mismatch");
        }
    }

    private static void clearSyncStateLocked(String reason) {
        syncCandidatesAfterObservation = new long[0];
        syncObservationStep = -1L;
        syncedCurrentSeed = null;
        syncStatus = hasObservation ? "unsynced" : "waiting_observation";
        PlayerRngFileLogger.log("sync_reset reason=" + reason);
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

    public static @Nullable Long getSyncedCurrentSeed() {
        synchronized (PREDICTION_LOCK) {
            refreshSyncStateLocked();
            return syncedCurrentSeed;
        }
    }

    public static int getSyncCandidateCount() {
        synchronized (PREDICTION_LOCK) {
            return syncCandidatesAfterObservation.length;
        }
    }

    public static String getSyncStatus() {
        synchronized (PREDICTION_LOCK) {
            refreshSyncStateLocked();
            return syncStatus;
        }
    }

    public static String getLastObservationSource() {
        synchronized (PREDICTION_LOCK) {
            return lastObservationSource;
        }
    }

    public static boolean hasObservation() {
        synchronized (PREDICTION_LOCK) {
            return hasObservation;
        }
    }

    public static int getLastObservationValue() {
        synchronized (PREDICTION_LOCK) {
            return lastObservationValue;
        }
    }

    public static boolean syncMatchesCurrent() {
        synchronized (PREDICTION_LOCK) {
            refreshSyncStateLocked();
            return currentSeed != null && syncedCurrentSeed != null && currentSeed.equals(syncedCurrentSeed);
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

    public static String formatObservedValue(int value) {
        return String.format("0x%08X", value);
    }
}
