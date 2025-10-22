package com.kleadingsolutions.expenseshare.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.security.Principal;
import java.util.UUID;

/**
 * Robust helper for extracting the current application's internal user UUID from the security context.
 *
 * This handles multiple principal types produced by Spring Security:
 * - Jwt (resource server)
 * - JwtAuthenticationToken
 * - OidcUser (oauth2Login)
 * - OAuth2User (oauth2Login)
 * - Principal / String fallback
 *
 * Behavior:
 * - Prefer explicit "user_id" or "userId" JWT/attribute claim if present (assumed to be app UUID).
 * - Fall back to subject ("sub") or "email" if present.
 * - If the resolved identifier is not a UUID, an IllegalStateException is thrown with guidance
 *   to perform an explicit external-id -> internal-UUID mapping (via a user service).
 *
 * Note:
 * - If you use external provider ids (Google `sub` or `email`) you must map them to your internal UUID
 *   in the application (e.g. userService.findByExternalId(...)). Do NOT assume external ids are UUIDs.
 */
public final class SecurityUtils {

    private SecurityUtils() { /* utility */ }

    public static UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found in security context");
        }

        Object principal = auth.getPrincipal();
        String idCandidate = null;

        // Jwt principal (resource-server)
        if (principal instanceof Jwt) {
            Jwt jwt = (Jwt) principal;
            idCandidate = firstNonEmpty(
                    jwt.getClaimAsString("user_id"),
                    jwt.getClaimAsString("userId"),
                    jwt.getSubject(),
                    jwt.getClaimAsString("sub"),
                    jwt.getClaimAsString("email")
            );
        }
        // Jwt wrapped in JwtAuthenticationToken
        else if (principal instanceof JwtAuthenticationToken) {
            Jwt jwt = ((JwtAuthenticationToken) principal).getToken();
            idCandidate = firstNonEmpty(
                    jwt.getClaimAsString("user_id"),
                    jwt.getClaimAsString("userId"),
                    jwt.getSubject(),
                    jwt.getClaimAsString("sub"),
                    jwt.getClaimAsString("email")
            );
        }
        // OIDC user (oauth2Login)
        else if (principal instanceof OidcUser) {
            OidcUser user = (OidcUser) principal;
            idCandidate = firstNonEmpty(
                    toStringSafe(user.getAttribute("user_id")),
                    toStringSafe(user.getAttribute("userId")),
                    user.getSubject(),
                    user.getEmail(),
                    user.getName()
            );
        }
        // Generic OAuth2User (oauth2Login)
        else if (principal instanceof OAuth2User) {
            OAuth2User user = (OAuth2User) principal;
            idCandidate = firstNonEmpty(
                    toStringSafe(user.getAttribute("user_id")),
                    toStringSafe(user.getAttribute("userId")),
                    toStringSafe(user.getAttribute("sub")),
                    toStringSafe(user.getAttribute("email")),
                    user.getName()
            );
        }
        // Principal or String fallback
        else if (principal instanceof Principal) {
            idCandidate = ((Principal) principal).getName();
        } else if (principal instanceof String) {
            idCandidate = (String) principal;
        }

        if (idCandidate == null || idCandidate.isBlank()) {
            throw new IllegalStateException(
                    "Authenticated principal does not expose a user identifier (user_id/sub/email/name). " +
                            "If you use external provider ids (e.g. Google 'sub' or 'email'), implement a mapping from that " +
                            "external id to your application's internal UUID (e.g. userService.findByExternalId(...))."
            );
        }

        try {
            return UUID.fromString(idCandidate);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "Resolved principal identifier is not a UUID: '" + idCandidate + "'. " +
                            "Implement an explicit mapping from the external identifier to your internal UUID (e.g. via a UserService) " +
                            "or include an internal 'user_id' UUID claim in tokens used by your APIs.",
                    ex
            );
        }
    }

    private static String firstNonEmpty(String... candidates) {
        if (candidates == null) return null;
        for (String c : candidates) {
            if (c != null && !c.isBlank()) return c;
        }
        return null;
    }

    private static String toStringSafe(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}