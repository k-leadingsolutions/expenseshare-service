package com.kleadingsolutions.expenseshare.mapper;

import com.kleadingsolutions.expenseshare.dto.ExpenseDto;
import com.kleadingsolutions.expenseshare.model.Expense;

public final class ExpenseMapper {
    private ExpenseMapper() {}

    public static ExpenseDto toDto(Expense e) {
        if (e == null) return null;
        return ExpenseDto.builder()
                .id(e.getId())
                .groupId(e.getGroupId())
                .createdBy(e.getCreatedBy())
                .description(e.getDescription())
                .amount(e.getAmount())
                .currency(e.getCurrency())
                .createdAt(e.getCreatedAt())
                .build();
    }
}