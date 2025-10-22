package com.kleadingsolutions.expenseshare.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementDto {
    private UUID id;
    private UUID groupId;
    private UUID payerId;
    private UUID receiverId;
    private BigDecimal amount;
    private UUID expenseId;
    private String status;
    private LocalDateTime createdAt;
}