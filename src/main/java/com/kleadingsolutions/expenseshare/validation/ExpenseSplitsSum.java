package com.kleadingsolutions.expenseshare.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ExpenseSplitsSumValidator.class)
public @interface ExpenseSplitsSum {
    String message() default "splits sum mismatch";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}