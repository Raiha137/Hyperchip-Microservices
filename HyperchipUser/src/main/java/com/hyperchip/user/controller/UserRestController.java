package com.hyperchip.user.controller;

import com.hyperchip.common.dto.UserDto;
import com.hyperchip.user.model.Address;
import com.hyperchip.user.model.UserDtls;
import com.hyperchip.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * UserRestController
 * ------------------
 * Provides REST APIs for managing users and their addresses.
 * Includes listing users, fetching a single user, soft deletion,
 * and CRUD operations on user addresses.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class UserRestController {

    private final UserService userService;

    /**
     * GET /api/users
     * ----------------
     * Lists all users with optional search query and pagination.
     *
     * @param q        Optional search query string (search by name/email)
     * @param pageable Spring Pageable object to handle page, size, sort
     * @return paginated list of UserDtls
     */
    @GetMapping("/users")
    public ResponseEntity<Page<UserDtls>> listUsers(
            @RequestParam(value = "q", required = false) String q,
            Pageable pageable) {

        // Delegates to service to handle search and pagination
        Page<UserDtls> page = userService.listUsers(q, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * GET /api/users/{id}
     * --------------------
     * Fetches details of a single user by ID.
     *
     * @param id User ID
     * @return UserDtls object if found, 404 otherwise
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<UserDtls> getUser(@PathVariable Long id) {
        return userService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * PATCH /api/users/{id}/soft-delete
     * -----------------------------------
     * Soft-deletes a user (marks as deleted without removing from DB).
     * Optional body can contain "performedBy" to log who performed the deletion.
     *
     * @param id   User ID
     * @param body Optional JSON body with "performedBy"
     * @return HTTP 200 OK
     */
    @PatchMapping("/users/{id}/soft-delete")
    public ResponseEntity<Void> softDeleteUser(@PathVariable Long id,
                                               @RequestBody(required = false) Map<String, String> body) {
        String performedBy = body != null ? body.get("performedBy") : "SYSTEM";
        userService.softDeleteUser(id, performedBy);
        return ResponseEntity.ok().build();
    }

    /**
     * PATCH /api/addresses/{id}/soft-delete
     * --------------------------------------
     * Soft-deletes a user address.
     * Optional body can contain "performedBy" to log who performed the deletion.
     *
     * @param id   Address ID
     * @param body Optional JSON body with "performedBy"
     * @return HTTP 200 OK
     */
    @PatchMapping("/addresses/{id}/soft-delete")
    public ResponseEntity<Void> softDeleteAddress(@PathVariable Long id,
                                                  @RequestBody(required = false) Map<String, String> body) {
        String performedBy = body != null ? body.get("performedBy") : "SYSTEM";
        userService.softDeleteAddress(id, performedBy);
        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/users/{id}/addresses
     * -------------------------------
     * Lists all addresses belonging to a specific user.
     *
     * @param id User ID
     * @return List of Address objects
     */
    @GetMapping("/users/{id}/addresses")
    public ResponseEntity<?> listAddresses(@PathVariable Long id) {
        return ResponseEntity.ok(userService.listAddresses(id));
    }

    /**
     * POST /api/users/{id}/addresses
     * -------------------------------
     * Adds a new address for a specific user.
     *
     * @param id      User ID
     * @param address Address object (JSON body)
     * @return Newly created Address object with HTTP 201 status
     */
    @PostMapping("/users/{id}/addresses")
    public ResponseEntity<Address> addAddress(@PathVariable Long id, @RequestBody Address address) {
        Address saved = userService.addAddress(id, address);
        return ResponseEntity.status(201).body(saved);
    }

    /**
     * PATCH /api/addresses/{id}
     * ---------------------------
     * Updates an existing address partially.
     * Only fields provided in the body will be updated.
     *
     * @param id      Address ID
     * @param address Address object containing updated fields
     * @return Updated Address object
     */
    @PatchMapping("/addresses/{id}")
    public ResponseEntity<Address> updateAddress(@PathVariable Long id, @RequestBody Address address) {
        return ResponseEntity.ok(userService.updateAddress(id, address));
    }

    /**
     * POST /api/users/from-auth
     * --------------------------
     * Creates a new user from authentication data (e.g., SSO/Google login).
     *
     * @param userDto UserDto object containing user information
     * @return Newly created UserDtls object with HTTP 201 status
     */
    @PostMapping("/users/from-auth")
    public ResponseEntity<UserDtls> createFromAuth(@RequestBody UserDto userDto) {
        return ResponseEntity.status(201).body(userService.createUserFromAuth(userDto));
    }

}
