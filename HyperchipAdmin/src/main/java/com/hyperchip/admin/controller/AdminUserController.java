package com.hyperchip.admin.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.annotation.PostConstruct;
import java.security.Principal;
import java.util.Map;

/**
 * AdminUserController
 *
 * Purpose:
 * Handles all Admin-side User Management UI operations.
 * This controller acts as a bridge between the Admin UI (Thymeleaf)
 * and backend microservices like auth-service and user-service.
 */
@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Slf4j
public class AdminUserController {

    /**
     * RestTemplate is used to communicate with other microservices
     * (auth-service and user-service) from the admin UI module.
     */
    private final RestTemplate restTemplate;

    /**
     * Base URL for user-service APIs.
     * Used mainly for user profile operations like soft delete.
     */
    @Value("${user.service.url}")
    private String userServiceBaseUrl;

    /**
     * Base URL for auth-service APIs.
     * Used for authentication-related actions such as block/unblock.
     */
    @Value("${auth.service.url}")
    private String authServiceBaseUrl;

    /**
     * Purpose:
     * Ensures that service URLs do not contain accidental leading/trailing spaces.
     * This prevents malformed URLs during RestTemplate calls.
     */
    @PostConstruct
    public void init() {
        if (authServiceBaseUrl != null) {
            authServiceBaseUrl = authServiceBaseUrl.trim();
        }
        if (userServiceBaseUrl != null) {
            userServiceBaseUrl = userServiceBaseUrl.trim();
        }
    }

    // --------------------------------------------------------
    // LIST / SEARCH USERS
    // --------------------------------------------------------

    /**
     * Purpose:
     * Displays the Admin User List page.
     *
     * What it does:
     * - Fetches paginated users from auth-service
     * - Supports search by keyword
     * - Sends user data to Thymeleaf UI
     *
     * Why it is needed:
     * Allows admin to view and search users efficiently.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String listUsers(
            @RequestParam(value = "keyword", required = false) String keyword,
            Model model,
            @PageableDefault(size = 20)
            org.springframework.data.domain.Pageable pageable
    ) {

        log.debug(
                "Admin UI request to list users | keyword='{}' | page={}",
                keyword,
                pageable.getPageNumber()
        );

        // Build user-service URL dynamically with optional search & pagination
        String url = UriComponentsBuilder.fromHttpUrl(authServiceBaseUrl)
                .path("/api/admin/users")
                .queryParamIfPresent("q", java.util.Optional.ofNullable(keyword))
                .queryParam("page", pageable.getPageNumber())
                .queryParam("size", pageable.getPageSize())
                .toUriString();

        try {
            // Call auth-service to fetch user list
            ResponseEntity<Map[]> resp =
                    restTemplate.getForEntity(url, Map[].class);

            Map[] users = resp.getBody();

            // Add users to model for UI rendering
            model.addAttribute("users", users == null ? new Map[0] : users);

            // Debug logging to understand response structure
            if (users != null && users.length > 0) {
                log.info("First user record: {}", users[0]);
                log.info("User keys: {}", users[0].keySet());
            }

        } catch (Exception e) {
            // Handle service failure gracefully
            log.error("Failed to fetch users from auth-service: {}", url, e);
            model.addAttribute("users", new Map[0]);
            model.addAttribute(
                    "errorMsg",
                    "Failed to load users (service unavailable)."
            );
        }

        // Preserve search and pagination state in UI
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("currentPage", pageable.getPageNumber());

        return "admin/user-list";
    }

    // --------------------------------------------------------
    // DELETE USER (SOFT DELETE)
    // --------------------------------------------------------

    /**
     * Purpose:
     * Performs a soft delete on a user account.
     *
     * What it does:
     * - Marks user as deleted without removing DB records
     * - Sends admin identity for audit tracking
     *
     * Why it is needed:
     * Allows recovery and auditing instead of permanent deletion.
     */
    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteUser(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes ra
    ) {

        String admin = principal != null ? principal.getName() : "SYSTEM";
        log.info("Admin '{}' requested soft delete for user id={}", admin, id);

        String url = userServiceBaseUrl + "/api/users/{id}/soft-delete";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Include admin name for audit logging in backend
        HttpEntity<Map<String, String>> req =
                new HttpEntity<>(Map.of("performedBy", admin), headers);

        try {
            restTemplate.exchange(url, HttpMethod.PATCH, req, Void.class, id);
            ra.addFlashAttribute("succMsg", "User deleted successfully.");

        } catch (Exception e) {
            log.error("Soft delete failed for user id={}", id, e);
            ra.addFlashAttribute("errorMsg", "Delete failed.");
        }

        return "redirect:/admin/users";
    }

