package com.hyperchip.admin.repository;

import com.hyperchip.admin.dto.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * UserRepository
 *
 * Handles database operations for the User entity.
 * Includes standard CRUD (via JpaRepository) and custom queries for
 * searching, filtering, and checking uniqueness.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    // -----------------------------
    // SEARCH ACTIVE USERS BY NAME OR EMAIL
    // -----------------------------
    // Returns users that are not deleted and whose name or email contains the query string (case-insensitive)
    @Query("SELECT u FROM User u " +
            "WHERE u.deleted = false AND " +
            "(LOWER(u.name) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<User> searchActive(@Param("q") String q, Pageable pageable);

    // -----------------------------
    // LIST ALL ACTIVE USERS
    // -----------------------------
    // Simply returns users where deleted = false
    Page<User> findAllByDeletedFalse(Pageable pageable);

    // -----------------------------
    // FILTER ACTIVE USERS BY ROLE (CASE-INSENSITIVE)
    // -----------------------------
    // Example usage: role = "ADMIN" or "USER"
    Page<User> findByRoleContainingIgnoreCaseAndDeletedFalse(String role, Pageable pageable);

    // -----------------------------
    // CHECK IF EMAIL EXISTS AMONG ACTIVE USERS
    // -----------------------------
    // Useful for signup or update validation
    boolean existsByEmailIgnoreCaseAndDeletedFalse(String email);

    // -----------------------------
    // OPTIONAL: LIST ACTIVE USERS (ALTERNATIVE METHOD NAME)
    // -----------------------------
    Page<User> findByDeletedFalse(Pageable pageable);
}
