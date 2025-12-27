package com.hyperchip.order.service;

import com.hyperchip.common.dto.*;

/**
 * Service interface for Coupon operations in Hyperchip e-commerce.
 *
 * <p>This interface defines the contract for managing coupons, including:</p>
 * <ul>
 *     <li>Creating new coupons</li>
 *     <li>Retrieving coupon details by code</li>
 *     <li>Deleting coupons</li>
 *     <li>Applying coupons to orders</li>
 *     <li>Removing applied coupons from orders</li>
 * </ul>
 */
public interface CouponService {

    /**
     * Create a new coupon based on the provided request data.
     *
     * @param request the data required to create a coupon
     * @return the response containing coupon details such as code, discount, and validity
     */
    CreateCouponResponse createCoupon(CreateCouponRequest request);

    /**
     * Retrieve a coupon by its unique code.
     *
     * @param code the unique coupon code
     * @return the coupon details as a DTO, or null if not found
     */
    CouponDto getCouponByCode(String code);

    /**
     * Delete a coupon by its ID.
     *
     * @param id the unique identifier of the coupon
     */
    void deleteCoupon(Long id);

    /**
     * Apply a coupon to an order.
     *
     * @param request the request containing order and coupon information
     * @return the response indicating success, discount applied, and updated totals
     */
    ApplyCouponResponse applyCoupon(ApplyCouponRequest request);

    /**
     * Remove an applied coupon from an order.
     *
     * @param request the request containing order and coupon information
     * @return the response indicating success and updated totals after removal
     */
    RemoveCouponResponse removeCoupon(RemoveCouponRequest request);
}
