package com.kleadingsolutions.expenseshare.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Resolves the application's internal UUID for the current authenticated principal.
 * Uses UserService to map external provider ids (provider + providerId) to internal UUIDs.
 */
@Service
public class AuthService {

  private final UserService userService;

  public AuthService(UserService userService) {
    this.userService = userService;
  }

  /**
   * Resolve current user's internal UUID. If the external user does not exist in local DB,
   * this will create a new local user (auto-provision) using available claims (sub/email/name).
   *
   * Throws IllegalStateException if authentication is missing.
   */
  public UUID getCurrentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      throw new IllegalStateException("No authenticated user in security context");
    }

    // Determine provider and providerId from principal
    String provider = "unknown";
    String providerId = null;
    String email = null;
    String name = null;

    Object principal = auth.getPrincipal();

    if (principal instanceof Jwt) {
      Jwt jwt = (Jwt) principal;
      // If your tokens include a claim identifying provider use it; otherwise default to "google"
      provider = jwt.getClaimAsString("iss") != null && jwt.getClaimAsString("iss").contains("accounts.google") ? "google" : "external";
      providerId = jwt.getSubject(); // usually 'sub'
      email = jwt.getClaimAsString("email");
      name = jwt.getClaimAsString("name");
    } else if (principal instanceof JwtAuthenticationToken) {
      Jwt jwt = ((JwtAuthenticationToken) principal).getToken();
      provider = jwt.getClaimAsString("iss") != null && jwt.getClaimAsString("iss").contains("accounts.google") ? "google" : "external";
      providerId = jwt.getSubject();
      email = jwt.getClaimAsString("email");
      name = jwt.getClaimAsString("name");
    } else if (principal instanceof OidcUser) {
      OidcUser user = (OidcUser) principal;
      provider = "google";
      providerId = user.getSubject();
      email = user.getEmail();
      name = user.getFullName();
    } else if (principal instanceof OAuth2User) {
      OAuth2User user = (OAuth2User) principal;
      provider = "google";
      Object sub = user.getAttribute("sub");
      providerId = sub != null ? String.valueOf(sub) : user.getName();
      email = (String) user.getAttribute("email");
      name = (String) user.getAttribute("name");
    } else {
      // fallback - use name
      providerId = auth.getName();
    }

    // find or create local user mapping
    var user = userService.findOrCreate(provider, providerId, email, name);
    return user.getId();
  }
}