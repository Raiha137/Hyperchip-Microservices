package com.hyperchip.user.controller;

import com.hyperchip.common.dto.UserOrderNotificationDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * NotificationController
 * ----------------------
 * Handles notifications related to user orders.
 *
 * Purpose:
 * - Receives order status updates (e.g., from Order Service or external systems)
 * - Logs notifications for monitoring or further processing
 * - Provides a REST endpoint for asynchronous order notifications
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications/orders")
public class NotificationController {

    /**
     * orderStatusNotification
     * -----------------------
     * POST /api/notifications/orders/status
     *
     * Purpose:
     * - Accepts a UserOrderNotificationDto payload
     * - Logs the received notification
     * - Responds with HTTP 202 Accepted to acknowledge receipt
     */
    @PostMapping("/status")
    public ResponseEntity<Void> orderStatusNotification(@RequestBody UserOrderNotificationDto dto) {
        log.info("Received order status notification: {}", dto);
        return ResponseEntity.accepted().build();
    }
}