    // --------------------------------------------------------
    // BLOCK USER
    // --------------------------------------------------------

    /**
     * Purpose:
     * Blocks a user from logging in or accessing the system.
     *
     * What it does:
     * - Calls auth-service to mark user as blocked
     * - Stores admin identity for audit
     */
    @PostMapping("/{id}/block")
    @PreAuthorize("hasRole('ADMIN')")
    public String blockUser(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes ra
    ) {

        String admin = principal != null ? principal.getName() : "SYSTEM";
        log.info("Admin '{}' requested block for user id={}", admin, id);

        String url = authServiceBaseUrl + "/api/admin/users/{id}/block";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> req =
                new HttpEntity<>(
                        Map.of("blocked", true, "performedBy", admin),
                        headers
                );

        try {
            restTemplate.postForEntity(url, req, Void.class, id);
            ra.addFlashAttribute("succMsg", "User blocked successfully.");

        } catch (Exception e) {
            log.error("User block failed for id={}", id, e);
            ra.addFlashAttribute("errorMsg", "Block failed.");
        }

        return "redirect:/admin/users";
    }

    // --------------------------------------------------------
    // UNBLOCK USER
    // --------------------------------------------------------

    /**
     * Purpose:
     * Restores access to a previously blocked user.
     *
     * What it does:
     * - Calls auth-service to remove block flag
     */
    @PostMapping("/{id}/unblock")
    @PreAuthorize("hasRole('ADMIN')")
    public String unblockUser(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes ra
    ) {

        String admin = principal != null ? principal.getName() : "SYSTEM";
        log.info("Admin '{}' requested unblock for user id={}", admin, id);

        String url = authServiceBaseUrl + "/api/admin/users/{id}/unblock";

        HttpEntity<Map<String, Object>> req =
                new HttpEntity<>(
                        Map.of("blocked", false, "performedBy", admin)
                );

        try {
            restTemplate.postForEntity(url, req, Void.class, id);
            ra.addFlashAttribute("succMsg", "User unblocked successfully.");

        } catch (Exception e) {
            log.error("User unblock failed for id={}", id, e);
            ra.addFlashAttribute("errorMsg", "Unblock failed.");
        }

        return "redirect:/admin/users";
    }

    // --------------------------------------------------------
    // TOGGLE BLOCK / UNBLOCK
    // --------------------------------------------------------

    /**
     * Purpose:
     * Dynamically toggles user block status based on current state.
     *
     * What it does:
     * - Fetches current block status
     * - Automatically decides block or unblock
     *
     * Why it is useful:
     * Simplifies UI logic with a single action button.
     */
    @PostMapping("/block-unblock/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String toggleBlockUser(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes ra
    ) {

        String admin = principal != null ? principal.getName() : "SYSTEM";
        log.info("Admin '{}' requested toggle block for user id={}", admin, id);

        String userUrl = authServiceBaseUrl + "/api/admin/users/{id}";

        Map<String, Object> user = null;
        try {
            user = restTemplate.getForObject(userUrl, Map.class, id);
        } catch (Exception e) {
            log.warn("Could not fetch user details: {}", e.getMessage());
        }

        boolean currentlyBlocked =
                user != null && Boolean.TRUE.equals(user.get("blocked"));

        String action = currentlyBlocked ? "unblock" : "block";
        String url =
                authServiceBaseUrl + "/api/admin/users/{id}/" + action;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> req =
                new HttpEntity<>(
                        Map.of(
                                "blocked", !currentlyBlocked,
                                "performedBy", admin
                        ),
                        headers
                );

        try {
            restTemplate.postForEntity(url, req, Void.class, id);
            ra.addFlashAttribute(
                    "succMsg",
                    currentlyBlocked
                            ? "User unblocked successfully."
                            : "User blocked successfully."
            );

        } catch (Exception e) {
            log.error("Toggle block failed for id={}", id, e);
            ra.addFlashAttribute(
                    "errorMsg",
                    "Block/unblock failed. Please check services."
            );
        }

        return "redirect:/admin/users";
    }
}
