package com.kleadingsolutions.expenseshare.controller;

import com.kleadingsolutions.expenseshare.dto.*;
import com.kleadingsolutions.expenseshare.service.AuthService;
import com.kleadingsolutions.expenseshare.service.GroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupControllerTest {

    @Mock
    private GroupService groupService;

    @Mock
    private AuthService authService;

    @InjectMocks
    private GroupController groupController;

    private UUID me;
    private UUID groupId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        me = UUID.randomUUID();
        groupId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    void myGroups_shouldReturnList() {
        // Arrange
        GroupDto g1 = GroupDto.builder().id(UUID.randomUUID()).name("g1").createdBy(me).build();
        List<GroupDto> groups = List.of(g1);

        when(authService.getCurrentUserId()).thenReturn(me);
        when(groupService.listGroupsForUser(me)).thenReturn(groups);

        // Act
        ResponseEntity<List<GroupDto>> resp = groupController.myGroups();

        // Assert
        assertNotNull(resp);
        assertEquals(200, resp.getStatusCodeValue());
        assertEquals(groups, resp.getBody());
    }

    @Test
    void createGroup_shouldCallServiceAndReturnDto() {
        // Arrange
        CreateGroupRequest req = mock(CreateGroupRequest.class);
        when(req.getName()).thenReturn("dev-group");

        GroupDto returned = GroupDto.builder().id(groupId).name("dev-group").createdBy(me).build();

        when(authService.getCurrentUserId()).thenReturn(me);
        when(groupService.createGroup("dev-group", me)).thenReturn(returned);

        // Act
        ResponseEntity<GroupDto> resp = groupController.createGroup(req);

        // Assert
        assertNotNull(resp);
        assertEquals(200, resp.getStatusCodeValue());
        assertSame(returned, resp.getBody());
    }

    @Test
    void listExpenses_shouldReturnList() {
        // Arrange
        ExpenseDto e = ExpenseDto.builder().id(UUID.randomUUID()).groupId(groupId).description("d").amount(BigDecimal.valueOf(10.0)).currency("USD").build();
        when(groupService.listExpenses(groupId)).thenReturn(List.of(e));

        // Act
        ResponseEntity<List<ExpenseDto>> resp = groupController.listExpenses(groupId);

        // Assert
        assertNotNull(resp);
        assertEquals(200, resp.getStatusCodeValue());
        assertEquals(1, resp.getBody().size());
        assertEquals(e, resp.getBody().get(0));
    }

    @Test
    void listBalances_shouldReturnList() {
        BalanceDto b = BalanceDto.builder().userId(userId).balance(BigDecimal.valueOf(0.0)).build();
        when(groupService.listBalances(groupId)).thenReturn(List.of(b));

        ResponseEntity<List<BalanceDto>> resp = groupController.listBalances(groupId);

        // Assert
        assertNotNull(resp);
        assertEquals(200, resp.getStatusCodeValue());
        assertEquals(1, resp.getBody().size());
        assertEquals(b, resp.getBody().get(0));
    }

    @Test
    void addMember_shouldInvokeService_andReturnOk() {
        AddMemberRequest req = mock(AddMemberRequest.class);
        when(req.getUserId()).thenReturn(userId);
        when(authService.getCurrentUserId()).thenReturn(me);

        ResponseEntity<Void> resp = groupController.addMember(groupId, req);

        // Assert
        assertNotNull(resp);
        assertEquals(200, resp.getStatusCodeValue());
        verify(groupService).addMember(groupId, userId, me);
    }

    @Test
    void removeMember_shouldInvokeService_withActor_whenAuthSucceeds() {
        when(authService.getCurrentUserId()).thenReturn(me);

        ResponseEntity<Void> resp = groupController.removeMember(groupId, userId);

        // Assert
        assertNotNull(resp);
        assertEquals(200, resp.getStatusCodeValue());
        verify(groupService).removeMember(groupId, userId, me);
    }

    @Test
    void removeMember_shouldInvokeService_withNullActor_whenAuthThrows() {
        when(authService.getCurrentUserId()).thenThrow(new RuntimeException("no auth"));

        ResponseEntity<Void> resp = groupController.removeMember(groupId, userId);

        // Assert
        assertNotNull(resp);
        assertEquals(200, resp.getStatusCodeValue());
        // Expect actor passed as null when authService throws
        verify(groupService).removeMember(groupId, userId, null);
    }

    @Test
    void leaveGroup_shouldInvokeService_withCurrentUser() {
        when(authService.getCurrentUserId()).thenReturn(me);

        ResponseEntity<Void> resp = groupController.leaveGroup(groupId);

        // Assert
        assertNotNull(resp);
        assertEquals(200, resp.getStatusCodeValue());
        verify(groupService).leaveGroup(groupId, me);
    }
}