package com.hyperchip.auth.controller;

import com.hyperchip.auth.model.User;
import com.hyperchip.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * AuthAdminRestController
 *
 * Developer simple notes:
 * - This controller is ONLY for ADMIN usage
 * - Used to manage users from admin panel
 * - Supports:
 *   1. View single user
 *   2. List all users (with search)
 *   3. Block user
 *   4. Unblock user
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
public class AuthAdminRestController {

    private final UserRepository userRepository;

    /**
     * Get single user by ID
     *
     * URL:
     *   GET /api/admin/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getUser(@PathVariable Long id) {

        Optional<User> opt = userRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User u = opt.get();

        return ResponseEntity.ok(
                Map.of(
                        "id", u.getId(),
                        "email", u.getEmail(),
                        "blocked", u.getBlocked() != null ? u.getBlocked() : false
                )
        );
    }

    /**
     * Block a user
     *
     * URL:
     *   POST /api/admin/users/{id}/block
     *
     * Developer note:
     * - Once blocked, user cannot login
     */
    @PostMapping("/{id}/block")
    public ResponseEntity<Void> blockUser(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body
    ) {

        Optional<User> opt = userRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User u = opt.get();
        u.setBlocked(Boolean.TRUE);
        userRepository.save(u);

        return ResponseEntity.noContent().build();
    }

    /**
     * Unblock a user
     *
     * URL:
     *   POST /api/admin/users/{id}/unblock
     */
    @PostMapping("/{id}/unblock")
    public ResponseEntity<Void> unblockUser(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body
    ) {

        Optional<User> opt = userRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User u = opt.get();
        u.setBlocked(Boolean.FALSE);
        userRepository.save(u);

        return ResponseEntity.noContent().build();
    }

    /**
     * Get all users (with optional search)
     *
     * URL:
     *   GET /api/admin/users
     *   GET /api/admin/users?q=keyword
     *
     * Search applies to:
     * - Full name
     * - Email
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllUsers(
            @RequestParam(value = "q", required = false) String q
    ) {

        var stream = userRepository.findAll().stream();

        // Apply search filter if keyword exists
        if (q != null && !q.isBlank()) {
            String kw = q.toLowerCase();
            stream = stream.filter(u ->
                    (u.getFullName() != null && u.getFullName().toLowerCase().contains(kw)) ||
                            (u.getEmail() != null && u.getEmail().toLowerCase().contains(kw))
            );
        }

        // Map entity -> simple response
        List<Map<String, Object>> users = stream
                .map(u -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", u.getId());
                    m.put("email", u.getEmail());
                    m.put("blocked", u.getBlocked() != null ? u.getBlocked() : false);
                    m.put("fullName", u.getFullName());
                    m.put("phone", u.getPhone());
                    return m;
                })
                .toList();

        return ResponseEntity.ok(users);
    }
}
