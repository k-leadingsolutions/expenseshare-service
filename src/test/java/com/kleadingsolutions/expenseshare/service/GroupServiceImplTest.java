package com.kleadingsolutions.expenseshare.service;

import com.kleadingsolutions.expenseshare.dto.BalanceDto;
import com.kleadingsolutions.expenseshare.dto.ExpenseDto;
import com.kleadingsolutions.expenseshare.dto.GroupDto;
import com.kleadingsolutions.expenseshare.exception.NotFoundException;
import com.kleadingsolutions.expenseshare.model.*;
import com.kleadingsolutions.expenseshare.repository.*;
import com.kleadingsolutions.expenseshare.service.impl.GroupServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private BalanceRepository balanceRepository;
    @Mock
    private AuthService authService;

    @InjectMocks
    private GroupServiceImpl groupService;

    private UUID groupId;
    private UUID creatorId;
    private UUID userId;
    private UUID otherUser;

    @BeforeEach
    void setUp() {
        groupId = UUID.randomUUID();
        creatorId = UUID.randomUUID();
        userId = UUID.randomUUID();
        otherUser = UUID.randomUUID();
    }

    @Test
    void listGroupsForUser_returnsMappedGroups() {
        // memberships
        GroupMember gm = GroupMember.builder().groupId(groupId).userId(userId).status("ACTIVE").build();
        when(groupMemberRepository.findByUserId(userId)).thenReturn(List.of(gm));

        Group g = Group.builder().id(groupId).name("G1").createdBy(creatorId).createdAt(LocalDateTime.now()).build();
        when(groupRepository.findAllById(List.of(groupId))).thenReturn(List.of(g));

        List<GroupDto> dtos = groupService.listGroupsForUser(userId);
        assertNotNull(dtos);
        assertEquals(1, dtos.size());
        assertEquals(groupId, dtos.get(0).getId());
        assertEquals("G1", dtos.get(0).getName());
    }

    @Test
    void createGroup_savesGroupAndAddsCreatorAsMember() {
        Group saved = Group.builder().id(groupId).name("New").createdBy(creatorId).createdAt(LocalDateTime.now()).build();
        when(groupRepository.save(ArgumentMatchers.any(Group.class))).thenReturn(saved);

        GroupDto dto = groupService.createGroup("New", creatorId);
        assertNotNull(dto);
        assertEquals(groupId, dto.getId());

        // verify member saved
        verify(groupMemberRepository, times(1)).save(ArgumentMatchers.any(GroupMember.class));
    }

    @Test
    void addMember_happyPath_addsMemberWhenActorIsMember() {
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(Group.builder().id(groupId).build()));

        GroupMember actorMember = GroupMember.builder().groupId(groupId).userId(creatorId).status("ACTIVE").build();
        when(groupMemberRepository.findByGroupId(groupId)).thenReturn(List.of(actorMember));

        when(userRepository.findById(userId)).thenReturn(Optional.of(User.builder().id(userId).email("a@b").build()));

        // existing members do not include userId
        when(groupMemberRepository.findByGroupId(groupId)).thenReturn(List.of(actorMember));

        groupService.addMember(groupId, userId, creatorId);

        verify(groupMemberRepository, times(1)).save(ArgumentMatchers.any(GroupMember.class));
    }

    @Test
    void addMember_throwsWhenActorNotMember() {
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(Group.builder().id(groupId).build()));
        when(groupMemberRepository.findByGroupId(groupId)).thenReturn(List.of()); // actor not present

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> groupService.addMember(groupId, userId, creatorId));
        assertTrue(ex.getMessage().toLowerCase().contains("actor"));
    }

    @Test
    void addMember_noSuchUser_throwsNotFound() {
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(Group.builder().id(groupId).build()));
        GroupMember actorMember = GroupMember.builder().groupId(groupId).userId(creatorId).status("ACTIVE").build();
        when(groupMemberRepository.findByGroupId(groupId)).thenReturn(List.of(actorMember));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> groupService.addMember(groupId, userId, creatorId));
    }

    @Test
    void removeMember_happyPath_removesMember() {
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(Group.builder().id(groupId).build()));

        GroupMember actorMember = GroupMember.builder().groupId(groupId).userId(creatorId).status("ACTIVE").build();
        GroupMember target = GroupMember.builder().groupId(groupId).userId(userId).status("ACTIVE").build();
        when(groupMemberRepository.findByGroupId(groupId)).thenReturn(List.of(actorMember, target));

        groupService.removeMember(groupId, userId, creatorId);

        // use StreamSupport to handle Iterable in deleteAll arg matcher
        verify(groupMemberRepository, times(1)).deleteAll(argThat(iterable ->
                StreamSupport.stream(iterable.spliterator(), false)
                        .anyMatch(m -> m.getUserId().equals(userId))
        ));
    }

    @Test
    void removeMember_whenMemberNotFound_throws() {
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(Group.builder().id(groupId).build()));
        GroupMember actorMember = GroupMember.builder().groupId(groupId).userId(creatorId).status("ACTIVE").build();
        when(groupMemberRepository.findByGroupId(groupId)).thenReturn(List.of(actorMember));

        assertThrows(NotFoundException.class, () -> groupService.removeMember(groupId, otherUser, creatorId));
    }

    @Test
    void leaveGroup_failsWhenNonZeroBalance() {
        Balance nonZero = Balance.builder().groupId(groupId).userId(userId).balance(new BigDecimal("5.00")).build();
        when(balanceRepository.findByGroupId(groupId)).thenReturn(List.of(nonZero));

        List<GroupMember> members = List.of(GroupMember.builder().groupId(groupId).userId(userId).status("ACTIVE").build());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> groupService.leaveGroup(groupId, userId));
        assertTrue(ex.getMessage().toLowerCase().contains("non-zero"));
    }

    @Test
    void leaveGroup_removesMemberWhenZeroBalance() {
        Balance zero = Balance.builder().groupId(groupId).userId(userId).balance(BigDecimal.ZERO).build();
        when(balanceRepository.findByGroupId(groupId)).thenReturn(List.of(zero));

        GroupMember gm = GroupMember.builder().groupId(groupId).userId(userId).status("ACTIVE").build();
        when(groupMemberRepository.findByGroupId(groupId)).thenReturn(List.of(gm));

        groupService.leaveGroup(groupId, userId);

        verify(groupMemberRepository, times(1)).deleteAll(argThat(iterable ->
                StreamSupport.stream(iterable.spliterator(), false)
                        .anyMatch(m -> m.getUserId().equals(userId))
        ));
    }

    @Test
    void listExpenses_requiresMembership_andReturnsDtosWhenMember() {
        // setup auth service returning a user who is a member
        when(authService.getCurrentUserId()).thenReturn(userId);

        GroupMember gm = GroupMember.builder().groupId(groupId).userId(userId).status("ACTIVE").build();
        when(groupMemberRepository.findByGroupId(groupId)).thenReturn(List.of(gm));

        Expense e = Expense.builder().id(UUID.randomUUID()).groupId(groupId).createdBy(creatorId)
                .description("d").amount(new BigDecimal("10.00")).currency("AED").createdAt(LocalDateTime.now()).build();
        when(expenseRepository.findByGroupId(groupId)).thenReturn(List.of(e));

        List<ExpenseDto> out = groupService.listExpenses(groupId);
        assertEquals(1, out.size());
        assertEquals(e.getId(), out.get(0).getId());
    }

    @Test
    void listExpenses_throwsWhenNotMember() {
        when(authService.getCurrentUserId()).thenReturn(userId);
        when(groupMemberRepository.findByGroupId(groupId)).thenReturn(List.of()); // not member

        assertThrows(IllegalArgumentException.class, () -> groupService.listExpenses(groupId));
    }

    @Test
    void listBalances_requiresMembership_andReturnsDtosWhenMember() {
        when(authService.getCurrentUserId()).thenReturn(userId);
        GroupMember gm = GroupMember.builder().groupId(groupId).userId(userId).status("ACTIVE").build();
        when(groupMemberRepository.findByGroupId(groupId)).thenReturn(List.of(gm));

        Balance b = Balance.builder().groupId(groupId).userId(userId).balance(new BigDecimal("2.50")).build();
        when(balanceRepository.findByGroupId(groupId)).thenReturn(List.of(b));

        List<BalanceDto> out = groupService.listBalances(groupId);
        assertEquals(1, out.size());
        assertEquals(b.getUserId(), out.get(0).getUserId());
        assertEquals(b.getBalance(), out.get(0).getBalance());
    }

    @Test
    void removeGroup_deletesGroupWhenExists() {
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(Group.builder().id(groupId).build()));
        groupService.removeGroup(groupId);
        verify(groupRepository, times(1)).deleteById(groupId);
    }

    @Test
    void removeGroup_throwsWhenNotFound() {
        when(groupRepository.findById(groupId)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> groupService.removeGroup(groupId));
    }
}