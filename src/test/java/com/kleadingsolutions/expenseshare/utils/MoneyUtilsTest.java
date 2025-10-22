package com.kleadingsolutions.expenseshare.utils;

import com.kleadingsolutions.expenseshare.util.MoneyUtils;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MoneyUtilsTest {

    @Test
    void scale_returnsZeroForNullAndTwoDecimalScaleForValues() {
        BigDecimal scaledNull = MoneyUtils.scale(null);
        assertEquals(new BigDecimal("0.00"), scaledNull);

        BigDecimal v = new BigDecimal("1.237");
        BigDecimal scaled = MoneyUtils.scale(v);
        assertEquals(new BigDecimal("1.24"), scaled); // HALF_EVEN: 1.237 -> 1.24

        BigDecimal v2 = new BigDecimal("1.234");
        assertEquals(new BigDecimal("1.23"), MoneyUtils.scale(v2));
    }

    @Test
    void toCents_and_fromCents_roundTrip() {
        BigDecimal amount = new BigDecimal("1234.56");
        long cents = MoneyUtils.toCents(amount);
        assertEquals(123456L, cents);

        BigDecimal back = MoneyUtils.fromCents(cents);
        assertEquals(new BigDecimal("1234.56"), back);
    }

    @Test
    void equalsWithTolerance_trueWhenAmountsEqualAtCentsPrecision() {
        BigDecimal a = new BigDecimal("10.001"); // scales to 10.00
        BigDecimal b = new BigDecimal("10.000");
        assertTrue(MoneyUtils.equalsWithTolerance(a, b));
    }

    @Test
    void equalsWithTolerance_falseWhenAmountsDifferentAtCentsPrecision() {
        BigDecimal a = new BigDecimal("10.009"); // scales to 10.01
        BigDecimal b = new BigDecimal("10.000");
        assertFalse(MoneyUtils.equalsWithTolerance(a, b));
    }

    @Test
    void toCents_handlesNegativeValues() {
        BigDecimal neg = new BigDecimal("-1.01");
        long cents = MoneyUtils.toCents(neg);
        assertEquals(-101L, cents);
        assertEquals(new BigDecimal("-1.01"), MoneyUtils.fromCents(cents));
    }
}