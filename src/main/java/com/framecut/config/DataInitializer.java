package com.framecut.config;

import com.framecut.entity.User;
import com.framecut.repository.ReviewRepository;
import com.framecut.repository.UserRepository;
import com.framecut.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final WatchlistRepository watchlistRepository;
    private final ReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_USERNAME:golddaizen}")
    private String adminUsername;

    @Value("${ADMIN_EMAIL:nosriraam7@gmail.com}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Running DataInitializer security audit & account setup...");

        // 1. Verify or initialize admin account
        Optional<User> byEmail = (adminEmail != null && !adminEmail.isBlank())
            ? userRepository.findByEmail(adminEmail)
            : Optional.empty();
        Optional<User> byUsername = (adminUsername != null && !adminUsername.isBlank())
            ? userRepository.findByUsername(adminUsername)
            : Optional.empty();

        User adminUser = null;
        if (byEmail.isPresent()) {
            adminUser = byEmail.get();
        } else if (byUsername.isPresent()) {
            adminUser = byUsername.get();
        }

        if (adminUser != null) {
            adminUser.setUsername(adminUsername);
            if (adminEmail != null && !adminEmail.isBlank()) {
                adminUser.setEmail(adminEmail);
            }
            adminUser.setRole(User.Role.ADMIN);
            // Update password only if an explicit password is provided in environment variables
            if (adminPassword != null && !adminPassword.isBlank()) {
                adminUser.setPassword(passwordEncoder.encode(adminPassword));
                log.info("Admin password updated from environment variable.");
            }
            userRepository.save(adminUser);
            log.info("Verified admin account '{}' with ADMIN role.", adminUsername);
        } else {
            // New deployment fallback: create initial admin account
            String initialPassword = (adminPassword != null && !adminPassword.isBlank())
                ? adminPassword
                : "FrameCutAdmin2026!";
            adminUser = User.builder()
                .username(adminUsername)
                .email(adminEmail != null && !adminEmail.isBlank() ? adminEmail : "admin@framecut.com")
                .password(passwordEncoder.encode(initialPassword))
                .role(User.Role.ADMIN)
                .build();
            userRepository.save(adminUser);
            log.info("Initialized admin account '{}' with ADMIN role.", adminUsername);
        }

        // 2. Clean up any other test users (ensure only the admin account exists)
        final Long adminId = adminUser.getId();
        List<User> allUsers = userRepository.findAll();
        for (User u : allUsers) {
            if (!u.getId().equals(adminId)) {
                log.info("Security cleanup: removing unverified user id={}", u.getId());
                try {
                    watchlistRepository.findAll().stream()
                        .filter(w -> w.getUser() != null && w.getUser().getId().equals(u.getId()))
                        .forEach(watchlistRepository::delete);
                    reviewRepository.findAll().stream()
                        .filter(r -> r.getUser() != null && r.getUser().getId().equals(u.getId()))
                        .forEach(reviewRepository::delete);
                    userRepository.delete(u);
                } catch (Exception ex) {
                    log.error("Failed to delete user {}: {}", u.getId(), ex.getMessage());
                }
            }
        }

        log.info("Security initialization complete. Active admin: '{}'. Total users in DB: {}",
            adminUser.getUsername(), userRepository.count());
    }
}
