package com.hyperchip.master.service;

import com.hyperchip.common.dto.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * OfferService
 *
 * This service defines all business operations related to:
 * - Product-level offers
 * - Category-level offers
 * - Best-offer price calculation
 * - Referral codes and referral rewards
 * - Coupons issued through referrals
 *
 * It acts as the central contract between controllers and
 * the OfferServiceImpl business logic.
 */
public interface OfferService {

    // ========================= PRODUCT OFFERS =========================

    /**
     * Create a new product-specific offer.
     *
     * Used by:
     * - Admin panel to configure discounts for a specific product
     *
     * @param dto ProductOfferDto containing productId, discount type, value, and validity
     * @return Created ProductOfferDto
     */
    ProductOfferDto createProductOffer(ProductOfferDto dto);

    /**
     * Update an existing product offer.
     *
     * Used by:
     * - Admin panel when editing an existing product offer
     *
     * @param id  ID of the product offer
     * @param dto Updated product offer details
     * @return Updated ProductOfferDto
     */
    ProductOfferDto updateProductOffer(Long id, ProductOfferDto dto);

    /**
     * Delete (deactivate) a product offer.
     *
     * Used by:
     * - Admin panel to disable a product offer
     *
     * @param id Product offer ID
     */
    void deleteProductOffer(Long id);

    /**
     * Fetch a product offer by ID.
     *
     * Used by:
     * - Admin UI to view/edit offer details
     *
     * @param id Product offer ID
     * @return ProductOfferDto
     */
    ProductOfferDto getProductOffer(Long id);


    // ========================= CATEGORY OFFERS =========================

    /**
     * Create a new category-level offer.
     *
     * Used by:
     * - Admin panel to apply discounts to all products under a category
     *
     * @param dto CategoryOfferDto containing categoryId and discount details
     * @return Created CategoryOfferDto
     */
    CategoryOfferDto createCategoryOffer(CategoryOfferDto dto);

    /**
     * Update an existing category offer.
     *
     * Used by:
     * - Admin panel when editing category-wide discounts
     *
     * @param id  Category offer ID
     * @param dto Updated category offer details
     * @return Updated CategoryOfferDto
     */
    CategoryOfferDto updateCategoryOffer(Long id, CategoryOfferDto dto);

    /**
     * Delete a category offer.
     *
     * Used by:
     * - Admin panel to remove category-level discounts
     *
     * @param id Category offer ID
     */
    void deleteCategoryOffer(Long id);

    /**
     * Fetch a category offer by ID.
     *
     * Used by:
     * - Admin UI for viewing/editing category offers
     *
     * @param id Category offer ID
     * @return CategoryOfferDto
     */
    CategoryOfferDto getCategoryOffer(Long id);


    // ========================= BEST OFFER CALCULATION =========================

    /**
     * Calculate the best possible offer for a product.
     *
     * Business rules:
     * - Considers both product-level and category-level offers
     * - Applies only active and date-valid offers
     * - Chooses the offer that gives the maximum discount
     *
     * Used by:
     * - Product listing page
     * - Product detail page
     * - Cart and checkout pricing logic
     *
     * @param productId     Product ID (nullable)
     * @param categoryId    Category ID (nullable)
     * @param originalPrice Original product price
     * @return BestOfferResponse containing discount and final price
     */
    BestOfferResponse calculateBestOffer(Long productId,
                                         Long categoryId,
                                         BigDecimal originalPrice);


    // ========================= REFERRAL SYSTEM =========================

    /**
     * Create or retrieve a referral code for a user.
     *
     * Business logic:
     * - Each user has only one referral code
     * - If already exists, return existing code
     * - Otherwise, generate a new unique code
     *
     * Used by:
     * - User profile page
     * - Referral sharing feature
     *
     * @param userId User ID
     * @return ReferralCodeDto
     */
    ReferralCodeDto createOrGetReferralCode(Long userId);

    /**
     * Register a referral when a new user signs up using a referral code.
     *
     * Business logic:
     * - Validates referral code
     * - Prevents self-referral
     * - Records referral reward entry
     * - Issues a coupon to the referrer
     *
     * Used by:
     * - User registration flow
     *
     * @param referralCode Referral code used
     * @param newUserId    Newly registered user ID
     * @param newUserEmail Newly registered user email
     * @return ReferralRewardDto
     */
    ReferralRewardDto registerReferral(String referralCode,
                                       Long newUserId,
                                       String newUserEmail);


    // ========================= LISTING & LOOKUPS =========================

    /**
     * Fetch all product offers.
     *
     * Used by:
     * - Admin offer management screens
     *
     * @return List of ProductOfferDto
     */
    List<ProductOfferDto> getAllProductOffers();

    /**
     * Fetch all category offers.
     *
     * Used by:
     * - Admin offer management screens
     *
     * @return List of CategoryOfferDto
     */
    List<CategoryOfferDto> getAllCategoryOffers();

    /**
     * Fetch all referral rewards earned by a referrer.
     *
     * Used by:
     * - Admin referral tracking
     * - User referral reward history
     *
     * @param referrerId Referrer user ID
     * @return List of ReferralRewardDto
     */
    List<ReferralRewardDto> getRewardsForReferrer(Long referrerId);

    /**
     * Fetch all active coupons available for a user.
     *
     * Used by:
     * - User profile
     * - Checkout coupon selection
     *
     * @param userId User ID
     * @return List of CouponDto
     */
    List<CouponDto> getCouponsForUser(Long userId);
}
