package com.hyperchip.order.controller;

import com.hyperchip.common.dto.*;
import com.hyperchip.order.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AdminCouponController
 *
 * Purpose:
 * - Exposes REST endpoints for administrative operations on coupons.
 * - Supports creation, deletion, and retrieval of coupon details.
 *
 * Developer notes:
 * - All operations are restricted to admin usage (security assumed handled elsewhere).
 * - Uses CouponService to encapsulate business logic; controller handles only HTTP requests/responses.
 * - DTOs (CreateCouponRequest, CreateCouponResponse, CouponDto) are used to decouple API from internal models.
 */
@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
public class AdminCouponController {

    private final CouponService couponService;

    /**
     * Create a new coupon
     *
     * Flow:
     * 1. Receive a CreateCouponRequest from the frontend
     * 2. Call the service layer to create the coupon
     * 3. Return the created coupon details as CreateCouponResponse
     *
     * @param request DTO containing coupon details (code, discount, expiry, etc.)
     * @return HTTP 200 OK with CreateCouponResponse payload
     */
    @PostMapping
    public ResponseEntity<CreateCouponResponse> createCoupon(@RequestBody CreateCouponRequest request) {
        CreateCouponResponse response = couponService.createCoupon(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete an existing coupon by ID
     *
     * Flow:
     * 1. Receive the coupon ID from the path
     * 2. Delegate deletion to the service layer
     * 3. Return HTTP 204 No Content on success
     *
     * @param id coupon ID to delete
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCoupon(@PathVariable Long id) {
        couponService.deleteCoupon(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get coupon details by coupon code
     *
     * Flow:
     * 1. Receive coupon code from the path
     * 2. Fetch coupon details using the service layer
     * 3. Return coupon info as CouponDto
     *
     * @param code coupon code string
     * @return HTTP 200 OK with CouponDto payload
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<CouponDto> getCouponByCode(@PathVariable String code) {
        CouponDto coupon = couponService.getCouponByCode(code);
        return ResponseEntity.ok(coupon);
    }
}
