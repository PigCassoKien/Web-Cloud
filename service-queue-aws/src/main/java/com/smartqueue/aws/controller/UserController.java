package com.smartqueue.aws.controller;

import com.smartqueue.aws.dto.user.CreateUserRequest;
import com.smartqueue.aws.dto.user.LoginRequest;
import com.smartqueue.aws.dto.user.UserResponse;
import com.smartqueue.aws.model.Ticket;
import com.smartqueue.aws.service.TicketService;
import com.smartqueue.aws.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
@Slf4j
@CrossOrigin(origins = "*")
public class UserController {
    
    private final UserService userService;

    private final TicketService ticketService;


    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<UserResponse> registerUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("User registration request for email: {}", request.getEmail());
        return userService.createUser(request);
    }

    @PostMapping(value = "/login")
    public Mono<ResponseEntity<Map<String, Object>>> login(@Valid @RequestBody LoginRequest request) {
        return userService.authenticateUser(request.getEmail(), request.getPassword())
            .flatMap(authenticated -> {
                if (authenticated) {
                    return userService.getUserByEmail(request.getEmail()) // thêm method này
                        .map(user -> ResponseEntity.ok(Map.of(
                            "message", "Login successful",
                            "user", Map.of(
                                "userId", user.getUserId(),
                                "email", user.getEmail(),
                                "name", user.getName()
                            )
                        )));
                } else {
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid credentials")));
                }
            });
    }

    @GetMapping("/{userId}")
    public Mono<UserResponse> getUser(@PathVariable String userId) {
        log.debug("Fetching user details for ID: {}", userId);
        return userService.getUserById(userId);
    }

    @PutMapping("/{userId}/notifications")
    public Mono<UserResponse> updateNotificationPreferences(
            @PathVariable String userId,
            @RequestParam boolean emailEnabled,
            @RequestParam boolean smsEnabled) {
        log.info("Updating notification preferences for user: {}", userId);
        return userService.updateNotificationPreferences(userId, emailEnabled, smsEnabled);
    }

    @PostMapping("/{userId}/last-login")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> updateLastLogin(@PathVariable String userId) {
        log.debug("Updating last login for user: {}", userId);
        return userService.updateLastLogin(userId);
    }

    @GetMapping("/{userId}/tickets")
    public ResponseEntity<List<Ticket>> getUserTickets(@PathVariable String userId) {
        List<Ticket> tickets = ticketService.getTicketsByUserId(userId);
        return ResponseEntity.ok(tickets);
    }
}