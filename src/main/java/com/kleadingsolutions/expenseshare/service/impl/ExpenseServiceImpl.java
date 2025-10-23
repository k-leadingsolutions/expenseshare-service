package com.kleadingsolutions.expenseshare.service.impl;

import com.kleadingsolutions.expenseshare.aop.LogExecution;
import com.kleadingsolutions.expenseshare.dto.CreateExpenseRequest;
import com.kleadingsolutions.expenseshare.dto.ExpenseSplitDto;
import com.kleadingsolutions.expenseshare.enums.TransactionType;
import com.kleadingsolutions.expenseshare.exception.BadRequestException;
import com.kleadingsolutions.expenseshare.exception.ForbiddenException;
import com.kleadingsolutions.expenseshare.model.Balance;
import com.kleadingsolutions.expenseshare.model.Expense;
import com.kleadingsolutions.expenseshare.model.ExpenseSplit;
import com.kleadingsolutions.expenseshare.model.LedgerEntry;
import com.kleadingsolutions.expenseshare.repository.*;
import com.kleadingsolutions.expenseshare.service.ExpenseService;
import com.kleadingsolutions.expenseshare.util.MoneyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Expense creation logic:
 * - validate membership
 * - persist expense and splits
 * - append ledger entries (ledger-first)
 * - update materialized balances while locking rows (pessimistic)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final BalanceRepository balanceRepository;
    private final ExpenseSplitRepository expenseSplitRepository;

    @Override
    @Transactional
    @LogExecution(includeArgs = true, includeResult = false, warnThresholdMs = 300)
    public Expense createExpense(CreateExpenseRequest request, UUID actorId) {
        validate(request);

        UUID groupId = request.getGroupId();
        UUID payerId = request.getPayerId();

        // Fetch group members once and check membership
        var members = groupMemberRepository.findByGroupId(groupId);
        boolean actorIsMember = members.stream()
                .anyMatch(m -> Objects.equals(m.getUserId(), actorId) && "ACTIVE".equalsIgnoreCase(m.getStatus()));
        if (!actorIsMember) throw new ForbiddenException("Actor is not a member of the group");

        boolean payerIsMember = members.stream()
                .anyMatch(m -> Objects.equals(m.getUserId(), payerId) && "ACTIVE".equalsIgnoreCase(m.getStatus()));
        if (!payerIsMember) throw new BadRequestException("Payer is not a member of the group");

        // Ensure each split user is member
        for (ExpenseSplitDto s : request.getSplits()) {
            boolean member = members.stream()
                    .anyMatch(m -> Objects.equals(m.getUserId(), s.getUserId()) && "ACTIVE".equalsIgnoreCase(m.getStatus()));
            if (!member) throw new BadRequestException("Split user " + s.getUserId() + " is not a member of the group");
        }

        // Normalize amounts and validate sum equals total (use cents equality)
        BigDecimal normalizedTotal = getNormalizedTotal(request);

        // Persist Expense
        Expense saved = saveExpense(request, actorId, groupId, normalizedTotal);

        // Persist ExpenseSplit records
        List<ExpenseSplit> splits = request.getSplits().stream()
                .map(dto -> ExpenseSplit.builder()
                        .expenseId(saved.getId())
                        .userId(dto.getUserId())
                        .amount(MoneyUtils.scale(dto.getAmount()))
                        .shareType(String.valueOf(dto.getShareType()))
                        .build())
                .collect(Collectors.toList());
        expenseSplitRepository.saveAll(splits);

        // Build per-user balance deltas (accumulate duplicates)
        Map<UUID, BigDecimal> userDeltas = getUserDeltas(request, payerId);

        // Participants = all unique split users and payer (ensure payer is included even if not listed in splits)
        Set<UUID> participants = request.getSplits().stream()
                .map(ExpenseSplitDto::getUserId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        participants.add(payerId);

        Map<UUID, BigDecimal> deltas = computeActualDeltas(userDeltas, payerId, participants, normalizedTotal);
        // Sanity: sum of deltas must be zero (exact cents check)
        long deltasSumCents = deltas.values().stream()
                .mapToLong(MoneyUtils::toCents)
                .sum();
        if (deltasSumCents != 0L) {
            throw new IllegalStateException("Internal error: deltas do not sum to zero (cents): " + deltasSumCents + " -> " + MoneyUtils.fromCents(deltasSumCents));
        }

        // Create ledger entries (ledger-first)
        List<LedgerEntry> ledgerEntries = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (Map.Entry<UUID, BigDecimal> ent : deltas.entrySet()) {
            UUID uid = ent.getKey();
            BigDecimal delta = MoneyUtils.scale(ent.getValue());

            LedgerEntry le = LedgerEntry.builder()
                    .groupId(groupId)
                    .userId(uid)
                    .relatedId(saved.getId())
                    .amount(delta)
                    .createdBy(actorId)
                    .createdAt(now)
                    .currency(request.getCurrency())
                    .type(TransactionType.EXPENSE.name())
                    .build();
            ledgerEntries.add(le);
        }

        // persist ledger entries first (append-only)
        ledgerEntryRepository.saveAll(ledgerEntries);

        // Update/create materialized balances with pessimistic locking in deterministic order
        Set<UUID> affected = new HashSet<>(deltas.keySet());
        List<UUID> ordered = new ArrayList<>(affected);
        ordered.sort(Comparator.naturalOrder());

        Map<UUID, Balance> lockedBalances = new HashMap<>();
        for (UUID user : ordered) {
            lockedBalances.put(user, getOrCreateAndLockBalance(groupId, user, actorId));
        }

        // Apply deltas to balances and persist
        for (Map.Entry<UUID, BigDecimal> ent : deltas.entrySet()) {
            UUID uid = ent.getKey();
            BigDecimal delta = MoneyUtils.scale(ent.getValue());
            Balance bal = lockedBalances.get(uid);
            // If balance was not present for some reason, create it (getOrCreateAndLockBalance ensures existence)
            if (bal == null) {
                bal = Balance.builder()
                        .groupId(groupId)
                        .userId(uid)
                        .balance(delta)
                        .createdBy(actorId)
                        .createdAt(now)
                        .build();
            } else {
                bal.setBalance(MoneyUtils.scale(bal.getBalance().add(delta)));
            }
            balanceRepository.save(bal);
        }

        return saved;
    }

    private Balance getOrCreateAndLockBalance(UUID groupId, UUID userId, UUID actorId) {
        Optional<Balance> locked = balanceRepository.findLockedByGroupIdAndUserId(groupId, userId);
        if (locked.isPresent()) return locked.get();

        Balance newB = Balance.builder()
                .groupId(groupId)
                .userId(userId)
                .createdBy(actorId)
                .balance(MoneyUtils.scale(BigDecimal.ZERO))
                .build();
        try {
            // Attempt to insert the new balance
            Balance saved = balanceRepository.save(newB);
            // Now acquire a PESSIMISTIC_WRITE lock on the saved row and return it
            return balanceRepository.findLockedByGroupIdAndUserId(groupId, userId).orElse(saved);
        } catch (DataIntegrityViolationException ex) {
            // Concurrent insert happened (unique constraint). Re-query with lock and return the existing row.
            Optional<Balance> maybeLocked = balanceRepository.findLockedByGroupIdAndUserId(groupId, userId);
            // Log full context for troubleshooting and throw an explicit exception.
            log.error("Balance insert conflict but no row found for group={} user={} actor={}", groupId, userId, actorId, ex);
            return maybeLocked.orElseThrow(() -> new IllegalStateException("Balance insert conflict and no row found", ex));
        }
    }

    private Expense saveExpense(CreateExpenseRequest request, UUID actorId, UUID groupId, BigDecimal normalizedTotal) {
        Expense e = Expense.builder()
                .groupId(groupId)
                .amount(normalizedTotal)
                .currency(request.getCurrency())
                .description(request.getDescription())
                .createdBy(actorId)
                .createdAt(LocalDateTime.now())
                .build();
        return expenseRepository.save(e);
    }

    private static Map<UUID, BigDecimal> computeActualDeltas(Map<UUID, BigDecimal> userDeltas, UUID payerId, Set<UUID> participantIds, BigDecimal normalizedTotal) {
        Map<UUID, BigDecimal> deltas = new HashMap<>();
        BigDecimal payerShare = userDeltas.getOrDefault(payerId, BigDecimal.ZERO);
        for (UUID uid : participantIds) {
            if (Objects.equals(uid, payerId)) {
                BigDecimal delta = normalizedTotal.subtract(payerShare);
                delta = MoneyUtils.scale(delta);
                deltas.put(uid, delta);
            } else {
                BigDecimal share = userDeltas.getOrDefault(uid, BigDecimal.ZERO);
                BigDecimal delta = share.negate();
                delta = MoneyUtils.scale(delta);
                deltas.put(uid, delta);
            }
        }
        return deltas;
    }

    private static Map<UUID, BigDecimal> getUserDeltas(CreateExpenseRequest request, UUID payerId) {
        Map<UUID, BigDecimal> userDeltas = new HashMap<>();
        for (ExpenseSplitDto s : request.getSplits()) {
            UUID uid = s.getUserId();
            BigDecimal share = MoneyUtils.scale(s.getAmount());
            userDeltas.merge(uid, share, BigDecimal::add);
        }
        return userDeltas;
    }

    private static BigDecimal getNormalizedTotal(CreateExpenseRequest request) {
        BigDecimal normalizedTotal = MoneyUtils.scale(request.getAmount());
        long totalCents = MoneyUtils.toCents(normalizedTotal);

        long sumSplitsCents = request.getSplits().stream()
                .map(s -> MoneyUtils.toCents(MoneyUtils.scale(s.getAmount())))
                .reduce(0L, Long::sum);

        if (sumSplitsCents != totalCents) {
            throw new BadRequestException("Split amounts must sum to total amount. sum=" + MoneyUtils.fromCents(sumSplitsCents) + " total=" + MoneyUtils.fromCents(totalCents));
        }
        return normalizedTotal;
    }

    private static void validate(CreateExpenseRequest request) {
        if (request == null) throw new BadRequestException("request is required");
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be positive");
        }
        if (request.getSplits() == null || request.getSplits().isEmpty()) {
            throw new BadRequestException("Splits are required");
        }
    }
}