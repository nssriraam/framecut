package com.framecut.config;

import com.framecut.entity.User;
import com.framecut.repository.ReviewRepository;
import com.framecut.repository.UserRepository;
import com.framecut.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Running DataInitializer for admin account setup...");

        final String targetEmail = "nosriraam7@gmail.com";
        final String targetUsername = "golddaizen";
        final String targetPassword = "tatake0";

        // 1. Look for existing user by email or username
        Optional<User> byEmail = userRepository.findByEmail(targetEmail);
        Optional<User> byUsername = userRepository.findByUsername(targetUsername);

        User adminUser;
        if (byEmail.isPresent()) {
            adminUser = byEmail.get();
            adminUser.setUsername(targetUsername);
            adminUser.setEmail(targetEmail);
            adminUser.setPassword(passwordEncoder.encode(targetPassword));
            adminUser.setRole(User.Role.ADMIN);
            userRepository.save(adminUser);
            log.info("Updated existing user by email '{}' to username '{}' with ADMIN role and new password", targetEmail, targetUsername);
        } else if (byUsername.isPresent()) {
            adminUser = byUsername.get();
            adminUser.setEmail(targetEmail);
            adminUser.setPassword(passwordEncoder.encode(targetPassword));
            adminUser.setRole(User.Role.ADMIN);
            userRepository.save(adminUser);
            log.info("Updated existing user by username '{}' with email '{}' and ADMIN role", targetUsername, targetEmail);
        } else {
            adminUser = User.builder()
                .username(targetUsername)
                .email(targetEmail)
                .password(passwordEncoder.encode(targetPassword))
                .role(User.Role.ADMIN)
                .build();
            userRepository.save(adminUser);
            log.info("Created new admin user '{}' with email '{}' and ADMIN role", targetUsername, targetEmail);
        }

        // 2. Clean up any other test users (keep only the admin account)
        List<User> allUsers = userRepository.findAll();
        for (User u : allUsers) {
            if (!u.getId().equals(adminUser.getId())) {
                log.info("Purging other user account: id={}, username={}, email={}", u.getId(), u.getUsername(), u.getEmail());
                try {
                    watchlistRepository.findAll().stream()
                        .filter(w -> w.getUser() != null && w.getUser().getId().equals(u.getId()))
                        .forEach(watchlistRepository::delete);
                    reviewRepository.findAll().stream()
                        .filter(r -> r.getUser() != null && r.getUser().getId().equals(u.getId()))
                        .forEach(reviewRepository::delete);
                    userRepository.delete(u);
                } catch (Exception ex) {
                    log.error("Failed to delete old test user {}: {}", u.getUsername(), ex.getMessage());
                }
            }
        }

        log.info("DataInitializer completed. Active Admin: username='{}', email='{}', total users in DB: {}",
            adminUser.getUsername(), adminUser.getEmail(), userRepository.count());
    }
}
