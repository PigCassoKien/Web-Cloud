// java
package com.smartqueue.aws.service;

import com.smartqueue.aws.dto.user.CreateUserRequest;
import com.smartqueue.aws.dto.user.UserResponse;
import com.smartqueue.aws.event.UserRegisteredEvent;
import com.smartqueue.aws.model.User;
import com.smartqueue.aws.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher applicationEventPublisher;

    public Mono<UserResponse> createUser(CreateUserRequest request) {
        log.info("Creating new user with email: {}", request.getEmail());

        return Mono.fromCallable(() -> {
                    Optional<User> existingUser = userRepository.findByEmail(request.getEmail());
                    if (existingUser.isPresent()) {
                        throw new RuntimeException("User with email already exists");
                    }

                    User user = User.builder()
                            .userId(UUID.randomUUID().toString())
                            .email(request.getEmail())
                            .phone(request.getPhone())
                            .name(request.getName())
                            .password(passwordEncoder.encode(request.getPassword()))
                            .emailNotificationEnabled(request.isEmailNotificationEnabled())
                            .smsNotificationEnabled(request.isSmsNotificationEnabled())
                            .createdAt(LocalDateTime.now())
                            .lastLoginAt(LocalDateTime.now())
                            .isActive(true)
                            .build();

                    userRepository.save(user);
                    applicationEventPublisher.publishEvent(new UserRegisteredEvent(this, user));

                    return UserResponse.builder()
                            .userId(user.getUserId())
                            .email(user.getEmail())
                            .phone(user.getPhone())
                            .name(user.getName())
                            .emailNotificationEnabled(user.isEmailNotificationEnabled())
                            .smsNotificationEnabled(user.isSmsNotificationEnabled())
                            .createdAt(user.getCreatedAt())
                            .lastLoginAt(user.getLastLoginAt())
                            .isActive(user.isActive())
                            .build();
                })
                .doOnSuccess(userResponse ->
                        log.info("Successfully created user: {}", userResponse.getUserId())
                )
                .doOnError(error ->
                        log.error("Failed to create user with email: {}", request.getEmail(), error)
                );
    }

    public Mono<UserResponse> getUserById(String userId) {
        log.debug("Fetching user by ID: {}", userId);

        return Mono.fromCallable(() -> {
            return userRepository.findById(userId)
                    .map(user -> UserResponse.builder()
                            .userId(user.getUserId())
                            .email(user.getEmail())
                            .phone(user.getPhone())
                            .name(user.getName())
                            .emailNotificationEnabled(user.isEmailNotificationEnabled())
                            .smsNotificationEnabled(user.isSmsNotificationEnabled())
                            .createdAt(user.getCreatedAt())
                            .lastLoginAt(user.getLastLoginAt())
                            .isActive(user.isActive())
                            .build())
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        });
    }

    public Mono<Void> updateLastLogin(String userId) {
        log.debug("Updating last login for user: {}", userId);

        return Mono.fromRunnable(() -> {
            userRepository.findById(userId)
                    .ifPresent(user -> {
                        user.setLastLoginAt(LocalDateTime.now());
                        userRepository.save(user);
                    });
        });
    }

    public Mono<UserResponse> updateNotificationPreferences(String userId, boolean emailEnabled, boolean smsEnabled) {
        log.debug("Updating notification preferences for user: {}", userId);

        return Mono.fromCallable(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

            user.setEmailNotificationEnabled(emailEnabled);
            user.setSmsNotificationEnabled(smsEnabled);
            userRepository.save(user);

            return UserResponse.builder()
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .name(user.getName())
                    .emailNotificationEnabled(user.isEmailNotificationEnabled())
                    .smsNotificationEnabled(user.isSmsNotificationEnabled())
                    .createdAt(user.getCreatedAt())
                    .lastLoginAt(user.getLastLoginAt())
                    .isActive(user.isActive())
                    .build();
        });
    }

    public Mono<Boolean> authenticateUser(String email, String password) {
        log.debug("Authenticating user with email: {}", email);

        return Mono.fromCallable(() -> {
                    User user = userRepository.findByEmail(email)
                            .orElseThrow(() -> new RuntimeException("Invalid credentials"));

                    boolean matches = passwordEncoder.matches(password, user.getPassword());

                    if (matches) {
                        user.setLastLoginAt(LocalDateTime.now());
                        userRepository.save(user);
                        log.info("User authenticated successfully: {}", email);
                    } else {
                        log.warn("Authentication failed - password mismatch for email: {}", email);
                    }

                    return matches;
                })
                .onErrorResume(error -> {
                    log.error("Authentication error for email: {}", email, error);
                    return Mono.just(false);
                });
    }

    // New helper used by UserController
    public Mono<User> getUserByEmail(String email) {
        log.debug("Fetching user by email: {}", email);
        try {
            return Mono.justOrEmpty(userRepository.findByEmail(email));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
}
