package com.kleadingsolutions.expenseshare.security;

import com.kleadingsolutions.expenseshare.model.Group;
import com.kleadingsolutions.expenseshare.repository.GroupMemberRepository;
import com.kleadingsolutions.expenseshare.repository.GroupRepository;
import com.kleadingsolutions.expenseshare.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Bean used in @PreAuthorize SpEL expressions to determine whether the current principal
 * can manage the given group. "Manage" here means either:
 *  - the current user is the group's creator (createdBy), OR
 *  - the current user is an ACTIVE member of the group
 *
 * Notes:
 * - Method is read-only transactional to avoid lazy-loading issues when called outside controller transactions.
 * - Uses exists/boolean repository checks where possible to avoid loading entire collections.
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
    @Transactional(readOnly = true)
    public boolean canManageGroup(Object principal, UUID groupId) {
        if (groupId == null) {
            log.debug("canManageGroup called with null groupId; denying");
            return false;
        }

        UUID currentUserId;
        try {
            currentUserId = authService.getCurrentUserId();
        } catch (RuntimeException ex) {
            log.debug("Failed to obtain current user from AuthService: {}", ex.getMessage());
            return false;
        }

        if (currentUserId == null) {
            log.debug("canManageGroup: no authenticated user; denying for group={}", groupId);
            return false;
        }

        log.debug("Checking manage permission for user={} on group={}", currentUserId, groupId);

        // Fast-path: check if the user is the group's creator without loading the full Group entity,
        // try a repository-level check if available.
        try {
            // If GroupRepository defines existsByIdAndCreatedBy(groupId, currentUserId) use it for best performance.
            boolean isCreator;
            try {
                isCreator = groupRepository.existsByIdAndCreatedBy(groupId, currentUserId);
            } catch (NoSuchMethodError | AbstractMethodError e) {
                // Repository method not present; fallback to findById and compare createdBy.
                Optional<Group> gOpt = groupRepository.findById(groupId);
                isCreator = gOpt.map(g -> currentUserId.equals(g.getCreatedBy())).orElse(false);
            }

            if (isCreator) {
                log.debug("User {} is creator of group {}; granting manage permission", currentUserId, groupId);
                return true;
            }
        } catch (Exception ex) {
            // Be conservative: if repository lookup fails, deny and log
            log.warn("Error while checking group creator for group={} user={}: {}", groupId, currentUserId, ex.getMessage());
            return false;
        }

        // Otherwise check membership with ACTIVE status; prefer a repository exists query to avoid loading the whole list.
        try {
            boolean isActiveMember;
            try {
                isActiveMember = groupMemberRepository.existsByGroupIdAndUserIdAndStatus(groupId, currentUserId, "ACTIVE");
            } catch (NoSuchMethodError | AbstractMethodError e) {
                // Fallback: repository doesn't provide existsBy..., load members and check
                isActiveMember = groupMemberRepository.findByGroupId(groupId).stream()
                        .anyMatch(m -> currentUserId.equals(m.getUserId()) && "ACTIVE".equalsIgnoreCase(m.getStatus()));
            }

            if (isActiveMember) {
                log.debug("User {} is ACTIVE member of group {}; granting manage permission", currentUserId, groupId);
                return true;
            } else {
                log.debug("User {} is not an ACTIVE member of group {}; denying", currentUserId, groupId);
                return false;
            }
        } catch (Exception ex) {
            log.warn("Error while checking membership for group={} user={}: {}", groupId, currentUserId, ex.getMessage());
            return false;
        }
    }
}