package com.kleadingsolutions.expenseshare.service.impl;

import com.kleadingsolutions.expenseshare.aop.LogExecution;
import com.kleadingsolutions.expenseshare.enums.Currency;
import com.kleadingsolutions.expenseshare.enums.TransactionType;
import com.kleadingsolutions.expenseshare.exception.BadRequestException;
import com.kleadingsolutions.expenseshare.exception.ForbiddenException;
import com.kleadingsolutions.expenseshare.exception.NotFoundException;
import com.kleadingsolutions.expenseshare.model.Balance;
import com.kleadingsolutions.expenseshare.model.GroupMember;
import com.kleadingsolutions.expenseshare.model.LedgerEntry;
import com.kleadingsolutions.expenseshare.model.Settlement;
import com.kleadingsolutions.expenseshare.repository.BalanceRepository;
import com.kleadingsolutions.expenseshare.repository.GroupMemberRepository;
import com.kleadingsolutions.expenseshare.repository.LedgerEntryRepository;
import com.kleadingsolutions.expenseshare.repository.SettlementRepository;
import com.kleadingsolutions.expenseshare.service.SettlementService;
import com.kleadingsolutions.expenseshare.util.MoneyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Settlement implementation (optimistic retry). Note: marking PENDING/FAILED/COMPLETED
 * is subject to transactional semantics; persisting a FAILED state when the outer
 * transaction rolls back requires a separate REQUIRES_NEW approach.
 */
@Service
@RequiredArgsConstructor
public class SettlementServiceImpl implements SettlementService {

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_BACKOFF_MS = 50L;

    private final BalanceRepository balanceRepository;
    private final SettlementRepository settlementRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final GroupMemberRepository groupMemberRepository;

    @Override
    @Transactional
    @LogExecution(includeArgs = true, includeResult = false, warnThresholdMs = 300)
    public Settlement settle(UUID groupId, UUID payerId, UUID receiverId, BigDecimal amount, UUID initiatedBy) {
        if (groupId == null || payerId == null || receiverId == null) {
            throw new BadRequestException("groupId, payerId and receiverId are required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new BadRequestException("amount must be positive");
        }
        if (payerId.equals(receiverId)) {
            throw new BadRequestException("payer and receiver cannot be the same user");
        }

        // Ensure initiator is a member of the group
        boolean initiatorMember = groupMemberRepository.findByGroupId(groupId).stream()
                .anyMatch(m -> m.getUserId().equals(initiatedBy) && "ACTIVE".equalsIgnoreCase(m.getStatus()));
        if (!initiatorMember) {
            throw new ForbiddenException("Initiator is not a group member");
        }

        // Ensure payer and receiver are members
        Set<UUID> members = groupMemberRepository.findByGroupId(groupId).stream()
                .filter(m -> "ACTIVE".equalsIgnoreCase(m.getStatus()))
                .map(GroupMember::getUserId).collect(Collectors.toSet());
        if (!members.contains(payerId) || !members.contains(receiverId)) {
            throw new NotFoundException("Payer or receiver not a member of the group");
        }

        BigDecimal amt = MoneyUtils.scale(amount);

        // Persist settlement (PENDING) so ledger entries can reference it
        Settlement settlement = Settlement.builder()
                .groupId(groupId)
                .payerId(payerId)
                .receiverId(receiverId)
                .amount(amt)
                .expenseId(null)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();
        Settlement savedSettlement = settlementRepository.save(settlement);
        UUID sid = savedSettlement.getId();

        // Create ledger entries linked to settlement id
        LedgerEntry debit = LedgerEntry.builder()
                .groupId(groupId)
                .userId(payerId)
                .amount(amt.negate())
                .type(TransactionType.SETTLEMENT.name())
                .createdBy(initiatedBy)
                .currency(Currency.AED.name())
                .relatedId(sid)
                .createdAt(LocalDateTime.now())
                .build();

        LedgerEntry credit = LedgerEntry.builder()
                .groupId(groupId)
                .userId(receiverId)
                .amount(amt)
                .type(TransactionType.SETTLEMENT.name())
                .relatedId(sid)
                .currency(Currency.AED.name())
                .createdAt(LocalDateTime.now())
                .createdBy(initiatedBy)
                .build();

        ledgerEntryRepository.saveAll(Arrays.asList(debit, credit));

        // Update balances with optimistic-lock retry
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                Optional<Balance> payerOpt = balanceRepository.findByGroupIdAndUserId(groupId, payerId);
                Balance payerBal = payerOpt.orElseGet(() -> {
                    Balance b = Balance.builder()
                            .groupId(groupId)
                            .userId(payerId)
                            .balance(MoneyUtils.scale(BigDecimal.ZERO))
                            .createdAt(LocalDateTime.now())
                            .build();
                    return balanceRepository.save(b);
                });

                Optional<Balance> recvOpt = balanceRepository.findByGroupIdAndUserId(groupId, receiverId);
                Balance receiverBal = recvOpt.orElseGet(() -> {
                    Balance b = Balance.builder()
                            .groupId(groupId)
                            .userId(receiverId)
                            .balance(MoneyUtils.scale(BigDecimal.ZERO))
                            .createdAt(LocalDateTime.now())
                            .build();
                    return balanceRepository.save(b);
                });

                payerBal.setBalance(MoneyUtils.scale(payerBal.getBalance().subtract(amt)));
                receiverBal.setBalance(MoneyUtils.scale(receiverBal.getBalance().add(amt)));

                balanceRepository.save(payerBal);
                balanceRepository.save(receiverBal);

                // success -> break loop
                break;
            } catch (OptimisticLockingFailureException ex) {
                if (attempt >= MAX_RETRIES) {
                    // NOTE: saving FAILED here may be rolled back with outer tx.
                    savedSettlement.setStatus("FAILED");
                    settlementRepository.save(savedSettlement);
                    throw ex;
                }
                try {
                    Thread.sleep(RETRY_BACKOFF_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ie);
                }
            }
        }

        // Mark settlement completed
        savedSettlement.setStatus("COMPLETED");
        return settlementRepository.save(savedSettlement);
    }
}