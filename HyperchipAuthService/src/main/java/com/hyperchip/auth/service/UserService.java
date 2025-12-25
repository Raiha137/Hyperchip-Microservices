package com.hyperchip.auth.service;

import com.hyperchip.auth.model.User;
import java.util.Optional;

/**
 * UserService
 * -----------
 * This interface defines all user-related operations
 * used by the authentication service.
 *
 * The implementation of this interface will:
 *  - Register users
 *  - Check user existence
 *  - Load user data
 *  - Block and unblock users
 */
public interface UserService {

    /**
     * Register a new user in the auth service.
     *
     * What this method does:
     *  - Saves user details in auth database
     *  - Sets default role and status
     *  - Sends user data to user-service (if configured)
     *
     * @param user User entity with signup details
     * @return saved User object
     */
    User registerUser(User user);

    /**
     * Find a user by email address.
     *
     * Used in:
     *  - Login
     *  - OTP verification
     *  - Google / OAuth login
     *
     * @param email user email
     * @return Optional user (empty if not found)
     */
    Optional<User> findByEmail(String email);

    /**
     * Check whether an email already exists.
     *
     * Used during:
     *  - Signup validation
     *  - Prevent duplicate accounts
     *
     * @param email user email
     * @return true if email already exists
     */
    boolean emailExists(String email);

    /**
     * Block a user account.
     *
     * Used by:
     *  - Admin actions
     *  - Security enforcement
     *
     * Once blocked:
     *  - User cannot log in
     *  - User status remains in database
     *
     * @param userId ID of the user
     * @param reason Reason for blocking (optional)
     */
    void blockUser(Long userId, String reason);

    /**
     * Unblock a user account.
     *
     * Used when:
     *  - Admin restores access
     *  - Temporary blocks are removed
     *
     * @param userId ID of the user
     */
    void unblockUser(Long userId);

}
