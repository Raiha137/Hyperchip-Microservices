package com.hyperchip.admin.service;

import com.hyperchip.admin.dto.User;
import com.hyperchip.admin.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * AdminUserService
 *
 * Handles business logic for admin operations on users.
 * Includes listing with search & pagination, blocking/unblocking,
 * soft deletion, and fetching single users.
 */
@Service
public class AdminUserService {

    @Autowired
    private UserRepository userRepository;

    // ========================================================
    // LIST USERS WITH SEARCH, PAGINATION & SORTING
    // ========================================================
    /**
     * Fetch a paginated list of users.
     * - If 'q' is provided, searches by name or email (case-insensitive)
     * - Sorts by given field and direction
     *
     * @param q       Optional search query
     * @param page    Page number (0-based)
     * @param size    Page size
     * @param sortBy  Field to sort by (default: createdAt)
     * @param dir     Sort direction ("asc" or "desc", default: desc)
     * @return Page<User>
     */
    public Page<User> listUsers(String q, int page, int size, String sortBy, String dir) {

        String sortField = (sortBy != null && !sortBy.isEmpty()) ? sortBy : "createdAt";
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(dir)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortField));

        if (q != null && !q.isEmpty()) {
            // Search active users (not deleted) by name/email
            return userRepository.searchActive(q, pageable);
        } else {
            // List all active users (not deleted)
            return userRepository.findByDeletedFalse(pageable);
        }
    }

    // ========================================================
    // BLOCK USER
    // ========================================================
    /**
     * Block a user account (prevents login)
     *
     * @param id User ID
     * @return Updated User
     */
    public User blockUser(Long id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        u.setBlocked(true);
        return userRepository.save(u);
    }

    // ========================================================
    // UNBLOCK USER
    // ========================================================
    /**
     * Unblock a user account (allows login)
     *
     * @param id User ID
     * @return Updated User
     */
    public User unblockUser(Long id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        u.setBlocked(false);
        return userRepository.save(u);
    }

    // ========================================================
    // SOFT DELETE USER
    // ========================================================
    /**
     * Soft-delete a user (mark as deleted, do not remove from DB)
     *
     * @param id User ID
     * @return Updated User
     */
    public User softDeleteUser(Long id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        u.setDeleted(true);
        return userRepository.save(u);
    }

    // ========================================================
    // GET USER BY ID (ONLY IF NOT DELETED)
    // ========================================================
    /**
     * Fetch a user by ID, only if not soft-deleted
     *
     * @param id User ID
     * @return Optional<User>
     */
    public Optional<User> getById(Long id) {
        return userRepository.findById(id)
                .filter(u -> !u.isDeleted());
    }
}
