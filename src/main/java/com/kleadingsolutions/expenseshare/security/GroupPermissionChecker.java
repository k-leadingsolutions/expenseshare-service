package com.kleadingsolutions.expenseshare.security;

import com.kleadingsolutions.expenseshare.model.Group;
import com.kleadingsolutions.expenseshare.repository.GroupMemberRepository;
import com.kleadingsolutions.expenseshare.repository.GroupRepository;
import com.kleadingsolutions.expenseshare.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Bean used in @PreAuthorize SpEL expressions to determine whether the current principal
 * can manage the given group. "Manage" here means either:
 *  - the current user is the group's creator (createdBy), OR
 *  - the current user is an ACTIVE member of the group
 */
@Component("groupPermissionChecker")
@RequiredArgsConstructor
@Slf4j
public class GroupPermissionChecker {

    private final GroupMemberRepository groupMemberRepository;
    private final GroupRepository groupRepository;
    private final AuthService authService;

    /**
     * SpEL-friendly signature: principal is ignored; we discover the current user from AuthService.
     *
     * @param principal ignored (provided by SpEL)
     * @param groupId   group to test
     * @return true if current principal can manage the group
     */
    public boolean canManageGroup(Object principal, UUID groupId) {
        if (groupId == null) {
            log.debug("canManageGroup called with null groupId; denying");
            return false;
        }

        UUID currentUserId;
        try {
            currentUserId = authService.getCurrentUserId();
        } catch (RuntimeException ex) {
            return false;
        }

        log.debug("groupPermissionChecker.canManageGroup principal={}, groupId={}", principal, groupId);

        if (currentUserId == null) return false;

        log.debug("groupPermissionChecker.canManageGroup is not authorized currentUserId={}, groupId={}", currentUserId, groupId);

        // If the current user is the group creator -> can manage
        Optional<Group> g = groupRepository.findById(groupId);
        if (g.isPresent()) {
            Group group = g.get();
            if (group.getCreatedBy() != null && group.getCreatedBy().equals(currentUserId)) {
                return true;
            }
        }

        // Otherwise check membership with ACTIVE status
        return groupMemberRepository.findByGroupId(groupId).stream()
                .anyMatch(m -> m.getUserId() != null
                        && m.getUserId().equals(currentUserId)
                        && "ACTIVE".equalsIgnoreCase(m.getStatus()));
    }
}