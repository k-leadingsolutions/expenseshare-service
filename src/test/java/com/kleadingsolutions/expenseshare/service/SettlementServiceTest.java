package com.kleadingsolutions.expenseshare.service;

import com.kleadingsolutions.expenseshare.model.Balance;
import com.kleadingsolutions.expenseshare.model.GroupMember;
import com.kleadingsolutions.expenseshare.model.Settlement;
import com.kleadingsolutions.expenseshare.repository.BalanceRepository;
import com.kleadingsolutions.expenseshare.repository.GroupMemberRepository;
import com.kleadingsolutions.expenseshare.repository.LedgerEntryRepository;
import com.kleadingsolutions.expenseshare.repository.SettlementRepository;
import com.kleadingsolutions.expenseshare.service.impl.SettlementServiceImpl;
import com.kleadingsolutions.expenseshare.util.MoneyUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    BalanceRepository balanceRepository;
    @Mock
    SettlementRepository settlementRepository;
    @Mock
    LedgerEntryRepository ledgerEntryRepository;
    @Mock
    GroupMemberRepository groupMemberRepository;

    @InjectMocks
    SettlementServiceImpl settlementService;

    private UUID groupId;
    private UUID payerId;
    private UUID receiverId;
    private UUID initiator;

    @BeforeEach
    void setUp() {
        groupId = UUID.randomUUID();
        payerId = UUID.randomUUID();
        receiverId = UUID.randomUUID();
        initiator = UUID.randomUUID();
    }

    @Test
    void settle_success_marksCompleted_and_updatesBalances() {
        // Arrange
        GroupMember gmInitiator = GroupMember.builder().groupId(groupId).userId(initiator).status("ACTIVE").build();
        GroupMember gmPayer = GroupMember.builder().groupId(groupId).userId(payerId).status("ACTIVE").build();
        GroupMember gmReceiver = GroupMember.builder().groupId(groupId).userId(receiverId).status("ACTIVE").build();

        when(groupMemberRepository.findByGroupId(groupId)).thenReturn(List.of(gmInitiator, gmPayer, gmReceiver));

        Settlement initial = Settlement.builder()
                .groupId(groupId)
                .payerId(payerId)
                .receiverId(receiverId)
                .amount(MoneyUtils.scale(new BigDecimal("10.00")))
                .status("PENDING")
                .build();
        // simulate save returning object with id
        Settlement saved = Settlement.builder()
                .id(UUID.randomUUID())
                .groupId(groupId)
                .payerId(payerId)
                .receiverId(receiverId)
                .amount(MoneyUtils.scale(new BigDecimal("10.00")))
                .status("PENDING")
                .build();
        when(settlementRepository.save(any(Settlement.class))).thenReturn(saved);

        Balance payerBal = Balance.builder().groupId(groupId).userId(payerId).balance(MoneyUtils.scale(new BigDecimal("20.00"))).build();
        Balance recvBal = Balance.builder().groupId(groupId).userId(receiverId).balance(MoneyUtils.scale(new BigDecimal("0.00"))).build();
        when(balanceRepository.findByGroupIdAndUserId(groupId, payerId)).thenReturn(Optional.of(payerBal));
        when(balanceRepository.findByGroupIdAndUserId(groupId, receiverId)).thenReturn(Optional.of(recvBal));

        // ledger saveAll: we don't need to return anything specific
        when(ledgerEntryRepository.saveAll(anyList())).thenReturn(List.of());

        // Act
        Settlement result = settlementService.settle(groupId, payerId, receiverId, new BigDecimal("10.00"), initiator);

        // Assert
        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());

        // Verify interactions
        verify(settlementRepository, atLeast(1)).save(any(Settlement.class)); // initial save and final save
        verify(ledgerEntryRepository, times(1)).saveAll(anyList());
        // balances saved at least twice (payer and receiver)
        verify(balanceRepository, atLeast(2)).save(any(Balance.class));
    }

    @Test
    void settle_retriesOnOptimisticLockingAndSucceeds() {
        // Arrange
        GroupMember gmInitiator = GroupMember.builder().groupId(groupId).userId(initiator).status("ACTIVE").build();
        GroupMember gmPayer = GroupMember.builder().groupId(groupId).userId(payerId).status("ACTIVE").build();
        GroupMember gmReceiver = GroupMember.builder().groupId(groupId).userId(receiverId).status("ACTIVE").build();

        when(groupMemberRepository.findByGroupId(groupId)).thenReturn(List.of(gmInitiator, gmPayer, gmReceiver));

        Settlement saved = Settlement.builder()
                .id(UUID.randomUUID())
                .groupId(groupId)
                .payerId(payerId)
                .receiverId(receiverId)
                .amount(MoneyUtils.scale(new BigDecimal("5.00")))
                .status("PENDING")
                .build();
        when(settlementRepository.save(any(Settlement.class))).thenReturn(saved);

        Balance payerBal = Balance.builder().groupId(groupId).userId(payerId).balance(MoneyUtils.scale(new BigDecimal("20.00"))).build();
        Balance recvBal = Balance.builder().groupId(groupId).userId(receiverId).balance(MoneyUtils.scale(new BigDecimal("0.00"))).build();
        when(balanceRepository.findByGroupIdAndUserId(groupId, payerId)).thenReturn(Optional.of(payerBal));
        when(balanceRepository.findByGroupIdAndUserId(groupId, receiverId)).thenReturn(Optional.of(recvBal));

        // simulate optimistic locking conflict on first save call, then succeed on subsequent calls
        final var callCount = new java.util.concurrent.atomic.AtomicInteger(0);
        when(balanceRepository.save(any(Balance.class))).thenAnswer(invocation -> {
            if (callCount.getAndIncrement() == 0) {
                throw new OptimisticLockingFailureException("simulated conflict");
            }
            return invocation.getArgument(0);
        });

        when(ledgerEntryRepository.saveAll(anyList())).thenReturn(List.of());

        // Act
        Settlement result = settlementService.settle(groupId, payerId, receiverId, new BigDecimal("5.00"), initiator);

        // Assert
        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());

        // Verify we attempted saves multiple times: at least one retry => >=2 saves attempted
        verify(balanceRepository, atLeast(2)).save(any(Balance.class));
        verify(settlementRepository, atLeast(1)).save(any(Settlement.class));
        verify(ledgerEntryRepository, times(1)).saveAll(anyList());
    }
}