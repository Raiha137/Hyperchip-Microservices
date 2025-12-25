package com.hyperchip.user.service.other;

import com.hyperchip.user.model.UserDtls;
import com.hyperchip.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Optional;

/**
 * Component to provide details of the currently authenticated user.
 *
 * Purpose:
 * - Helps controllers and services quickly obtain the current user's ID.
 * - Abstracts the logic of fetching UserDtls from the principal/email.
 * - Keeps authentication-related utility logic separate from business logic.
 */
@Component
@RequiredArgsConstructor // Generates constructor for final fields
public class CurrentUserDetailProvider {

    /**
     * Repository to fetch user details from the database.
     */
    private final UserRepository userRepository;

    /**
     * Get the current authenticated user's ID.
     *
     * Purpose:
     * - Maps the Spring Security Principal to a UserDtls record in the database.
     * - Returns the internal database ID of the user.
     *
     * Usage:
     * - Controllers or services can call this to know which user is currently logged in.
     * - Returns null if the principal is null or no matching user is found.
     *
     * @param principal Authenticated principal provided by Spring Security
     * @return Long representing UserDtls.id, or null if not found
     */
    public Long getUserId(Principal principal) {
        if (principal == null) return null;

        // Principal's name is usually the email or username
        String username = principal.getName();

        // Fetch user from repository by email
        Optional<UserDtls> userOptional = userRepository.findByEmail(username);

        // Return user's ID if present, else null
        return userOptional.map(UserDtls::getId).orElse(null);
    }

    /**
     * Convenience method for controllers or legacy code that expects Integer user IDs.
     *
     * Purpose:
     * - Converts Long ID to Integer for monolithic compatibility.
     * - Returns null if user ID is not found.
     *
     * @param principal Authenticated principal
     * @return Integer user ID or null
     */
    public Integer getUserIdAsInteger(Principal principal) {
        Long id = getUserId(principal);
        return id == null ? null : (int) (long) id; // explicit cast from Long to int
    }
}
