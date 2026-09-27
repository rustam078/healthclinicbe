package com.clinic.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * One short line per API call: "POST /api/appointments -> 201 (23 ms) user=admin".
 * Runs before security, so rejected calls (401/403) are logged too. Never logs bodies or query text
 * (no passwords or patient details). Quieten with logging.level.API=WARN (then only server errors).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("API");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long started = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            write(request, response.getStatus(), System.currentTimeMillis() - started);
        }
    }

    private static void write(HttpServletRequest request, int status, long millis) {
        String line = "{} {} -> {} ({} ms) user={}";
        Object[] values = {request.getMethod(), request.getRequestURI(), status, millis, user(request)};
        if (status >= 500) {
            log.warn(line, values);
        } else {
            log.info(line, values);
        }
    }

    /** The signed-in user from the session (the security context is already cleared at this point). */
    private static String user(HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            Object context = session == null ? null
                    : session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            return context instanceof SecurityContext security && security.getAuthentication() != null
                    ? security.getAuthentication().getName() : "-";
        } catch (IllegalStateException sessionEnded) {
            return "-";
        }
    }
}
