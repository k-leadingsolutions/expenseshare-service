package com.kleadingsolutions.expenseshare.dto;

import com.kleadingsolutions.expenseshare.enums.ShareType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseSplitDto {
    @NotNull(message = "userId is required")
    private UUID userId;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", inclusive = true, message = "amount must be >= 0.01")
    private BigDecimal amount;

    @NotBlank(message = "shareType is required")
    private ShareType shareType; // EQUAL, CUSTOM, PERCENT
}