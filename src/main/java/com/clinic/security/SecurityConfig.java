package com.clinic.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices.RememberMeTokenAlgorithm;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import java.time.Duration;

/**
 * Session based security (no JWT): the server keeps the login in the HTTP session,
 * the browser holds only the session cookie plus a CSRF cookie that the SPA echoes back.
 * A signed "remember me" cookie keeps the user signed in across session timeouts and server restarts until
 * they sign out (it stops working when the password changes or the account is deactivated).
 * Module level READ/WRITE checks are done by {@link AccessInterceptor}.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JsonSecurityHandlers handlers,
                                                   SecurityContextRepository contextRepository,
                                                   TokenBasedRememberMeServices rememberMeServices) throws Exception {
        http.csrf(this::csrf)
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .securityContext(context -> context.securityContextRepository(contextRepository))
                .authorizeHttpRequests(this::rules)
                .rememberMe(remember -> remember.rememberMeServices(rememberMeServices).key(rememberMeServices.getKey()))
                .exceptionHandling(errors -> errors.authenticationEntryPoint(handlers).accessDeniedHandler(handlers))
                .logout(logout -> logout.logoutUrl("/api/auth/logout").logoutSuccessHandler(handlers))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);
        return http.build();
    }

    /** CSRF token in a JavaScript-readable cookie; the SPA sends it back in the X-XSRF-TOKEN header. */
    private void csrf(CsrfConfigurer<HttpSecurity> csrf) {
        csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler());
    }

    private void rules(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/settings", "/api/settings/logo").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    /** Every login is remembered; the cookie is cleared on sign out. */
    @Bean
    public TokenBasedRememberMeServices rememberMeServices(UserDetailsService userDetailsService,
                                                           @Value("${app.security.remember-me-key}") String key,
                                                           @Value("${app.security.remember-me-days}") int days) {
        TokenBasedRememberMeServices services = new TokenBasedRememberMeServices(key, userDetailsService, RememberMeTokenAlgorithm.SHA256);
        services.setAlwaysRemember(true);
        services.setTokenValiditySeconds((int) Duration.ofDays(days).toSeconds());
        services.setCookieName("clinic-remember-me");
        return services;
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
}
