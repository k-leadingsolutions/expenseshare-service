package com.kleadingsolutions.expenseshare.service;

import com.kleadingsolutions.expenseshare.dto.CreateExpenseRequest;
import com.kleadingsolutions.expenseshare.dto.ExpenseSplitDto;
import com.kleadingsolutions.expenseshare.model.Expense;
import com.kleadingsolutions.expenseshare.model.GroupMember;
import com.kleadingsolutions.expenseshare.repository.*;
import com.kleadingsolutions.expenseshare.service.impl.ExpenseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExpenseServiceImpl focusing on split/ledger/balance behavior.
 * This test provides mocked GroupMember objects so membership checks pass.
 */
@ExtendWith(MockitoExtension.class)
public class ExpenseServiceTest {

    private ExpenseRepository expenseRepository;
    private GroupMemberRepository groupMemberRepository;
    private LedgerEntryRepository ledgerEntryRepository;
    private BalanceRepository balanceRepository;
    private ExpenseServiceImpl expenseService;
    private ExpenseSplitRepository expenseSplitRepository;

    @BeforeEach
    public void setUp() {
        expenseRepository = mock(ExpenseRepository.class);
        groupMemberRepository = mock(GroupMemberRepository.class);
        ledgerEntryRepository = mock(LedgerEntryRepository.class);
        balanceRepository = mock(BalanceRepository.class);
        expenseSplitRepository = mock(ExpenseSplitRepository.class);

        expenseService = new ExpenseServiceImpl(expenseRepository, groupMemberRepository, ledgerEntryRepository, balanceRepository, expenseSplitRepository);
    }

    @Test
    public void createExpense_equalSplit_shouldCreateExpenseAndLedgerAndBalances() {
        UUID groupId = UUID.randomUUID();
        UUID payer = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        UUID actor = payer;

        // Create mocked GroupMember objects that return the expected getUserId() and getStatus()
        GroupMember gmActor = mock(GroupMember.class);
        when(gmActor.getUserId()).thenReturn(actor);
        when(gmActor.getStatus()).thenReturn("ACTIVE");

        GroupMember gmPayer = mock(GroupMember.class);
        when(gmPayer.getUserId()).thenReturn(payer);

        GroupMember gmU2 = mock(GroupMember.class);
        when(gmU2.getUserId()).thenReturn(u2);
        when(gmU2.getStatus()).thenReturn("ACTIVE");

        // stub membership lookup to return these members
        when(groupMemberRepository.findByGroupId(groupId))
                .thenReturn(List.of(gmActor, gmPayer, gmU2));

        CreateExpenseRequest req = CreateExpenseRequest.builder()
                .groupId(groupId)
                .payerId(payer)
                .description("Dinner")
                .amount(new BigDecimal("100.00"))
                .currency("AED")
                .splits(List.of(
                        ExpenseSplitDto.builder().userId(payer).amount(new BigDecimal("25.00")).build(),
                        ExpenseSplitDto.builder().userId(u2).amount(new BigDecimal("75.00")).build()
                ))
                .build();

        // stub expense save to return expense with id
        ArgumentCaptor<Expense> cap = ArgumentCaptor.forClass(Expense.class);
        when(expenseRepository.save(cap.capture())).thenAnswer(inv -> {
            Expense e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        // Call service
        Expense saved = expenseService.createExpense(req, actor);

        // verify saved expense and side effects
        assertThat(saved).isNotNull();
        assertThat(cap.getValue().getAmount()).isEqualTo(new BigDecimal("100.00").setScale(2));
        verify(ledgerEntryRepository, times(1)).saveAll(anyList());
        // BalanceRepository.save() should be invoked for each participant (2)
        verify(balanceRepository, atLeast(2)).save(any());
    }
}