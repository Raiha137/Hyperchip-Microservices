package com.hyperchip.order.controller;

import com.hyperchip.common.dto.ApplyCouponRequest;
import com.hyperchip.common.dto.ApplyCouponResponse;
import com.hyperchip.common.dto.RemoveCouponRequest;
import com.hyperchip.common.dto.RemoveCouponResponse;
import com.hyperchip.order.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user-facing coupon operations.
 * Purpose: Allows users to apply or remove coupons for their orders.
 */
@RestController
@RequestMapping("/api/user/coupons")
@RequiredArgsConstructor
public class UserCouponController {

    private final CouponService couponService;

    // ====================================================
    // APPLY COUPON
    // ====================================================
    /**
     * Purpose: Applies a coupon to a user's current order.
     * Input: coupon code and order details.
     * Output: updated order totals and discount info.
     */
    @PostMapping("/apply")
    public ResponseEntity<ApplyCouponResponse> applyCoupon(@RequestBody ApplyCouponRequest request) {
        ApplyCouponResponse response = couponService.applyCoupon(request);
        return ResponseEntity.ok(response);
    }

    // ====================================================
    // REMOVE COUPON
    // ====================================================
    /**
     * Purpose: Removes a previously applied coupon from a user's order.
     * Input: coupon code and order details.
     * Output: updated order totals without discount.
     */
    @PostMapping("/remove")
    public ResponseEntity<RemoveCouponResponse> removeCoupon(@RequestBody RemoveCouponRequest request) {
        RemoveCouponResponse response = couponService.removeCoupon(request);
        return ResponseEntity.ok(response);
    }
}
