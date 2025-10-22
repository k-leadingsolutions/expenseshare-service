package com.kleadingsolutions.expenseshare.dto;

import com.kleadingsolutions.expenseshare.validation.ExpenseSplitsSum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Class-level constraint @ExpenseSplitsSum ensures sum(splits.amount) == amount (within tolerance).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ExpenseSplitsSum(message = "Sum of splits must equal expense amount")
public class CreateExpenseRequest {

    @NotNull(message = "groupId is required")
    private UUID groupId;

    @NotNull(message = "payerId is required")
    private UUID payerId;

    @NotBlank(message = "description is required")
    private String description;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", inclusive = true, message = "amount must be >= 0.01")
    private BigDecimal amount;

    @NotBlank(message = "currency is required")
    private String currency;

    @NotNull(message = "splits are required")
    @Size(min = 1, message = "at least one split is required")
    @Valid
    private List<ExpenseSplitDto> splits;
}