package com.kleadingsolutions.expenseshare.repository;

import com.kleadingsolutions.expenseshare.model.Balance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BalanceRepository extends JpaRepository<Balance, UUID> {

    Optional<Balance> findByGroupIdAndUserId(UUID groupId, UUID userId);

    /**
     * Returns the balance for the given group/user while acquiring a PESSIMISTIC_WRITE lock.
     * Using an explicit @Query prevents Spring Data from parsing the method name and
     * interpreting "ForUpdate" as a property.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Balance b where b.groupId = :groupId and b.userId = :userId")
    Optional<Balance> findLockedByGroupIdAndUserId(@Param("groupId") UUID groupId,
                                                   @Param("userId") UUID userId);

    // Added read helper
    List<Balance> findByGroupId(UUID groupId);
}