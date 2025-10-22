package com.kleadingsolutions.expenseshare.service;

import java.util.UUID;

public interface BalanceService {
    /**
     * Recompute the balance for a specific group and user by aggregating ledger entries.
     * Returns the recomputed value (does not modify DB unless reconcile==true).
     */
    java.math.BigDecimal recomputeBalance(UUID groupId, UUID userId, boolean reconcile);

    /**
     * Recompute and reconcile balances for all members of a group.
     */
    void recomputeAndReconcileGroup(UUID groupId);
}