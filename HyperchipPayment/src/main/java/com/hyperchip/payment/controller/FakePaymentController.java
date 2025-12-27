package com.hyperchip.payment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * FakePaymentController
 *
 * Simple fake payment endpoints used for testing and local development.
 * - createFake: returns a fake payment order (use to simulate creating a payment)
 * - verifyFake: returns a fake verification response (use to simulate payment callback)
 *
 * IMPORTANT: This controller is only for testing. Do NOT use in production.
 */
@RestController
@RequestMapping("/api/payment")   // IMPORTANT: no 's' here
public class FakePaymentController {

    // === FAKE CREATE ===
    /**
     * Fake create endpoint.
     *
     * Returns a small map with fake payment details so you can test payment flows.
     * Example: call this to get a fake order before calling verify.
     *
     * @param orderId order id to attach to the fake payment
     * @return map with fake=true, orderId, amount, currency
     */
    @GetMapping("/create/{orderId}")
    public ResponseEntity<Map<String, Object>> createFake(@PathVariable Long orderId) {
        Map<String, Object> res = new HashMap<>();
        // mark this response as fake (for client/UI to detect)
        res.put("fake", true);
        // attach order id so caller can relate response to order
        res.put("orderId", orderId);
        // small fixed amount for testing
        res.put("amount", 1);
        // currency code
        res.put("currency", "INR");
        return ResponseEntity.ok(res);
    }

    // === FAKE VERIFY ===
    /**
     * Fake verify endpoint.
     *
     * Simulates a payment verification/callback.
     * Returns success=true and the same orderId to confirm payment.
     *
     * @param orderId the order being "verified"
     * @param payload optional request body (ignored by this fake)
     * @return map with success=true and orderId
     */
    @PostMapping("/verify/{orderId}")
    public ResponseEntity<Map<String, Object>> verifyFake(@PathVariable Long orderId,
                                                          @RequestBody(required = false) Map<String, Object> payload) {
        Map<String, Object> res = new HashMap<>();
        // always return success for this fake endpoint
        res.put("success", true);
        // echo back order id for correlation
        res.put("orderId", orderId);
        return ResponseEntity.ok(res);
    }
}
