package com.kleadingsolutions.expenseshare.service.impl;

import com.kleadingsolutions.expenseshare.aop.LogExecution;
import com.kleadingsolutions.expenseshare.model.Balance;
import com.kleadingsolutions.expenseshare.repository.BalanceRepository;
import com.kleadingsolutions.expenseshare.repository.LedgerEntryRepository;
import com.kleadingsolutions.expenseshare.service.BalanceService;
import com.kleadingsolutions.expenseshare.util.MoneyUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BalanceServiceImpl implements BalanceService {

    private static final Logger log = LoggerFactory.getLogger(BalanceServiceImpl.class);

    private final LedgerEntryRepository ledgerEntryRepository;
    private final BalanceRepository balanceRepository;

    /**
     * Recompute the balance for a user from the ledger. When reconcile==true the persisted
     * balance row will be updated (locked) with the recomputed amount.
     */
    @Override
    @Transactional
    @LogExecution(includeArgs = true, includeResult = false, warnThresholdMs = 300)
    public BigDecimal recomputeBalance(UUID groupId, UUID userId, boolean reconcile) {
        BigDecimal sum = ledgerEntryRepository.sumAmountByGroupIdAndUserId(groupId, userId);
        if (sum == null) sum = BigDecimal.ZERO;
        sum = MoneyUtils.scale(sum);

        log.debug("Recomputed ledger sum for group={} user={} sum={}", groupId, userId, sum);

        if (reconcile) {
            // Acquire a lock when reconciling to avoid races with concurrent updates
            Optional<Balance> locked = balanceRepository.findLockedByGroupIdAndUserId(groupId, userId);
            BigDecimal finalSum = sum;
            Balance b;
            if (locked.isPresent()) {
                b = locked.get();
            } else {
                // Create path with concurrent-insert handling
                Balance nb = Balance.builder()
                        .groupId(groupId)
                        .userId(userId)
                        .balance(finalSum)
                        .build();
                try {
                    Balance saved = balanceRepository.save(nb);
                    log.info("Created new Balance row id={} for group={} user={} balance={}", saved.getId(), groupId, userId, finalSum);
                    // Acquire lock on saved row
                    b = balanceRepository.findLockedByGroupIdAndUserId(groupId, userId).orElse(saved);
                } catch (DataIntegrityViolationException dive) {
                    // concurrent insert happened; re-query the locked row
                    log.warn("Concurrent insert detected when creating balance for group={} user={}, re-querying locked row", groupId, userId);
                    b = balanceRepository.findLockedByGroupIdAndUserId(groupId, userId)
                            .orElseThrow(() -> new IllegalStateException("Balance insert conflict and no row found", dive));
                }
            }

            b.setBalance(sum);
            balanceRepository.save(b);
            log.info("Reconciled balance for group={} user={} updated_balance={}", groupId, userId, b.getBalance());
        }
        return sum;
    }

    @Override
    @Transactional
    @LogExecution(includeArgs = true, includeResult = false, warnThresholdMs = 300)
    public void recomputeAndReconcileGroup(UUID groupId) {
        log.info("Starting group reconcile for group={}", groupId);
        // Recompute for balances that exist for this group (avoid all records filter)
        List<Balance> balances = balanceRepository.findByGroupId(groupId);

        // Lock and update in deterministic order to avoid deadlocks
        List<UUID> userIds = balances.stream()
                .map(Balance::getUserId)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());

        for (UUID userId : userIds) {
            // Lock the balance row before recomputing/updating
            Optional<Balance> lockedOpt = balanceRepository.findLockedByGroupIdAndUserId(groupId, userId);
            Balance b = lockedOpt.orElseThrow(() -> new IllegalStateException("Balance row disappeared while reconciling for user=" + userId));

            BigDecimal recomputed = ledgerEntryRepository.sumAmountByGroupIdAndUserId(groupId, userId);
            if (recomputed == null) recomputed = BigDecimal.ZERO;
            b.setBalance(MoneyUtils.scale(recomputed));
            balanceRepository.save(b);
            log.debug("Reconciled user={} group={} new_balance={}", userId, groupId, b.getBalance());
        }

        log.info("Completed group reconcile for group={}", groupId);
    }
}