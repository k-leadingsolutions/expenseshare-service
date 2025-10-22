package com.kleadingsolutions.expenseshare.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceDto {
    private UUID userId;
    private BigDecimal balance;
}