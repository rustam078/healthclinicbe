package com.clinic.util;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/** Reads the logged-in username from the security context (empty for anonymous/system work). */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Optional<String> username() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean loggedIn = auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
        return loggedIn ? Optional.of(auth.getName()) : Optional.empty();
    }

    public static String usernameOrSystem() {
        return username().orElse("system");
    }
}
