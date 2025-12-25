package com.hyperchip.master.controller;

import com.hyperchip.master.model.Coupon;
import com.hyperchip.master.repository.CouponRepository;
import com.hyperchip.master.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CouponController
 *
 * Purpose:
 * - Exposes REST APIs for managing and applying coupons in the system.
 * - Includes:
 *   1) Listing user-specific coupons ("My Coupons" page)
 *   2) Validating coupon codes
 *   3) Applying coupons at checkout
 */
@RestController
@RequestMapping("/api/coupons") // Base path for coupon APIs
@RequiredArgsConstructor
public class CouponController {

    private final CouponRepository couponRepository; // For direct DB queries related to coupons
    private final CouponService couponService;       // Business logic for coupon validation & application

    // ===========================
    // 1) GET USER COUPONS
    // ===========================
    /**
     * GET /api/coupons/user/{userId}
     *
     * Purpose:
     * - Retrieve all active coupons assigned to a specific user.
     * - Typically used for the "My Coupons" page in the UI.
     *
     * @param userId ID of the user
     * @return List of active coupons assigned to the user
     */
    @GetMapping("/user/{userId}")
    public List<Coupon> getCouponsForUser(@PathVariable Long userId) {
        return couponRepository.findByAssignedUserIdAndActiveTrue(userId);
    }

    // ===========================
    // 2) VALIDATE COUPON BY CODE
    // ===========================
    /**
     * GET /api/coupons/validate/{code}
     *
     * Purpose:
     * - Check if a coupon code is valid (exists and active).
     * - This is a simple validation endpoint and is not necessarily used by checkout JS.
     *
     * @param code Coupon code to validate
     * @return Coupon object if valid, or 400 Bad Request if invalid
     */
    @GetMapping("/validate/{code}")
    public ResponseEntity<Coupon> validateCoupon(@PathVariable String code) {
        Coupon c = couponService.getValidCoupon(code);
        if (c == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(c);
    }

    // ===========================
    // 3) APPLY COUPON AT CHECKOUT
    // ===========================
    /**
     * POST /api/coupons/apply
     *
     * Purpose:
     * - Apply a coupon to a user's order during checkout.
     * - Returns success/failure, discount amount, new total, and other metadata.
     *
     * @param body JSON payload containing:
     *             - code: Coupon code (required)
     *             - orderTotal: Total order amount (required)
     *             - userId: Optional user ID
     * @return Map with result: success, discountAmount, newTotal, code, userId
     */
    @PostMapping("/apply")
    public ResponseEntity<Map<String, Object>> applyCoupon(@RequestBody Map<String, Object> body) {

        Map<String, Object> res = new HashMap<>();

        // 1) Read and sanitize coupon code
        String rawCode = String.valueOf(body.getOrDefault("code", "")).trim();
        if (rawCode.isEmpty()) {
            res.put("success", false);
            res.put("message", "Coupon code is required");
            return ResponseEntity.badRequest().body(res);
        }
        String code = rawCode.toUpperCase(); // Normalize code to uppercase

        // 2) Read and validate orderTotal safely
        double orderTotal;
        try {
            orderTotal = Double.parseDouble(String.valueOf(body.getOrDefault("orderTotal", "0")));
        } catch (NumberFormatException ex) {
            res.put("success", false);
            res.put("message", "Invalid order amount");
            return ResponseEntity.badRequest().body(res);
        }

        // 3) Optional userId handling – don't fail if missing
        Object userIdObj = body.get("userId");
        Long userId = null;
        if (userIdObj != null && !userIdObj.toString().isBlank()) {
            try {
                userId = Long.valueOf(userIdObj.toString());
            } catch (NumberFormatException ignored) {
            }
        }

        // 4) Validate coupon existence and status
        Coupon coupon = couponService.getValidCoupon(code);
        if (coupon == null) {
            res.put("success", false);
            res.put("message", "Invalid or expired coupon");
            return ResponseEntity.ok(res);
        }

        // 5) Calculate discount and new total
        double discount = orderTotal * (coupon.getDiscountPercentage() / 100.0);
        double newTotal = Math.max(orderTotal - discount, 0);

        // 6) Prepare response
        res.put("success", true);
        res.put("discountAmount", discount);
        res.put("newTotal", newTotal);
        res.put("code", code);
        res.put("userId", userId);

        return ResponseEntity.ok(res);
    }
}
