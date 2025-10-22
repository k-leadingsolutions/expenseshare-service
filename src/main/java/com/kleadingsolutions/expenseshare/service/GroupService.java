package com.kleadingsolutions.expenseshare.service;

import com.kleadingsolutions.expenseshare.dto.BalanceDto;
import com.kleadingsolutions.expenseshare.dto.ExpenseDto;
import com.kleadingsolutions.expenseshare.dto.GroupDto;

import java.util.List;
import java.util.UUID;

public interface GroupService {

    List<GroupDto> listGroupsForUser(UUID userId);

    GroupDto createGroup(String name, UUID creatorId);

    void addMember(UUID groupId, UUID userId, UUID actorId);

    void removeMember(UUID groupId, UUID userId, UUID actorId);

    void leaveGroup(UUID groupId, UUID userId);

    List<ExpenseDto> listExpenses(UUID groupId);

    List<BalanceDto> listBalances(UUID groupId);

    /**
     * Remove the group and all group-scoped data (expenses, ledger entries, balances).
     * This operation requires administrative privileges (controller should enforce).
     *
     * @param groupId id of the group to delete
     */
    void removeGroup(UUID groupId);
}