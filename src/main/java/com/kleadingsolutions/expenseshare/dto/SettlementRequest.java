package com.kleadingsolutions.expenseshare.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementRequest {
    @NotNull(message = "groupId is required")
    private UUID groupId;

    @NotNull(message = "payerId is required")
    private UUID payerId;

    @NotNull(message = "receiverId is required")
    private UUID receiverId;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", inclusive = true, message = "amount must be >= 0.01")
    private BigDecimal amount;
}