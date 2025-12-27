package com.hyperchip.order.controller;

import com.hyperchip.common.dto.PlaceOrderResponse;
import com.hyperchip.common.dto.OrderResponse;
import com.hyperchip.order.model.OrderStatus;
import com.hyperchip.order.service.OrderService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * OrderAdminRestController
 *
 * Purpose:
 * - Provides REST APIs for administrative operations on orders.
 * - Supports listing orders, fetching order details, updating status,
 *   canceling orders, returning orders, and downloading invoices.
 *
 * Developer notes:
 * - All business logic is delegated to OrderService.
 * - Uses ResponseEntity for flexible HTTP responses, including status codes and headers.
 * - Validates input where necessary (status values, required fields).
 * - Commented-out code represents optional endpoints that were re-enabled at the bottom.
 */
@RestController
@RequestMapping("/api/admin/orders")
public class OrderAdminRestController {

    private final OrderService orderService;

    public OrderAdminRestController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * List orders for admin
     *
     * Flow:
     * 1. Accept optional paging parameters (page, size), status filter, and free-text search q.
     * 2. Delegate to orderService.listForAdmin().
     * 3. Return list of OrderResponse DTOs.
     *
     * @param page   page number (default 0)
     * @param size   page size (default 20)
     * @param status optional filter by order status
     * @param q      optional free-text query (order number search)
     * @return list of orders as OrderResponse DTOs
     */
    @GetMapping
    public ResponseEntity<List<OrderResponse>> listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q
    ) {
        List<OrderResponse> list = orderService.listForAdmin(page, size, status, q);
        return ResponseEntity.ok(list);
    }

    /**
     * Get order details by ID
     *
     * Flow:
     * 1. Accept numeric orderId as path variable.
     * 2. Delegate to orderService.findOrderResponseById().
     * 3. Return 200 OK if found, 404 Not Found otherwise.
     *
     * @param orderId numeric order ID
     * @return OrderResponse DTO or 404
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long orderId) {
        Optional<OrderResponse> opt = orderService.findOrderResponseById(orderId);
        return opt.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Update order status
     *
     * Flow:
     * 1. Accept JSON body { "status": "SHIPPED", "note": "optional note" }.
     * 2. Validate status is not null/blank and is a valid OrderStatus enum.
     * 3. Delegate to orderService.updateStatusAdmin().
     * 4. Return 200 OK if successful, appropriate error response otherwise.
     *
     * @param orderId order ID
     * @param body    map containing status and optional note
     * @return ResponseEntity with status or error message
     */
    @PostMapping("/{orderId}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long orderId, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        String note = body.getOrDefault("note", null);

        if (status == null || status.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "status is required"));
        }

        try {
            OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid status value"));
        }

        try {
            orderService.updateStatusAdmin(orderId, status, note);
            return ResponseEntity.ok(Map.of("status", "OK"));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * Cancel order by admin
     *
     * Flow:
     * 1. Accept optional reason in request body.
     * 2. Delegate to orderService.cancelOrderByAdmin().
     * 3. Return PlaceOrderResponse DTO.
     *
     * @param orderId order ID
     * @param body    optional reason for cancellation
     * @return PlaceOrderResponse DTO
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<PlaceOrderResponse> cancelOrderByAdmin(
            @PathVariable Long orderId,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        PlaceOrderResponse resp = orderService.cancelOrderByAdmin(orderId, reason);
        return ResponseEntity.ok(resp);
    }

    /**
     * Return order by admin
     *
     * Flow:
     * 1. Accept required reason in request body.
     * 2. Delegate to orderService.returnOrderByAdmin().
     * 3. Return PlaceOrderResponse DTO.
     *
     * @param orderId order ID
     * @param body    map containing reason
     * @return PlaceOrderResponse DTO
     */
    @PostMapping("/{orderId}/return")
    public ResponseEntity<PlaceOrderResponse> returnOrderByAdmin(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        PlaceOrderResponse resp = orderService.returnOrderByAdmin(orderId, reason);
        return ResponseEntity.ok(resp);
    }

    /**
     * Download invoice PDF for an order
     *
     * Flow:
     * 1. Delegate to orderService.getInvoicePdf().
     * 2. If PDF exists, return as application/pdf with attachment headers.
     * 3. Return 404 if not found, 500 on error.
     *
     * @param orderId order ID
     * @return PDF file as byte array or error response
     */
    @GetMapping("/{orderId}/invoice")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long orderId) {
        try {
            byte[] pdf = orderService.getInvoicePdf(orderId);
            if (pdf == null || pdf.length == 0) {
                return ResponseEntity.notFound().build();
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename("invoice-" + orderId + ".pdf")
                    .build());
            headers.setContentLength(pdf.length);
            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
