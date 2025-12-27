package com.hyperchip.order.controller;

import com.hyperchip.common.dto.OrderResponse;
import com.hyperchip.common.dto.PlaceOrderRequest;
import com.hyperchip.common.dto.PlaceOrderResponse;
import com.hyperchip.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * OrderUserLegacyController
 *
 * Purpose:
 * - Maintain backward-compatible endpoints for legacy clients.
 * - Handles old URLs under /api/user/orders/**.
 * - Required because some services (payment-service, user-service proxy) still call these URLs.
 *
 * Developer notes:
 * - Uses OrderService for all business logic.
 * - Provides endpoints for placing an order, fetching a single order, and listing orders for a user.
 * - Returns standard JSON responses using ResponseEntity.
 */
@RestController
@RequestMapping("/api/user/orders")
@RequiredArgsConstructor
public class OrderUserLegacyController {

    private final OrderService orderService;

    /**
     * Place an order (legacy endpoint).
     *
     * Flow:
     * 1. Accept a PlaceOrderRequest JSON body (optional for backward compatibility).
     * 2. Delegate to OrderService.placeOrder().
     * 3. Return PlaceOrderResponse with order confirmation details.
     *
     * URL: POST /api/user/orders/place
     *
     * @param req PlaceOrderRequest DTO
     * @return PlaceOrderResponse DTO
     */
    @PostMapping("/place")
    public ResponseEntity<PlaceOrderResponse> placeOrderLegacy(
            @RequestBody(required = false) PlaceOrderRequest req) {
        PlaceOrderResponse resp = orderService.placeOrder(req);
        return ResponseEntity.ok(resp);
    }

    /**
     * Fetch a single order by ID or order number (legacy endpoint).
     *
     * Flow:
     * 1. Accept idOrOrderNumber as path variable.
     * 2. Delegate to OrderService.getOrderByIdentifier().
     * 3. Return OrderResponse as JSON.
     *
     * URL: GET /api/user/orders/{idOrOrderNumber}
     *
     * @param idOrOrderNumber numeric ID or string order number
     * @return OrderResponse DTO
     */
    @GetMapping("/{idOrOrderNumber}")
    public ResponseEntity<OrderResponse> getOrderLegacy(
            @PathVariable String idOrOrderNumber) {
        OrderResponse resp = orderService.getOrderByIdentifier(idOrOrderNumber);
        return ResponseEntity.ok(resp);
    }

    /**
     * List all orders for a given user (legacy endpoint).
     *
     * Flow:
     * 1. Accept userId as path variable.
     * 2. Delegate to OrderService.getOrdersByUserId().
     * 3. Return list of OrderResponse DTOs as JSON.
     *
     * URL: GET /api/user/orders/user/{userId}
     *
     * @param userId user numeric ID
     * @return List of OrderResponse DTOs
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponse>> listForUserLegacy(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
    }
}
