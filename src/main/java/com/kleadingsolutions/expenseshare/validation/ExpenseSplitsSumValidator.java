package com.kleadingsolutions.expenseshare.validation;

import com.kleadingsolutions.expenseshare.dto.CreateExpenseRequest;
import com.kleadingsolutions.expenseshare.dto.ExpenseSplitDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class ExpenseSplitsSumValidator implements ConstraintValidator<ExpenseSplitsSum, CreateExpenseRequest> {

    private static final int SCALE = 2;
    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");

    @Override
    public boolean isValid(CreateExpenseRequest value, ConstraintValidatorContext context) {
        if (value == null) return true; // @NotNull on the param handles null overall
        BigDecimal total = value.getAmount();
        List<ExpenseSplitDto> splits = value.getSplits();
        if (total == null || splits == null || splits.isEmpty()) return true; // other annotations handle requiredness

        BigDecimal sum = splits.stream()
                .map(ExpenseSplitDto::getAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(SCALE, RoundingMode.HALF_EVEN);

        BigDecimal diff = total.setScale(SCALE, RoundingMode.HALF_EVEN).subtract(sum).abs();
        return diff.compareTo(TOLERANCE) <= 0;
    }
}