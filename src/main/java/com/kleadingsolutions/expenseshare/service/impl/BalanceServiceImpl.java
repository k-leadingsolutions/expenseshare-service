package com.kleadingsolutions.expenseshare.service.impl;

import com.kleadingsolutions.expenseshare.aop.LogExecution;
import com.kleadingsolutions.expenseshare.model.Balance;
import com.kleadingsolutions.expenseshare.repository.BalanceRepository;
import com.kleadingsolutions.expenseshare.repository.LedgerEntryRepository;
import com.kleadingsolutions.expenseshare.service.BalanceService;
import com.kleadingsolutions.expenseshare.util.MoneyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BalanceServiceImpl implements BalanceService {

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

        if (reconcile) {
            // Acquire a lock when reconciling to avoid races with concurrent updates
            Optional<Balance> locked = balanceRepository.findLockedByGroupIdAndUserId(groupId, userId);
            BigDecimal finalSum = sum;
            Balance b = locked.orElseGet(() -> {
                Balance nb = Balance.builder()
                        .groupId(groupId)
                        .userId(userId)
                        .balance(finalSum)
                        .build();
                return balanceRepository.save(nb);
            });
            b.setBalance(sum);
            balanceRepository.save(b);
        }
        return sum;
    }

    @Override
    @Transactional
    @LogExecution(includeArgs = true, includeResult = false, warnThresholdMs = 300)
    public void recomputeAndReconcileGroup(UUID groupId) {
        // Recompute for balances that exist for this group (avoid all records filter)
        List<Balance> balances = balanceRepository.findByGroupId(groupId);

        for (Balance b : balances) {
            BigDecimal recomputed = ledgerEntryRepository.sumAmountByGroupIdAndUserId(groupId, b.getUserId());
            if (recomputed == null) recomputed = BigDecimal.ZERO;
            b.setBalance(MoneyUtils.scale(recomputed));
            balanceRepository.save(b);
        }
    }
}