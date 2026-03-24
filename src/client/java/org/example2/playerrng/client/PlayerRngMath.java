package org.example2.playerrng.client;

public final class PlayerRngMath {
    private static final long MASK_48 = (1L << 48) - 1L;
    private static final long MULTIPLIER = 0x5DEECE66DL;
    private static final long ADDEND = 0xBL;

    private PlayerRngMath() {}

    public static long nextSeed(long seed) {
        return (seed * MULTIPLIER + ADDEND) & MASK_48;
    }

    public static long advance(long seed, long steps) {
        long remaining = steps;
        long resultMultiplier = 1L;
        long resultAddend = 0L;
        long multiplier = MULTIPLIER;
        long addend = ADDEND;

        while (remaining > 0L) {
            if ((remaining & 1L) != 0L) {
                resultMultiplier = (resultMultiplier * multiplier) & MASK_48;
                resultAddend = (resultAddend * multiplier + addend) & MASK_48;
            }

            addend = ((multiplier + 1L) * addend) & MASK_48;
            multiplier = (multiplier * multiplier) & MASK_48;
            remaining >>= 1;
        }

        return (resultMultiplier * seed + resultAddend) & MASK_48;
    }

    public static long seedAfterUnboundedNextInt(int observedValue, int lowBits) {
        return ((((long) observedValue) & 0xFFFF_FFFFL) << 16) | (lowBits & 0xFFFFL);
    }

    public static boolean matchesUnboundedNextIntOutput(long stateAfterCall, int observedValue) {
        return ((int) (stateAfterCall >>> 16)) == observedValue;
    }
}
