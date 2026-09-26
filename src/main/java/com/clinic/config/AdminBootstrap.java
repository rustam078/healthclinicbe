package com.clinic.config;

import com.clinic.entity.AppUser;
import com.clinic.enums.Role;
import com.clinic.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the first administrator when the user table is empty (fresh installation). */
@Slf4j
@Component
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    public AdminBootstrap(UserRepository userRepository, PasswordEncoder passwordEncoder,
                          @Value("${app.bootstrap.admin-username}") String username,
                          @Value("${app.bootstrap.admin-password}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }
        if (password == null || password.isBlank()) {
            log.warn("No users exist. Set APP_ADMIN_PASSWORD to create the first administrator.");
            return;
        }
        userRepository.save(newAdmin());
        log.info("Created first administrator account '{}'", username);
    }

    private AppUser newAdmin() {
        AppUser admin = new AppUser();
        admin.setUsername(username);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setFullName("Administrator");
        admin.setRole(Role.ADMIN);
        return admin;
    }
}
