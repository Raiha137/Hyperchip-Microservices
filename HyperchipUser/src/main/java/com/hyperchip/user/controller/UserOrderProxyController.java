package com.hyperchip.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * UserOrderProxyController
 * -----------------------
 * Acts as a proxy between the user frontend and the Order Service.
 * Handles:
 *   - Canceling orders or order items
 *   - Return requests & cancellation
 *   - Replacement requests & cancellation
 * Each method constructs the appropriate URL and calls the Order Service via RestTemplate.
 */
@RestController
@RequestMapping("/user/api/proxy/orders")
@RequiredArgsConstructor
public class UserOrderProxyController {

    private final RestTemplate restTemplate;

    @Value("${order.service.url:http://localhost:8093}")
    private String orderServiceBase;   // Base URL for the Order Service

    /**
     * Utility method to remove trailing slash from base URL to avoid double-slash issues.
     */
    private String cleanBase() {
        return orderServiceBase.endsWith("/")
                ? orderServiceBase.substring(0, orderServiceBase.length() - 1)
                : orderServiceBase;
    }

    // ==================== 1️⃣ CANCEL WHOLE ORDER ====================

    /**
     * Cancel an entire order.
     * @param orderId The order ID to cancel
     * @param reason Optional reason for cancellation
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<String> cancelOrder(
            @PathVariable Long orderId,
            @RequestParam(required = false) String reason) {

        String url = cleanBase() + "/api/orders/" + orderId + "/cancel"
                + (reason != null ? "?reason=" + URLEncoder.encode(reason, StandardCharsets.UTF_8) : "");

        return restTemplate.postForEntity(url, null, String.class);
    }

    // ==================== 2️⃣ CANCEL ONE SPECIFIC ITEM IN ORDER ====================

    /**
     * Cancel a specific item within an order.
     * @param orderId The order ID
     * @param itemId The specific item ID to cancel
     * @param reason Optional reason for item cancellation
     */
    @PostMapping("/{orderId}/items/{itemId}/cancel")
    public ResponseEntity<String> cancelOrderItem(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @RequestParam(required = false) String reason) {

        String url = cleanBase() + "/api/orders/" + orderId + "/items/" + itemId + "/cancel"
                + (reason != null ? "?reason=" + URLEncoder.encode(reason, StandardCharsets.UTF_8) : "");

        return restTemplate.postForEntity(url, null, String.class);
    }

    // ==================== 3️⃣ RETURN REQUEST ====================

    /**
     * Submit a return request for an order.
     * @param orderId The order ID
     * @param reason The reason for return
     */
    @PostMapping("/{orderId}/return-request")
    public ResponseEntity<String> requestReturn(
            @PathVariable Long orderId,
            @RequestParam("reason") String reason) {

        String url = cleanBase() + "/api/orders/" + orderId + "/return-request?reason="
                + URLEncoder.encode(reason, StandardCharsets.UTF_8);

        return restTemplate.postForEntity(url, null, String.class);
    }

    // ==================== 4️⃣ CANCEL RETURN REQUEST ====================

    /**
     * Cancel a previously submitted return request.
     * @param orderId The order ID
     */
    @PostMapping("/{orderId}/cancel-return")
    public ResponseEntity<String> cancelReturn(@PathVariable Long orderId) {

        String url = cleanBase() + "/api/orders/" + orderId + "/cancel-return";
        return restTemplate.postForEntity(url, null, String.class);
    }

    // ==================== 5️⃣ REPLACEMENT REQUEST ====================

    /**
     * Submit a replacement request for an order.
     * @param orderId The order ID
     * @param reason Reason for replacement
     */
    @PostMapping("/{orderId}/replacement-request")
    public ResponseEntity<String> requestReplacement(
            @PathVariable Long orderId,
            @RequestParam("reason") String reason) {

        String url = cleanBase() + "/api/orders/" + orderId + "/replacement-request?reason="
                + URLEncoder.encode(reason, StandardCharsets.UTF_8);

        return restTemplate.postForEntity(url, null, String.class);
    }

    // ==================== 6️⃣ CANCEL REPLACEMENT REQUEST ====================

    /**
     * Cancel a previously submitted replacement request.
     * @param orderId The order ID
     */
    @PostMapping("/{orderId}/cancel-replacement")
    public ResponseEntity<String> cancelReplacement(@PathVariable Long orderId) {

        String url = cleanBase() + "/api/orders/" + orderId + "/cancel-replacement";
        return restTemplate.postForEntity(url, null, String.class);
    }
}
