package com.kleadingsolutions.expenseshare.security;

import com.kleadingsolutions.expenseshare.repository.GroupMemberRepository;
import com.kleadingsolutions.expenseshare.repository.GroupRepository;
import com.kleadingsolutions.expenseshare.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupPermissionCheckerTest {

    @Mock
    GroupMemberRepository groupMemberRepository;

    @Mock
    GroupRepository groupRepository;

    @Mock
    AuthService authService;

    @InjectMocks
    GroupPermissionChecker checker;

    private UUID groupId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        groupId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    void canManageGroup_returnsFalse_whenGroupIdNull() {
        boolean result = checker.canManageGroup(null, null);
        assertFalse(result);
    }

    @Test
    void canManageGroup_returnsFalse_whenAuthServiceFails() {
        when(authService.getCurrentUserId()).thenThrow(new RuntimeException("no auth"));
        boolean result = checker.canManageGroup(null, groupId);
        assertFalse(result);
        verify(authService).getCurrentUserId();
    }

    @Test
    void canManageGroup_grantsWhenCreator() {
        when(authService.getCurrentUserId()).thenReturn(userId);
        when(groupRepository.existsByIdAndCreatedBy(groupId, userId)).thenReturn(true);

        boolean result = checker.canManageGroup(null, groupId);

        assertTrue(result);
        verify(groupRepository).existsByIdAndCreatedBy(groupId, userId);
        verifyNoInteractions(groupMemberRepository);
    }

    @Test
    void canManageGroup_grantsWhenActiveMember() {
        when(authService.getCurrentUserId()).thenReturn(userId);
        when(groupRepository.existsByIdAndCreatedBy(groupId, userId)).thenReturn(false);
        when(groupMemberRepository.existsByGroupIdAndUserIdAndStatus(groupId, userId, "ACTIVE")).thenReturn(true);

        boolean result = checker.canManageGroup(null, groupId);

        assertTrue(result);
        verify(groupRepository).existsByIdAndCreatedBy(groupId, userId);
        verify(groupMemberRepository).existsByGroupIdAndUserIdAndStatus(groupId, userId, "ACTIVE");
    }

    @Test
    void canManageGroup_deniesWhenNotCreatorOrMember() {
        when(authService.getCurrentUserId()).thenReturn(userId);
        when(groupRepository.existsByIdAndCreatedBy(groupId, userId)).thenReturn(false);
        when(groupMemberRepository.existsByGroupIdAndUserIdAndStatus(groupId, userId, "ACTIVE")).thenReturn(false);

        boolean result = checker.canManageGroup(null, groupId);

        assertFalse(result);
        verify(groupRepository).existsByIdAndCreatedBy(groupId, userId);
        verify(groupMemberRepository).existsByGroupIdAndUserIdAndStatus(groupId, userId, "ACTIVE");
    }
}