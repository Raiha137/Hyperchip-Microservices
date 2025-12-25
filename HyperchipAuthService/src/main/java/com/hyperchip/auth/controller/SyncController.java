package com.hyperchip.auth.controller;

import com.hyperchip.auth.service.UserSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SyncController
 *
 * Medium-simple developer comments:
 * - This controller is used to manually trigger user synchronization.
 * - It calls UserSyncService to push all users from Auth service
 *   to the User / Profile service (or any external service).
 * - Mostly used for:
 *   - Admin maintenance
 *   - Initial data sync
 *   - Debugging or manual retry
 *
 * NOTE:
 * - This endpoint should be protected (ADMIN only) in production.
 */
@RestController
@RequiredArgsConstructor
public class SyncController {

    // Service that contains the actual sync logic
    private final UserSyncService userSyncService;

    /**
     * Sync all users to external User Service.
     *
     * Flow:
     * 1. Calls service method to fetch all users
     * 2. Sends user data to another service (via REST or messaging)
     * 3. Returns a simple success message
     */
    @GetMapping("/sync-users")
    public String syncUsers() {

        // Trigger full user synchronization
        userSyncService.syncAllUsersToUserService();

        // Simple response for confirmation
        return "✅ Sync completed!";
    }
}
