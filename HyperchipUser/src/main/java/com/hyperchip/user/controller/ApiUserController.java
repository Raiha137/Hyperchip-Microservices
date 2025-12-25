package com.hyperchip.user.controller;

import com.hyperchip.user.model.UserDtls;
import com.hyperchip.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ApiUserController
 * ----------------
 * REST controller for managing user-related operations from the admin side.
 *
 * Purpose:
 * - Expose APIs to list and search users
 * - Support pagination for efficient data retrieval
 * - Allow optional search query to filter users
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class ApiUserController {

    /**
     * userService
     * -----------
     * Service layer to handle business logic for user management.
     *
     * Why service is used:
     * - Encapsulates repository calls and filtering logic
     * - Ensures controller remains thin and focused on request handling
     */
    private final UserService userService;

    /**
     * getAllUsers
     * -----------
     * Retrieves a paginated list of users, optionally filtered by a search query.
     *
     * Endpoint:
     * GET /api/admin/users?q={query}&page={page}&size={size}
     *
     * Parameters:
     * - q    : optional search string (name, email, etc.)
     * - page : zero-based page index (default 0)
     * - size : page size (default 20)
     *
     * Returns:
     * - List<UserDtls> : page content as a list of users
     */
    @GetMapping
    public List<UserDtls> getAllUsers(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        // Delegate to service to fetch paginated & optionally filtered users
        Page<UserDtls> p = userService.listUsers(q, PageRequest.of(page, size));

        // Return only the page content as a List
        return p.getContent();
    }
}
