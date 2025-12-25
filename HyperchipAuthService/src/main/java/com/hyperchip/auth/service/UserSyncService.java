package com.hyperchip.auth.service;

import com.hyperchip.auth.dto.UserDto;
import com.hyperchip.auth.model.User;
import com.hyperchip.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;

/**
 * UserSyncService
 * ----------------
 * This service is used to sync users from the auth-service
 * to the user-service.
 *
 * Why this is needed:
 * - Auth-service manages login and security
 * - User-service manages user profile and business data
 *
 * This class sends all existing users from auth DB
 * to user-service in one-by-one manner.
 */
@Service
@RequiredArgsConstructor
public class UserSyncService {

    // Repository to read users from auth database
    private final UserRepository userRepository;

    // RestTemplate is used to call user-service APIs
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Sync all users from auth-service to user-service.
     *
     * What this method does:
     * 1. Fetches all users from auth database
     * 2. Converts each User entity into UserDto
     * 3. Sends user data to user-service API
     * 4. Continues even if one user fails (non-blocking)
     *
     * This method is usually used:
     * - After deploying user-service
     * - During migration
     * - For manual admin sync
     */
    public void syncAllUsersToUserService() {

        // Step 1: Get all users from auth database
        List<User> users = userRepository.findAll();

        // Target endpoint in user-service
        String url = "http://localhost:8083/api/users/from-auth";

        // Step 2: Loop through each user and sync
        for (User user : users) {
            try {
                // Convert User entity to UserDto
                UserDto userDto = new UserDto();
                userDto.setId(user.getId());
                userDto.setFullName(user.getFullName());
                userDto.setEmail(user.getEmail());
                userDto.setPhone(user.getPhone());
                userDto.setRole(user.getRole());

                // Prepare HTTP headers
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                // Wrap DTO and headers into request
                HttpEntity<UserDto> request =
                        new HttpEntity<>(userDto, headers);

                // Step 3: Call user-service API
                restTemplate.postForEntity(url, request, Void.class);

                // Log success
                System.out.println("✅ Synced user: " + user.getEmail());

            } catch (Exception e) {
                // Step 4: Handle failure gracefully
                // Do not stop syncing other users
                System.err.println(
                        "⚠️ Failed to sync user " + user.getEmail()
                                + " : " + e.getMessage()
                );
            }
        }
    }
}
