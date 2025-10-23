package com.kleadingsolutions.expenseshare.service.impl;

import com.kleadingsolutions.expenseshare.aop.LogExecution;
import com.kleadingsolutions.expenseshare.dto.BalanceDto;
import com.kleadingsolutions.expenseshare.dto.ExpenseDto;
import com.kleadingsolutions.expenseshare.dto.GroupDto;
import com.kleadingsolutions.expenseshare.exception.NotFoundException;
import com.kleadingsolutions.expenseshare.model.Balance;
import com.kleadingsolutions.expenseshare.model.Expense;
import com.kleadingsolutions.expenseshare.model.Group;
import com.kleadingsolutions.expenseshare.model.GroupMember;
import com.kleadingsolutions.expenseshare.repository.*;
import com.kleadingsolutions.expenseshare.service.AuthService;
import com.kleadingsolutions.expenseshare.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of GroupService. Keep methods small and testable.
 *
 * Changes applied:
 * - Added logging at key points (entry, important events, failures).
 * - Reduced duplicate repository calls by reusing fetched lists.
 * - Handled concurrent-insert race when adding a GroupMember (catch DataIntegrityViolationException).
 * - Marked read-only transactions for list methods.
 */
@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {

    private static final Logger log = LoggerFactory.getLogger(GroupServiceImpl.class);

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final BalanceRepository balanceRepository;
    private final AuthService authService;

    @Override
    @LogExecution(includeArgs = true, includeResult = false, warnThresholdMs = 300)
    public List<GroupDto> listGroupsForUser(UUID userId) {
        log.debug("Listing groups for user {}", userId);
        List<GroupMember> memberships = groupMemberRepository.findByUserId(userId);
        List<UUID> groupIds = memberships.stream().map(GroupMember::getGroupId).collect(Collectors.toList());
        if (groupIds.isEmpty()) {
            log.debug("No group memberships found for user {}", userId);
            return Collections.emptyList();
        }
        List<Group> groups = groupRepository.findAllById(groupIds);
        return groups.stream().map(g -> GroupDto.builder()
                .id(g.getId())
                .name(g.getName())
                .createdBy(g.getCreatedBy())
                .createdAt(g.getCreatedAt())
                .build()).collect(Collectors.toList());
    }

    @Override
    @Transactional
    @LogExecution(includeArgs = true, includeResult = false, warnThresholdMs = 300)
    public GroupDto createGroup(String name, UUID creatorId) {
        log.info("Creating group '{}' by user {}", name, creatorId);
        Group g = Group.builder()
                .name(name)
                .createdBy(creatorId)
                .build();
        g = groupRepository.save(g);

        // add creator as member (handle concurrent inserts defensively)
        GroupMember gm = GroupMember.builder()
                .groupId(g.getId())
                .userId(creatorId)
                .joinedAt(LocalDateTime.now())
                .status("ACTIVE")
                .build();
        try {
            groupMemberRepository.save(gm);
            log.info("Added creator {} as member to group {}", creatorId, g.getId());
        } catch (DataIntegrityViolationException dive) {
            // concurrent insert — another thread added the member; that's fine
            log.warn("Concurrent member insert detected when adding creator {} to group {}: {}", creatorId, g.getId(), dive.getMessage());
        }

        return GroupDto.builder()
                .id(g.getId())
                .name(g.getName())
                .createdBy(g.getCreatedBy())
                .createdAt(g.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    @LogExecution(includeArgs = true, includeResult = false, warnThresholdMs = 300)
    public void addMember(UUID groupId, UUID userId, UUID actorId) {
        log.info("Actor {} requests to add user {} to group {}", actorId, userId, groupId);

        groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));

        // fetch members once and reuse
        List<GroupMember> membersForGroup = groupMemberRepository.findByGroupId(groupId);

        // require actor be member
        boolean actorMember = membersForGroup.stream()
                .anyMatch(m -> m.getUserId().equals(actorId) && "ACTIVE".equalsIgnoreCase(m.getStatus()));
        if (!actorMember) {
            log.warn("Actor {} is not a member of group {}", actorId, groupId);
            throw new IllegalArgumentException("Actor is not a member of the group");
        }

        // ensure user exists
        userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));

        // prevent duplicate
        boolean already = membersForGroup.stream()
                .anyMatch(m -> m.getUserId().equals(userId));
        if (already) {
            log.debug("User {} already a member of group {}, skipping add", userId, groupId);
            return;
        }

        GroupMember gm = GroupMember.builder()
                .groupId(groupId)
                .userId(userId)
                .status("ACTIVE")
                .joinedAt(LocalDateTime.now())
                .build();
        try {
            groupMemberRepository.save(gm);
            log.info("User {} added to group {} by {}", userId, groupId, actorId);
        } catch (DataIntegrityViolationException dive) {
            // concurrent insert by another request - treat as already added
            log.warn("Concurrent addMember detected for group={} user={}: {}", groupId, userId, dive.getMessage());
        }
    }

    @Override
    @Transactional
    @LogExecution(includeArgs = true, includeResult = false, warnThresholdMs = 300)
    public void removeMember(UUID groupId, UUID userId, UUID actorId) {
        log.info("Actor {} requests removal of user {} from group {}", actorId, userId, groupId);

        groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));

        // require actor be member
        List<GroupMember> membersForGroup = groupMemberRepository.findByGroupId(groupId);
        boolean actorMember = membersForGroup.stream()
                .anyMatch(m -> m.getUserId().equals(actorId) && "ACTIVE".equalsIgnoreCase(m.getStatus()));
        if (!actorMember) {
            log.warn("Actor {} is not a member of group {}", actorId, groupId);
            throw new IllegalArgumentException("Actor is not a member of the group");
        }

        List<GroupMember> members = membersForGroup.stream()
                .filter(m -> m.getUserId().equals(userId)).collect(Collectors.toList());
        if (members.isEmpty()) {
            log.warn("Attempted to remove non-member user {} from group {}", userId, groupId);
            throw new NotFoundException("Group member not found");
        }

        groupMemberRepository.deleteAll(members);
        log.info("Removed user {} from group {}", userId, groupId);
    }

    @Override
    @Transactional
    @LogExecution(includeArgs = true, includeResult = false, warnThresholdMs = 300)
    public void leaveGroup(UUID groupId, UUID userId) {
        log.info("User {} attempting to leave group {}", userId, groupId);
        // user can leave only if their balance for the group is zero
        List<Balance> balances = balanceRepository.findByGroupId(groupId);
        Optional<Balance> b = balances.stream().filter(x -> x.getUserId().equals(userId)).findFirst();
        if (b.isPresent() && b.get().getBalance().compareTo(java.math.BigDecimal.ZERO) != 0) {
            log.warn("User {} cannot leave group {} with non-zero balance {}", userId, groupId, b.get().getBalance());
            throw new IllegalStateException("Cannot leave group with non-zero balance");
        }

        // remove membership
        List<GroupMember> members = groupMemberRepository.findByGroupId(groupId)
                .stream().filter(m -> m.getUserId().equals(userId)).collect(Collectors.toList());
        if (members.isEmpty()) {
            log.warn("User {} is not a member of group {}; cannot leave", userId, groupId);
            throw new NotFoundException("Group member not found");
        }
        groupMemberRepository.deleteAll(members);
        log.info("User {} left group {}", userId, groupId);
    }

    private void ensureMember(UUID groupId, UUID userId) {
        boolean isMember = groupMemberRepository.findByGroupId(groupId).stream()
                .anyMatch(m -> m.getUserId().equals(userId) && "ACTIVE".equalsIgnoreCase(m.getStatus()));
        if (!isMember) {
            log.warn("User {} is not an ACTIVE member of group {}", userId, groupId);
            throw new IllegalArgumentException("User is not a member of the group");
        }
    }

    @Override
    @LogExecution(includeArgs = true, includeResult = false, warnThresholdMs = 300)
    @Transactional(readOnly = true)
    public List<ExpenseDto> listExpenses(UUID groupId) {
        UUID me = authService.getCurrentUserId();
        ensureMember(groupId, me);

        List<Expense> expenses = expenseRepository.findByGroupId(groupId);
        return expenses.stream().map(e -> ExpenseDto.builder()
                .id(e.getId())
                .groupId(e.getGroupId())
                .createdBy(e.getCreatedBy())
                .description(e.getDescription())
                .amount(e.getAmount())
                .currency(e.getCurrency())
                .createdAt(e.getCreatedAt())
                .build()).collect(Collectors.toList());
    }

    @Override
    @LogExecution(includeArgs = true, includeResult = false, warnThresholdMs = 300)
    @Transactional(readOnly = true)
    public List<BalanceDto> listBalances(UUID groupId) {
        UUID me = authService.getCurrentUserId();
        ensureMember(groupId, me);

        List<Balance> balances = balanceRepository.findByGroupId(groupId);
        return balances.stream().map(b -> BalanceDto.builder()
                .userId(b.getUserId())
                .balance(b.getBalance())
                .build()).collect(Collectors.toList());
    }

    @Override
    @Transactional
    @LogExecution(includeArgs = true, includeResult = false, warnThresholdMs = 300)
    public void removeGroup(UUID groupId) {
        Group g = groupRepository.findById(groupId).orElseThrow(() -> new NotFoundException("Group not found"));
        log.info("Removing group {} createdBy={}", groupId, g.getCreatedBy());
        // Deleting the group will cascade delete group-scoped data if DB constraints are set (Flyway uses ON DELETE CASCADE).
        groupRepository.deleteById(groupId);
    }
}