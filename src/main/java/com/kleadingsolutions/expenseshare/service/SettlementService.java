package com.kleadingsolutions.expenseshare.service;

import com.kleadingsolutions.expenseshare.model.Settlement;

import java.math.BigDecimal;
import java.util.UUID;

public interface SettlementService {
    /**
     * Create a settlement between payer and receiver within a group.
     * Atomic: debits payer and credits receiver and records ledger entries and settlement row.
     *
     * @param groupId    group where settlement occurs
     * @param payerId    user who pays
     * @param receiverId user who receives
     * @param amount     amount to transfer (positive)
     * @param actorId    authenticated actor performing the action
     * @return persisted Settlement
     */
    Settlement settle(UUID groupId, UUID payerId, UUID receiverId, BigDecimal amount, UUID actorId);
}