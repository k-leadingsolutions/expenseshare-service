package com.kleadingsolutions.expenseshare.mapper;

import com.kleadingsolutions.expenseshare.dto.SettlementDto;
import com.kleadingsolutions.expenseshare.model.Settlement;

public final class SettlementMapper {
    private SettlementMapper() {}

    public static SettlementDto toDto(Settlement s) {
        if (s == null) return null;
        return SettlementDto.builder()
                .id(s.getId())
                .groupId(s.getGroupId())
                .payerId(s.getPayerId())
                .receiverId(s.getReceiverId())
                .amount(s.getAmount())
                .expenseId(s.getExpenseId())
                .status(s.getStatus())
                .createdAt(s.getCreatedAt())
                .build();
    }
}