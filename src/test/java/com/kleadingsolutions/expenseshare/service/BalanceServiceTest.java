package com.kleadingsolutions.expenseshare.service;

import com.kleadingsolutions.expenseshare.model.Balance;
import com.kleadingsolutions.expenseshare.repository.BalanceRepository;
import com.kleadingsolutions.expenseshare.repository.LedgerEntryRepository;
import com.kleadingsolutions.expenseshare.service.impl.BalanceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {

    @Mock
    LedgerEntryRepository ledgerEntryRepository;

    @Mock
    BalanceRepository balanceRepository;

    @InjectMocks
    BalanceServiceImpl balanceService;

    private UUID groupId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        groupId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    void recomputeBalance_returnsZero_whenNoLedgerEntries() {
        when(ledgerEntryRepository.sumAmountByGroupIdAndUserId(groupId, userId)).thenReturn(null);

        BigDecimal result = balanceService.recomputeBalance(groupId, userId, false);

        assertNotNull(result);
        assertEquals(new BigDecimal("0.00"), result);
        verify(ledgerEntryRepository).sumAmountByGroupIdAndUserId(groupId, userId);
        verifyNoInteractions(balanceRepository);
    }

    @Test
    void recomputeBalance_reconciles_whenRequested_createsBalanceIfMissing() {
        when(ledgerEntryRepository.sumAmountByGroupIdAndUserId(groupId, userId)).thenReturn(new BigDecimal("12.34"));
        when(balanceRepository.findLockedByGroupIdAndUserId(groupId, userId)).thenReturn(Optional.empty());

        Balance created = Balance.builder()
                .groupId(groupId)
                .userId(userId)
                .balance(new BigDecimal("12.34"))
                .build();
        when(balanceRepository.save(Mockito.any(Balance.class))).thenReturn(created);

        BigDecimal result = balanceService.recomputeBalance(groupId, userId, true);

        assertEquals(new BigDecimal("12.34"), result);
        verify(balanceRepository, atLeastOnce()).save(Mockito.any(Balance.class));
        verify(ledgerEntryRepository).sumAmountByGroupIdAndUserId(groupId, userId);
    }

    @Test
    void recomputeAndReconcileGroup_updatesExistingBalances() {
        Balance b1 = Balance.builder().groupId(groupId).userId(userId).balance(new BigDecimal("1.00")).build();
        when(balanceRepository.findByGroupId(groupId)).thenReturn(java.util.List.of(b1));
        when(balanceRepository.findLockedByGroupIdAndUserId(groupId, userId)).thenReturn(Optional.of(b1));
        when(ledgerEntryRepository.sumAmountByGroupIdAndUserId(groupId, userId)).thenReturn(new BigDecimal("5.00"));

        balanceService.recomputeAndReconcileGroup(groupId);

        assertEquals(new BigDecimal("5.00"), b1.getBalance());
        verify(balanceRepository).save(b1);
        verify(ledgerEntryRepository).sumAmountByGroupIdAndUserId(groupId, userId);
    }
}