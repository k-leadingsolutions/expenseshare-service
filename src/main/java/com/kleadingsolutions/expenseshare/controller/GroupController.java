package com.kleadingsolutions.expenseshare.controller;

import com.kleadingsolutions.expenseshare.dto.*;
import com.kleadingsolutions.expenseshare.service.AuthService;
import com.kleadingsolutions.expenseshare.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@Slf4j
public class GroupController {

    private final GroupService groupService;
    private final AuthService authService;

    @GetMapping
    public ResponseEntity<List<GroupDto>> myGroups() {
        UUID me = authService.getCurrentUserId();
        return ResponseEntity.ok(groupService.listGroupsForUser(me));
    }

    @PostMapping
    public ResponseEntity<GroupDto> createGroup(@Valid @RequestBody CreateGroupRequest req) {
        UUID me = authService.getCurrentUserId();
        return ResponseEntity.ok(groupService.createGroup(req.getName(), me));
    }

    @GetMapping("/{groupId}/expenses")
    public ResponseEntity<List<ExpenseDto>> listExpenses(@PathVariable("groupId") UUID groupId) {
        // membership enforcement inside service (or add explicit check)
        return ResponseEntity.ok(groupService.listExpenses(groupId));
    }

    @GetMapping("/{groupId}/balances")
    public ResponseEntity<List<BalanceDto>> listBalances(@PathVariable("groupId") UUID groupId) {
        return ResponseEntity.ok(groupService.listBalances(groupId));
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<Void> addMember(@PathVariable("groupId") UUID groupId,@Valid @RequestBody AddMemberRequest req) {
        UUID actor = authService.getCurrentUserId();
        groupService.addMember(groupId, req.getUserId(), actor);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @groupPermissionChecker.canManageGroup(principal, #a0)")
    public ResponseEntity<Void> removeMember(@PathVariable("groupId") UUID groupId, @PathVariable("userId") UUID userId) {
        log.debug("Controller.removeMember bound: groupId={}, userId={}", groupId, userId);
        UUID actor = null;
        try {
            actor = authService.getCurrentUserId();
        } catch (Exception e) {
            log.error("authService.getCurrentUserId() threw", e);
        }
        log.debug("Controller.removeMember resolved actor={}", actor);
        groupService.removeMember(groupId, userId, actor);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{groupId}/leave")
    public ResponseEntity<Void> leaveGroup(@PathVariable("groupId") UUID groupId) {
        UUID me = authService.getCurrentUserId();
        groupService.leaveGroup(groupId, me);
        return ResponseEntity.ok().build();
    }
}