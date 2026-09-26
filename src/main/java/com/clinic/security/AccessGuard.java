package com.clinic.security;

import com.clinic.enums.Access;
import com.clinic.enums.Action;
import com.clinic.enums.Module;
import com.clinic.enums.Role;
import com.clinic.service.AuthService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

/** Checks the signed-in user's role against the module permission matrix. */
@Component
@RequiredArgsConstructor
public class AccessGuard {

    private final AuthService authService;

    public void require(Module module, Access level) {
        Role role = currentRole().orElseThrow(() -> new AccessDeniedException("Not signed in"));
        if (!authService.access(role, module).covers(level)) {
            throw new AccessDeniedException("No " + level + " access to " + module);
        }
    }

    public boolean hasAccess(Module module, Access level) {
        return currentRole().map(role -> authService.access(role, module).covers(level)).orElse(false);
    }

    public boolean can(Action action) {
        return currentRole().map(role -> authService.allowed(role, action)).orElse(false);
    }

    public void requireAction(Action action) {
        if (!can(action)) {
            throw new AccessDeniedException("Not allowed: " + action);
        }
    }

    /** Allowed when the role has at least one of the actions. */
    public void requireAny(Action... actions) {
        if (Arrays.stream(actions).noneMatch(this::can)) {
            throw new AccessDeniedException("Not allowed");
        }
    }

    public boolean isAdmin() {
        return currentRole().orElse(null) == Role.ADMIN;
    }

    public void requireAdmin() {
        if (!isAdmin()) {
            throw new AccessDeniedException("Administrator only");
        }
    }

    public Optional<Role> currentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return Optional.empty();
        }
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .flatMap(authority -> Arrays.stream(Role.values()).filter(role -> role.authority().equals(authority)))
                .findFirst();
    }
}
