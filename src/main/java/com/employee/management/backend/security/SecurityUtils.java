package com.employee.management.backend.security;

import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AuthenticatedUser currentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal instanceof AuthenticatedUser authenticatedUser ? authenticatedUser : null;
    }

    // Null for a super_admin caller (not scoped to any tenant) or if there's no authenticated
    // user at all.
    public static Long currentClientId() {
        AuthenticatedUser user = currentUser();
        return user != null ? user.clientId() : null;
    }
}
