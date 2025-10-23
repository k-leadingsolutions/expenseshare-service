package com.kleadingsolutions.expenseshare.repository;

import com.kleadingsolutions.expenseshare.model.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {
    List<GroupMember> findByGroupId(UUID groupId);
    List<GroupMember> findByUserId(UUID userId);
    Optional<GroupMember> findByGroupIdAndUserId(UUID groupId, UUID userId);
    /**
     * Check if a user is in a group with the given status (e.g., "ACTIVE").
     * Implemented by Spring Data JPA.
     */
    boolean existsByGroupIdAndUserIdAndStatus(UUID groupId, UUID userId, String status);

    /**
     * Convenience exists check (group + user) regardless of status.
     */
    boolean existsByGroupIdAndUserId(UUID groupId, UUID userId);
}