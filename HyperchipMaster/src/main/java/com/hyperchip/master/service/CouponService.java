package com.hyperchip.master.service;

import com.hyperchip.master.model.Coupon;
import com.hyperchip.master.repository.CouponRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service class responsible for managing Coupon-related operations.
 *
 * This service handles:
 * - Creating and updating coupons
 * - Validating coupons based on status and expiry
 * - User-specific coupon validation (referral / assigned coupons)
 * - Fetching active coupons for a user
 *
 * This service is mainly used by:
 * - Referral system
 * - Order pricing logic
 * - Admin coupon management
 */
@Service
public class CouponService {

    @Autowired
    private CouponRepository couponRepository;

    /**
     * Create or update a coupon.
     *
     * This method is used when:
     * - Admin creates a new coupon
     * - Referral system generates coupons automatically
     * - Coupon details need to be updated
     *
     * @param coupon Coupon entity to be saved
     * @return Saved Coupon entity
     */
    public Coupon saveCoupon(Coupon coupon) {
        return couponRepository.save(coupon);
    }

    /**
     * Validate a coupon by its code.
     *
     * Ensures:
     * - Coupon exists
     * - Coupon is active
     * - Coupon is not expired (or has no expiry)
     *
     * This is a basic validation method not tied to a specific user.
     *
     * @param code Coupon code entered by user
     * @return Valid Coupon or null if invalid/expired
     */
    public Coupon getValidCoupon(String code) {
        return couponRepository.findByCode(code)
                .filter(coupon ->
                        coupon.isActive()
                                && (coupon.getExpiryDate() == null
                                || coupon.getExpiryDate().isAfter(LocalDateTime.now()))
                )
                .orElse(null);
    }

    /**
     * Validate a coupon for a specific user.
     *
     * Used for:
     * - Referral coupons
     * - User-specific promotional coupons
     *
     * Ensures:
     * - Coupon is active
     * - Coupon is not expired
     * - Coupon is either unassigned OR assigned to the given user
     *
     * @param code Coupon code
     * @param userId Logged-in user ID
     * @return Valid Coupon or null if invalid for this user
     */
    public Coupon getValidCouponForUser(String code, Long userId) {
        return couponRepository.findByCode(code)
                .filter(coupon ->
                        coupon.isActive()
                                && (coupon.getExpiryDate() == null
                                || coupon.getExpiryDate().isAfter(LocalDateTime.now()))
                                && (coupon.getAssignedUserId() == null
                                || coupon.getAssignedUserId().equals(userId))
                )
                .orElse(null);
    }

    /**
     * Apply a coupon to an order total.
     *
     * NOTE:
     * - This method is not directly used by APIs currently.
     * - Kept for future extensibility or internal usage.
     *
     * Performs:
     * - Coupon validation
     * - Percentage-based discount calculation
     *
     * @param code Coupon code
     * @param orderTotal Original order amount
     * @return Discounted order amount
     * @throws IllegalArgumentException if coupon is invalid or expired
     */
    public double applyCoupon(String code, double orderTotal) {
        Coupon coupon = getValidCoupon(code);

        if (coupon == null) {
            throw new IllegalArgumentException("Invalid or expired coupon");
        }

        double discount = orderTotal * (coupon.getDiscountPercentage() / 100.0);
        return orderTotal - discount;
    }

    /**
     * Fetch all active coupons assigned to a specific user.
     *
     * Used for:
     * - Showing available coupons in user profile
     * - Checkout coupon selection
     *
     * @param userId User ID
     * @return List of active coupons for the user
     */
    public List<Coupon> getCouponsForUser(Long userId) {
        return couponRepository.findByAssignedUserIdAndActiveTrue(userId);
    }

    /**
     * Fetch active coupons assigned to a user.
     *
     * NOTE:
     * - Functionally same as getCouponsForUser()
     * - Kept for naming clarity and backward compatibility
     *
     * @param userId User ID
     * @return List of active coupons
     */
    public List<Coupon> findActiveCouponsForUser(Long userId) {
        return couponRepository.findByAssignedUserIdAndActiveTrue(userId);
    }
}
