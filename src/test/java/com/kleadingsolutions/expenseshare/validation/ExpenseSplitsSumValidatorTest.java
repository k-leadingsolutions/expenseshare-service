package com.kleadingsolutions.expenseshare.validation;

import com.kleadingsolutions.expenseshare.dto.CreateExpenseRequest;
import com.kleadingsolutions.expenseshare.dto.ExpenseSplitDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseSplitsSumValidatorTest {

    private ExpenseSplitsSumValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ExpenseSplitsSumValidator();
    }

    @Test
    void isValid_returnsTrue_whenValueIsNull() {
        assertTrue(validator.isValid(null, null));
    }

    @Test
    void isValid_returnsTrue_whenTotalOrSplitsAreMissing() {
        CreateExpenseRequest req = Mockito.mock(CreateExpenseRequest.class);

        // total null
        when(req.getAmount()).thenReturn(null);
        assertTrue(validator.isValid(req, null));

        // splits null
        when(req.getAmount()).thenReturn(new BigDecimal("10.00"));
        when(req.getSplits()).thenReturn(null);
        assertTrue(validator.isValid(req, null));

        // splits empty
        when(req.getSplits()).thenReturn(List.of());
        assertTrue(validator.isValid(req, null));
    }

    @Test
    void isValid_returnsTrue_whenSplitsSumExactlyMatchesTotal() {
        CreateExpenseRequest req = Mockito.mock(CreateExpenseRequest.class);
        when(req.getAmount()).thenReturn(new BigDecimal("10.00"));

        ExpenseSplitDto s1 = Mockito.mock(ExpenseSplitDto.class);
        ExpenseSplitDto s2 = Mockito.mock(ExpenseSplitDto.class);
        when(s1.getAmount()).thenReturn(new BigDecimal("6.00"));
        when(s2.getAmount()).thenReturn(new BigDecimal("4.00"));

        when(req.getSplits()).thenReturn(List.of(s1, s2));

        assertTrue(validator.isValid(req, null));
    }

    @Test
    void isValid_returnsTrue_whenDifferenceWithinTolerance() {
        // total 10.00, splits sum 9.99 -> diff 0.01 -> valid
        CreateExpenseRequest req = Mockito.mock(CreateExpenseRequest.class);
        when(req.getAmount()).thenReturn(new BigDecimal("10.00"));

        ExpenseSplitDto s1 = Mockito.mock(ExpenseSplitDto.class);
        ExpenseSplitDto s2 = Mockito.mock(ExpenseSplitDto.class);
        when(s1.getAmount()).thenReturn(new BigDecimal("5.00"));
        when(s2.getAmount()).thenReturn(new BigDecimal("4.99"));

        when(req.getSplits()).thenReturn(List.of(s1, s2));

        assertTrue(validator.isValid(req, null));
    }

    @Test
    void isValid_returnsFalse_whenDifferenceExceedsTolerance() {
        // total 10.00, splits sum 9.98 -> diff 0.02 -> invalid
        CreateExpenseRequest req = Mockito.mock(CreateExpenseRequest.class);
        when(req.getAmount()).thenReturn(new BigDecimal("10.00"));

        ExpenseSplitDto s1 = Mockito.mock(ExpenseSplitDto.class);
        ExpenseSplitDto s2 = Mockito.mock(ExpenseSplitDto.class);
        when(s1.getAmount()).thenReturn(new BigDecimal("5.00"));
        when(s2.getAmount()).thenReturn(new BigDecimal("4.98"));

        when(req.getSplits()).thenReturn(List.of(s1, s2));

        assertFalse(validator.isValid(req, null));
    }

    @Test
    void isValid_handlesRoundingForHighPrecisionSplitAmounts() {
        // splits with more decimals that sum to 10.0000 should be treated as 10.00 after rounding
        CreateExpenseRequest req = Mockito.mock(CreateExpenseRequest.class);
        when(req.getAmount()).thenReturn(new BigDecimal("10.00"));

        ExpenseSplitDto a = Mockito.mock(ExpenseSplitDto.class);
        ExpenseSplitDto b = Mockito.mock(ExpenseSplitDto.class);
        ExpenseSplitDto c = Mockito.mock(ExpenseSplitDto.class);

        when(a.getAmount()).thenReturn(new BigDecimal("3.3333"));
        when(b.getAmount()).thenReturn(new BigDecimal("3.3333"));
        when(c.getAmount()).thenReturn(new BigDecimal("3.3334"));

        when(req.getSplits()).thenReturn(List.of(a, b, c));

        assertTrue(validator.isValid(req, null));
    }
}