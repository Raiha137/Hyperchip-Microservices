package com.hyperchip.auth.repository;

import com.hyperchip.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * UserRepository
 *
 * Developer notes (medium):
 * - Handles database operations for User entity.
 * - Inherits standard CRUD operations from JpaRepository.
 * - Provides custom queries for email lookup and existence check.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by email.
     * Used for login, signup checks, or profile fetch.
     *
     * @param email User's email (unique)
     * @return Optional<User> found or empty
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a user exists with the given email.
     * Useful for signup validation.
     *
     * @param email User's email
     * @return true if user exists, false otherwise
     */
    boolean existsByEmail(String email);
}
