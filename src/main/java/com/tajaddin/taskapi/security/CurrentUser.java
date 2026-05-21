package com.tajaddin.taskapi.security;

import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Helper to read the authenticated user id from the security context. The
 * JwtAuthenticationFilter stores the user id (Long) as the principal.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long userId)) {
            throw new IllegalStateException("no authenticated user in context");
        }
        return userId;
    }
}
