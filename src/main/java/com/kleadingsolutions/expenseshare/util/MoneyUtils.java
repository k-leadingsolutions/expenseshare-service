package com.kleadingsolutions.expenseshare.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Money utility helpers using a fixed scale of 2 (cents).
 *
 * - scale(...) normalizes BigDecimal to scale=2 using HALF_EVEN.
 * - toCents/fromCents convert to/from integer cents for exact arithmetic.
 * - equalsWithTolerance(...) compares amounts at cents precision (exact).
 */
public final class MoneyUtils {
    private static final int SCALE = 2;
    private static final RoundingMode RM = RoundingMode.HALF_EVEN;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100L);

    private MoneyUtils() {}

    public static BigDecimal scale(BigDecimal v) {
        if (v == null) return BigDecimal.ZERO.setScale(SCALE, RM);
        return v.setScale(SCALE, RM);
    }

    /**
     * Convert amount to integer cents (long) after scaling.
     * Throws ArithmeticException if the value doesn't fit into a long.
     */
    public static long toCents(BigDecimal amt) {
        if (amt == null) return 0L;
        BigDecimal scaled = scale(amt);
        return scaled.multiply(HUNDRED).longValueExact();
    }

    /**
     * Convert cents back to BigDecimal with scale=2.
     */
    public static BigDecimal fromCents(long cents) {
        return BigDecimal.valueOf(cents, SCALE);
    }

    /**
     * Compare two amounts at cents precision (exact).
     * Returns true when amounts are equal after rounding to 2 decimal places.
     */
    public static boolean equalsWithTolerance(BigDecimal a, BigDecimal b) {
        long aC = toCents(a);
        long bC = toCents(b);
        return aC == bC;
    }
}