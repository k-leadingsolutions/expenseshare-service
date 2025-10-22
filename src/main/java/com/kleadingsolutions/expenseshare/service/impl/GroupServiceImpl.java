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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of GroupService. Keep methods small and testable.
 */
@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final BalanceRepository balanceRepository;
    private final AuthService authService;

    @Override
    @LogExecution(includeArgs = true, includeResult = false, warnThresholdMs = 300)
    public List<GroupDto> listGroupsForUser(UUID userId) {
        List<GroupMember> memberships = groupMemberRepository.findByUserId(userId);
        List<UUID> groupIds = memberships.stream().map(GroupMember::getGroupId).collect(Collectors.toList());
        if (groupIds.isEmpty()) return Collections.emptyList();
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
        Group g = Group.builder()
                .name(name)
                .createdBy(creatorId)
                .build();
        g = groupRepository.save(g);

        // add creator as member
        GroupMember gm = GroupMember.builder()
                .groupId(g.getId())
                .userId(creatorId)
                .joinedAt(LocalDateTime.now())
                .status("ACTIVE")
                .build();
        groupMemberRepository.save(gm);

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
        groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));
        // require actor be member:
        boolean actorMember = groupMemberRepository.findByGroupId(groupId)
                .stream().anyMatch(m -> m.getUserId().equals(actorId) && "ACTIVE".equalsIgnoreCase(m.getStatus()));
        if (!actorMember) throw new IllegalArgumentException("Actor is not a member of the group");

        // ensure user exists
        userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));

        // prevent duplicate
        boolean already = groupMemberRepository.findByGroupId(groupId).stream()
                .anyMatch(m -> m.getUserId().equals(userId));
        if (already) return;

        GroupMember gm = GroupMember.builder()
                .groupId(groupId)
                .userId(userId)
                .status("ACTIVE")
                .joinedAt(LocalDateTime.now())
                .build();
        groupMemberRepository.save(gm);
    }

    @Override
    @Transactional
    @LogExecution(includeArgs = true, includeResult = false, warnThresholdMs = 300)
    public void removeMember(UUID groupId, UUID userId, UUID actorId) {
        groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));

        // allow only actor who is group member to remove (or admin) - caller must enforce admin externally
        boolean actorMember = groupMemberRepository.findByGroupId(groupId)
                .stream().anyMatch(m -> m.getUserId().equals(actorId) && "ACTIVE".equalsIgnoreCase(m.getStatus()));
        if (!actorMember) throw new IllegalArgumentException("Actor is not a member of the group");

        List<GroupMember> members = groupMemberRepository.findByGroupId(groupId)
                .stream().filter(m -> m.getUserId().equals(userId)).collect(Collectors.toList());
        if (members.isEmpty()) throw new NotFoundException("Group member not found");

        groupMemberRepository.deleteAll(members);
    }

    @Override
    @Transactional
    @LogExecution(includeArgs = true, includeResult = false, warnThresholdMs = 300)
    public void leaveGroup(UUID groupId, UUID userId) {
        // user can leave only if their balance for the group is zero
        List<Balance> balances = balanceRepository.findByGroupId(groupId);
        Optional<Balance> b = balances.stream().filter(x -> x.getUserId().equals(userId)).findFirst();
        if (b.isPresent() && b.get().getBalance().compareTo(java.math.BigDecimal.ZERO) != 0) {
            throw new IllegalStateException("Cannot leave group with non-zero balance");
        }

        // remove membership
        List<GroupMember> members = groupMemberRepository.findByGroupId(groupId)
                .stream().filter(m -> m.getUserId().equals(userId)).collect(Collectors.toList());
        if (members.isEmpty()) throw new NotFoundException("Group member not found");
        groupMemberRepository.deleteAll(members);
    }

    private void ensureMember(UUID groupId, UUID userId) {
        boolean isMember = groupMemberRepository.findByGroupId(groupId).stream()
                .anyMatch(m -> m.getUserId().equals(userId) && "ACTIVE".equalsIgnoreCase(m.getStatus()));
        if (!isMember) {
            throw new IllegalArgumentException("User is not a member of the group");
        }
    }

    @Override
    @LogExecution(includeArgs = true, includeResult = false, warnThresholdMs = 300)
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
        // Deleting the group will cascade delete group-scoped data if DB constraints are set (Flyway uses ON DELETE CASCADE).
        groupRepository.deleteById(groupId);
    }
}