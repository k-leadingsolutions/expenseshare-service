package com.kleadingsolutions.expenseshare.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseDto {
    private UUID id;
    private UUID groupId;
    private UUID createdBy;
    private String description;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime createdAt;
}