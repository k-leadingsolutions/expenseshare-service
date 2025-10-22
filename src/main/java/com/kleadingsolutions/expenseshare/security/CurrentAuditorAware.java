package com.kleadingsolutions.expenseshare.security;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Optional;
import java.util.UUID;

/**
 * Provides current auditor UUID (domain user id) for @CreatedBy / @LastModifiedBy support.
 */
public class CurrentAuditorAware implements AuditorAware<UUID> {

    @Override
    public Optional<UUID> getCurrentAuditor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return Optional.empty();

        Object principal = auth.getPrincipal();
        if (principal instanceof OAuth2User) {
            OAuth2User u = (OAuth2User) principal;
            Object uid = u.getAttribute("uid");
            if (uid != null) {
                try {
                    return Optional.of(UUID.fromString(uid.toString()));
                } catch (IllegalArgumentException ex) {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }
}