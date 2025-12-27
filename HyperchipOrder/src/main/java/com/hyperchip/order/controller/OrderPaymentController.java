package com.hyperchip.order.controller;

import com.hyperchip.common.dto.AmountResponse;
import com.hyperchip.common.dto.MarkPaidRequest;
import com.hyperchip.order.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * OrderPaymentController
 *
 * Purpose:
 * - Provides REST APIs related to order payments.
 * - Supports querying the payable amount, marking orders as paid, and reporting failed payments.
 *
 * Developer notes:
 * - All business logic is delegated to OrderService.
 * - Responses use standard HTTP status codes.
 * - JSON bodies are used for input and output.
 */
@RestController
@RequestMapping
public class OrderPaymentController {

    private final OrderService orderService;

    public OrderPaymentController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Get the payable amount for a given order.
     *
     * Flow:
     * 1. Accept orderId as path variable.
     * 2. Ask OrderService for the payable amount.
     * 3. Return 200 OK with { "amount": ... } if found, 404 if order not found.
     *
     * @param orderId order ID
     * @return AmountResponse DTO or 404
     */
    @GetMapping("/{orderId}/amount")
    public ResponseEntity<AmountResponse> getOrderAmount(@PathVariable Long orderId) {
        Double amount = orderService.getPayableAmount(orderId);
        if (amount == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(new AmountResponse(amount));
    }

    /**
     * Mark an order as paid.
     *
     * Flow:
     * 1. Accept orderId as path variable and MarkPaidRequest body:
     *      { paymentReference, paymentMethod, amount }
     * 2. Delegate to OrderService.markOrderPaid().
     * 3. Return 200 OK with status "PAID" if successful, 400 with status "FAILED" otherwise.
     *
     * @param orderId order ID
     * @param req     MarkPaidRequest containing payment details
     * @return JSON status response
     */
    @PostMapping("/{orderId}/mark-paid")
    public ResponseEntity<?> markOrderPaid(@PathVariable Long orderId, @RequestBody MarkPaidRequest req) {
        boolean ok = orderService.markOrderPaid(
                orderId,
                req.getPaymentReference(),
                req.getPaymentMethod(),
                req.getAmount()
        );
        if (ok) return ResponseEntity.ok().body(java.util.Map.of("status","PAID"));
        else return ResponseEntity.status(400).body(java.util.Map.of("status","FAILED"));
    }

    /**
     * Mark an order payment as failed.
     *
     * Flow:
     * 1. Accept orderId as path variable and optional JSON body { "reason": ... }.
     * 2. Delegate to OrderService.markOrderPaymentFailed().
     * 3. Return 200 OK with status "FAILED" if successful, 400 with status "NOT_FOUND" if order not found.
     *
     * @param orderId order ID
     * @param body    map containing optional "reason"
     * @return JSON status response
     */
    @PostMapping("/{orderId}/mark-payment-failed")
    public ResponseEntity<?> markPaymentFailed(@PathVariable Long orderId, @RequestBody java.util.Map<String,String> body) {
        String reason = body == null ? null : body.get("reason");
        boolean ok = orderService.markOrderPaymentFailed(orderId, reason);
        if (ok) return ResponseEntity.ok().body(java.util.Map.of("status","FAILED"));
        else return ResponseEntity.status(400).body(java.util.Map.of("status","NOT_FOUND"));
    }
}
