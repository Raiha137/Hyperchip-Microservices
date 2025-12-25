package com.hyperchip.auth.service.impl;

import com.hyperchip.auth.config.CustomUserDetails;
import com.hyperchip.auth.dto.UserDto;
import com.hyperchip.auth.model.User;
import com.hyperchip.auth.repository.UserRepository;
import com.hyperchip.auth.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

/**
 * UserServiceImpl
 * ----------------
 * This class handles:
 *  - User registration
 *  - Loading user for Spring Security login
 *  - Sending user data to user-service
 *  - Blocking and unblocking users
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService, UserDetailsService {

    // Repository to access auth database
    private final UserRepository userRepository;

    // Used to call another microservice (user-service)
    private final RestTemplate restTemplate;

    // URL of user-service (configured in application.properties)
    @Value("${user.service.url}")
    private String userServiceUrl;

    /**
     * Register a new user
     * -------------------
     * Steps:
     * 1. Set default values (role, active, enabled, etc.)
     * 2. Save user in auth database
     * 3. Send user details to user-service (non-blocking)
     */
    @Override
    @Transactional
    public User registerUser(User user) {

        // Set default values if not provided
        if (user.getRole() == null) {
            user.setRole("ROLE_USER");
        }
        user.setActive(true);
        user.setBlocked(false);
        user.setEnabled(true);

        log.info("Registering user with email={}", user.getEmail());

        User savedUser;

        try {
            // Save user in auth DB
            savedUser = userRepository.save(user);
            log.info("User saved successfully with id={}", savedUser.getId());
        } catch (DataAccessException ex) {
            // Database related errors
            log.error("Database error while saving user", ex);
            throw ex;
        } catch (Exception ex) {
            // Any unexpected error
            log.error("Unexpected error while saving user", ex);
            throw ex;
        }

        /*
         * Send user data to user-service
         * This is OPTIONAL and should NOT fail user registration
         */
        try {
            if (userServiceUrl != null && !userServiceUrl.isBlank()) {

                // Convert User entity to UserDto
                UserDto userDto = UserDto.builder()
                        .id(savedUser.getId())
                        .fullName(savedUser.getFullName())
                        .email(savedUser.getEmail())
                        .phone(savedUser.getPhone())
                        .role(savedUser.getRole())
                        .build();

                // Prepare HTTP request
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<UserDto> request =
                        new HttpEntity<>(userDto, headers);

                String url = userServiceUrl + "/api/users/from-auth";

                // Call user-service
                restTemplate.postForEntity(url, request, Void.class);

                log.info("User data sent to user-service");
            }
        } catch (Exception ex) {
            // If user-service fails, do NOT rollback auth registration
            log.warn("Failed to send user to user-service (ignored)", ex);
        }

        return savedUser;
    }

    /**
     * Find user by email
     */
    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Check if email already exists
     */
    @Override
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Load user for Spring Security authentication
     * ---------------------------------------------
     * This method is used during login
     */
    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        // Normalize email (avoid case and space issues)
        String normalizedEmail = email.trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found with email: " + normalizedEmail));

        // Convert User entity to Spring Security UserDetails
        return new CustomUserDetails(user);
    }

    /**
     * Block a user
     * ------------
     * Used by admin to block user access
     */
    public void blockUser(Long userId, String reason) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        user.setBlocked(true);
        user.setBlockReason(reason);

        userRepository.save(user);

        log.info("User blocked. userId={}, reason={}", userId, reason);
    }

    /**
     * Unblock a user
     * --------------
     * Allows user to login again
     */
    public void unblockUser(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        user.setBlocked(false);
        user.setBlockReason(null);

        userRepository.save(user);

        log.info("User unblocked. userId={}", userId);
    }
}
