package com.hyperchip.user.repository;

import com.hyperchip.user.model.UserDtls;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository interface for UserDtls entity.
 * Provides basic CRUD operations and custom queries for user management.
 */
public interface UserRepository extends JpaRepository<UserDtls, Long> {

    /**
     * Find a user by email.
     * Used to check existence or retrieve user info for login/verification.
     *
     * @param email user email
     * @return Optional<UserDtls>
     */
    Optional<UserDtls> findByEmail(String email);

    /**
     * Check if a user exists with the given email.
     * Useful for signup or validation checks.
     *
     * @param email user email
     * @return true if user exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Find a user by authentication service user ID.
     * Useful when mapping external auth users to local UserDtls.
     *
     * @param authUserId ID from AuthService
     * @return Optional<UserDtls>
     */
    Optional<UserDtls> findByAuthUserId(Long authUserId);

    /**
     * List all users that are not soft-deleted.
     * Soft-deleted users have deleted = true.
     *
     * @param pageable pagination info
     * @return Page<UserDtls> of active users
     */
    Page<UserDtls> findByDeletedFalse(Pageable pageable);

    /**
     * Search users by name OR email (case-insensitive), excluding soft-deleted users.
     * Supports backend search functionality with pagination.
     *
     * @param name    search term for name
     * @param email   search term for email
     * @param pageable pagination info
     * @return Page<UserDtls> of matched users
     */
    Page<UserDtls> findByDeletedFalseAndNameContainingIgnoreCaseOrDeletedFalseAndEmailContainingIgnoreCase(
            String name, String email, Pageable pageable);
}
