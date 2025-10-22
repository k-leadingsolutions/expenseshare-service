package com.kleadingsolutions.expenseshare.repository;

import com.kleadingsolutions.expenseshare.model.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
    List<LedgerEntry> findByGroupId(UUID groupId);

    @Query("select coalesce(sum(le.amount), 0) from LedgerEntry le where le.groupId = :groupId and le.userId = :userId")
    BigDecimal sumAmountByGroupIdAndUserId(UUID groupId, UUID userId);
}