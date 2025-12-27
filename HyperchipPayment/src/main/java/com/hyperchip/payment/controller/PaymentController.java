package com.hyperchip.payment.controller;

import com.hyperchip.common.dto.PaymentResponse;
import com.hyperchip.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * PaymentController
 *
 * Handles payment-related HTTP endpoints:
 * - create payment order
 * - verify payment callbacks (multiple variants supported)
 * - handle payment failures
 *
 * Notes:
 * - This controller delegates real work to PaymentService.
 * - Endpoints are defensive: they catch exceptions, log them, and return JSON errors.
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Accepts a generic payload (Map) and forwards to verifyByBody after
     * converting values to strings. Useful when the incoming body has mixed types.
     *
     * POST /api/payments/paymentVerifyAndOrder
     */
    @PostMapping("/paymentVerifyAndOrder")
    public ResponseEntity<?> paymentVerifyAndOrder(@RequestBody Map<String, Object> payload) {
        try {
            // Convert Map<String,Object> -> Map<String,String> to match verifyByBody signature
            Map<String, String> stringPayload = new HashMap<>();
            if (payload != null) {
                payload.forEach((k, v) -> {
                    if (k != null && v != null) {
                        // convert each value toString() so verifyByBody can parse consistently
                        stringPayload.put(k, v.toString());
                    }
                });
            }
            // If verifyByBody is the controller method: call it directly (internal reuse)
            return verifyByBody(stringPayload);
        } catch (Exception e) {
            // Log the full exception for debugging
            log.error("paymentVerifyAndOrder failed", e);
            // Return a simple JSON error to the caller
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "verify failed"));
        }
    }

    /**
     * GET variant that accepts verification fields as query params.
     * Reuses paymentService.verifyPayment for verification logic.
     *
     * GET /api/payments/paymentVerifyAndOrder?orderId=...&razorpay_payment_id=...&razorpay_signature=...
     */
    @GetMapping("/paymentVerifyAndOrder")
    public ResponseEntity<?> paymentVerifyAndOrderGet(@RequestParam Map<String, String> params) {
        try {
            // parse orderId from params if present
            Long orderId = null;
            if (params.get("orderId") != null && !params.get("orderId").isBlank()) {
                orderId = Long.valueOf(params.get("orderId"));
            }

            String paymentId = params.get("razorpay_payment_id");
            String signature = params.get("razorpay_signature");

            if (orderId == null) {
                // Bad request if orderId missing
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "orderId is required"));
            }

            // reuse service verify method
            paymentService.verifyPayment(orderId, paymentId, signature);

            return ResponseEntity.ok(Map.of("success", true, "message", "Payment verified successfully"));
        } catch (Exception e) {
            // Log and return error details (message only)
            log.error("paymentVerifyAndOrderGet failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Payment verification failed", "error", e.getMessage()));
        }
    }

    /**
     * Create a payment order for the given orderId.
     *
     * POST /api/payments/create/{orderId}
     */
    @PostMapping("/create/{orderId}")
    public ResponseEntity<?> createOrder(@PathVariable Long orderId) {
        try {
            PaymentResponse resp = paymentService.createOrder(orderId);
            return ResponseEntity.ok(resp);
        } catch (Exception ex) {
            // Log error and include stack trace in the response for debugging (dev only)
            log.error("createOrder failed for {} : {}", orderId, ex.getMessage(), ex);
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Failed to create order",
                            "error", ex.getMessage(),
                            "stack", sw.toString()
                    ));
        }
    }

    /**
     * Convenience: same create as above, but accepts orderId as request param.
     *
     * POST /api/payments/create?orderId=...
     */
    @PostMapping("/create")
    public ResponseEntity<?> createOrderParam(@RequestParam("orderId") Long orderId) {
        return createOrder(orderId);
    }

    /**
     * Verify payment using payload (body may contain mixed keys).
     * Expects razorpay_payment_id and razorpay_signature (or alternative keys).
     *
     * POST /api/payments/verify/{orderId}
     */
    @PostMapping("/verify/{orderId}")
    public ResponseEntity<?> verify(@PathVariable Long orderId,
                                    @RequestBody(required = false) Map<String, Object> payload) {
        try {
            if (payload == null) payload = Map.of();

            // support multiple key names for flexibility
            String razorpayPaymentId = getString(payload, "razorpay_payment_id", "razorpayPaymentId");
            String razorpaySignature = getString(payload, "razorpay_signature", "razorpaySignature");

            if (razorpayPaymentId == null || razorpaySignature == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Missing payment id or signature"));
            }

            // delegate verification to service
            paymentService.verifyPayment(orderId, razorpayPaymentId, razorpaySignature);
            return ResponseEntity.ok(Map.of("success", true, "status", "OK"));
        } catch (Exception ex) {
            // Log full exception and return error with stack (dev helpful)
            log.error("verify failed for order {} : {}", orderId, ex.getMessage(), ex);
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "verify failed",
                            "error", ex.getMessage(),
                            "stack", sw.toString()
                    ));
        }
    }

    /**
     * Verify payment where body is Map<String,String>.
     * This method is reused internally (e.g., paymentVerifyAndOrder).
     *
     * POST /api/payments/verify
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyByBody(@RequestBody Map<String, String> body) {
        try {
            if (body == null || body.get("orderId") == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "orderId required"));
            }
            Long orderId = Long.parseLong(body.get("orderId"));
            String razorpayPaymentId = body.getOrDefault("razorpayPaymentId", body.get("razorpay_payment_id"));
            String razorpaySignature = body.getOrDefault("razorpaySignature", body.get("razorpay_signature"));

            if (razorpayPaymentId == null || razorpaySignature == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Missing payment id or signature"));
            }

            // delegate to service
            paymentService.verifyPayment(orderId, razorpayPaymentId, razorpaySignature);
            return ResponseEntity.ok(Map.of("success", true, "status", "OK"));
        } catch (Exception ex) {
            log.error("verifyByBody failed: {}", ex.getMessage(), ex);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", ex.getMessage()));
        }
    }

    /**
     * Called when payment fails on client/gateway side.
     * Marks the order as payment-failed and returns confirmation.
     *
     * POST /api/payments/paymentFailureHandler
     */
    @PostMapping("/paymentFailureHandler")
    public ResponseEntity<?> paymentFailureHandler(@RequestBody Map<String, Object> payload) {
        // assume payload contains orderId (will throw if missing)
        Long orderId = Long.valueOf(payload.get("orderId").toString());
        paymentService.markPaymentFailed(orderId, "Payment failed by user");
        return ResponseEntity.ok(Map.of("success", false, "message", "Payment failure recorded"));
    }

    /**
     * Helper: read first non-null value from payload using multiple possible keys.
     *
     * Examples:
     * - getString(payload, "razorpay_payment_id", "razorpayPaymentId")
     */
    private String getString(Map<String, Object> payload, String... keys) {
        for (String k : keys) {
            Object v = payload.get(k);
            if (v != null) return v.toString();
        }
        return null;
    }
}
