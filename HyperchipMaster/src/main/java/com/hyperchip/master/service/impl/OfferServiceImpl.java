package com.hyperchip.master.service.impl;

import com.hyperchip.common.dto.*;
import com.hyperchip.master.model.*;
import com.hyperchip.master.repository.*;
import com.hyperchip.master.service.CouponService;
import com.hyperchip.master.service.OfferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service implementation for managing offers and referral programs.
 * Includes:
 * - Product Offers (create/update/delete/list)
 * - Category Offers (create/update/delete/list)
 * - Best offer calculation for a product
 * - Referral codes and referral rewards handling
 * - Mapping entities to DTOs for API responses
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OfferServiceImpl implements OfferService {

    private final ProductOfferRepository productOfferRepository;
    private final CategoryOfferRepository categoryOfferRepository;
    private final ReferralCodeRepository referralCodeRepository;
    private final ReferralRewardRepository referralRewardRepository;
    private final CouponService couponService;

    // ---------------- PRODUCT OFFER ----------------

    /**
     * Creates a new product offer.
     * Validates discount value, sets active status, and stores start/end dates.
     *
     * @param dto ProductOfferDto containing offer details
     * @return Saved ProductOfferDto
     */
    @Override
    public ProductOfferDto createProductOffer(ProductOfferDto dto) {
        validateOfferValue(dto.getDiscountType(), dto.getDiscountValue());

        ProductOffer offer = ProductOffer.builder()
                .productId(dto.getProductId())
                .discountType(dto.getDiscountType())
                .discountValue(dto.getDiscountValue())
                .active(dto.getActive() != null ? dto.getActive() : Boolean.TRUE)
                .startAt(toInstant(dto.getStartAt()))
                .endAt(toInstant(dto.getEndAt()))
                .build();

        ProductOffer saved = productOfferRepository.save(offer);
        return toProductDto(saved);
    }

    /**
     * Updates an existing product offer by ID.
     * Validates discount value and updates all relevant fields.
     *
     * @param id  ProductOffer ID
     * @param dto ProductOfferDto with updated data
     * @return Updated ProductOfferDto
     */
    @Override
    public ProductOfferDto updateProductOffer(Long id, ProductOfferDto dto) {
        ProductOffer existing = productOfferRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product offer not found"));

        validateOfferValue(dto.getDiscountType(), dto.getDiscountValue());

        existing.setProductId(dto.getProductId());
        existing.setDiscountType(dto.getDiscountType());
        existing.setDiscountValue(dto.getDiscountValue());
        existing.setActive(dto.getActive() != null ? dto.getActive() : existing.getActive());
        existing.setStartAt(toInstant(dto.getStartAt()));
        existing.setEndAt(toInstant(dto.getEndAt()));

        ProductOffer saved = productOfferRepository.save(existing);
        return toProductDto(saved);
    }

    /**
     * Deletes (soft delete) a product offer by ID.
     * Sets the "active" flag to false instead of removing from DB.
     *
     * @param id ProductOffer ID
     */
    @Override
    public void deleteProductOffer(Long id) {
        ProductOffer existing = productOfferRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product offer not found"));

        existing.setActive(false);
        productOfferRepository.save(existing);
    }

    /**
     * Retrieves a product offer by ID.
     *
     * @param id ProductOffer ID
     * @return ProductOfferDto
     */
    @Override
    public ProductOfferDto getProductOffer(Long id) {
        ProductOffer existing = productOfferRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product offer not found"));
        return toProductDto(existing);
    }

    // ---------------- CATEGORY OFFER ----------------

    /**
     * Creates a new category offer.
     * Validates discount value, sets active status, and stores start/end dates.
     *
     * @param dto CategoryOfferDto containing offer details
     * @return Saved CategoryOfferDto
     */
    @Override
    public CategoryOfferDto createCategoryOffer(CategoryOfferDto dto) {
        validateOfferValue(dto.getDiscountType(), dto.getDiscountValue());

        CategoryOffer offer = CategoryOffer.builder()
                .categoryId(dto.getCategoryId())
                .discountType(dto.getDiscountType())
                .discountValue(dto.getDiscountValue())
                .active(dto.getActive() != null ? dto.getActive() : Boolean.TRUE)
                .startAt(toInstant(dto.getStartAt()))
                .endAt(toInstant(dto.getEndAt()))
                .build();

        CategoryOffer saved = categoryOfferRepository.save(offer);
        return toCategoryDto(saved);
    }

    /**
     * Updates an existing category offer by ID.
     *
     * @param id  CategoryOffer ID
     * @param dto CategoryOfferDto with updated values
     * @return Updated CategoryOfferDto
     */
    @Override
    public CategoryOfferDto updateCategoryOffer(Long id, CategoryOfferDto dto) {
        CategoryOffer existing = categoryOfferRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category offer not found"));

        validateOfferValue(dto.getDiscountType(), dto.getDiscountValue());

        existing.setCategoryId(dto.getCategoryId());
        existing.setDiscountType(dto.getDiscountType());
        existing.setDiscountValue(dto.getDiscountValue());
        existing.setActive(dto.getActive() != null ? dto.getActive() : existing.getActive());
        existing.setStartAt(toInstant(dto.getStartAt()));
        existing.setEndAt(toInstant(dto.getEndAt()));

        CategoryOffer saved = categoryOfferRepository.save(existing);
        return toCategoryDto(saved);
    }

    /**
     * Deletes a category offer by ID (hard delete).
     *
     * @param id CategoryOffer ID
     */
    @Override
    public void deleteCategoryOffer(Long id) {
        CategoryOffer existing = categoryOfferRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category offer not found"));

        categoryOfferRepository.delete(existing);
    }

    /**
     * Retrieves a category offer by ID.
     *
     * @param id CategoryOffer ID
     * @return CategoryOfferDto
     */
    @Override
    public CategoryOfferDto getCategoryOffer(Long id) {
        CategoryOffer existing = categoryOfferRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category offer not found"));
        return toCategoryDto(existing);
    }

    // ---------------- BEST OFFER CALCULATION ----------------

    /**
     * Calculates the best applicable discount for a product based on product and category offers.
     *
     * @param productId     Product ID
     * @param categoryId    Category ID
     * @param originalPrice Original price of the product
     * @return BestOfferResponse containing applied discount and final price
     */
    @Override
    public BestOfferResponse calculateBestOffer(Long productId,
                                                Long categoryId,
                                                BigDecimal originalPrice) {
        if (originalPrice == null || originalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Original price must be > 0");
        }

        Instant now = Instant.now();

        ProductOffer bestProductOffer = null;
        CategoryOffer bestCategoryOffer = null;

        // Find best product offer
        if (productId != null) {
            List<ProductOffer> productOffers = productOfferRepository.findByProductIdAndActiveTrue(productId);
            for (ProductOffer po : productOffers) {
                if (isWithinDate(po.getStartAt(), po.getEndAt(), now) &&
                        (bestProductOffer == null || compareDiscount(po, bestProductOffer, originalPrice) > 0)) {
                    bestProductOffer = po;
                }
            }
        }

        // Find best category offer
        if (categoryId != null) {
            List<CategoryOffer> categoryOffers = categoryOfferRepository.findByCategoryIdAndActiveTrue(categoryId);
            for (CategoryOffer co : categoryOffers) {
                if (isWithinDate(co.getStartAt(), co.getEndAt(), now) &&
                        (bestCategoryOffer == null || compareDiscount(co, bestCategoryOffer, originalPrice) > 0)) {
                    bestCategoryOffer = co;
                }
            }
        }

        // Compute discounts
        BigDecimal productDiscount = bestProductOffer != null
                ? calculateDiscount(bestProductOffer.getDiscountType(), bestProductOffer.getDiscountValue(), originalPrice)
                : BigDecimal.ZERO;

        BigDecimal categoryDiscount = bestCategoryOffer != null
                ? calculateDiscount(bestCategoryOffer.getDiscountType(), bestCategoryOffer.getDiscountValue(), originalPrice)
                : BigDecimal.ZERO;

        // Apply highest discount
        OfferScope scope = OfferScope.NONE;
        Long appliedId = null;
        BigDecimal finalDiscount = BigDecimal.ZERO;

        if (productDiscount.compareTo(categoryDiscount) >= 0 && productDiscount.compareTo(BigDecimal.ZERO) > 0) {
            scope = OfferScope.PRODUCT;
            appliedId = bestProductOffer.getId();
            finalDiscount = productDiscount;
        } else if (categoryDiscount.compareTo(BigDecimal.ZERO) > 0) {
            scope = OfferScope.CATEGORY;
            appliedId = bestCategoryOffer.getId();
            finalDiscount = categoryDiscount;
        }

        if (finalDiscount.compareTo(originalPrice) > 0) {
            finalDiscount = originalPrice;
        }

        BigDecimal finalPrice = originalPrice.subtract(finalDiscount);

        return BestOfferResponse.builder()
                .originalPrice(originalPrice.setScale(2, RoundingMode.HALF_UP))
                .discountAmount(finalDiscount.setScale(2, RoundingMode.HALF_UP))
                .finalPrice(finalPrice.setScale(2, RoundingMode.HALF_UP))
                .appliedScope(scope)
                .appliedOfferId(appliedId)
                .build();
    }

    // ---------------- REFERRAL ----------------

    /**
     * Retrieves an existing referral code for a user or creates a new one.
     *
     * @param userId User ID
     * @return ReferralCodeDto
     */
    @Override
    public ReferralCodeDto createOrGetReferralCode(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "UserId is required");
        }

        return referralCodeRepository.findByUserId(userId)
                .map(this::toReferralCodeDto)
                .orElseGet(() -> {
                    String code = "HC" + userId + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
                    ReferralCode rc = ReferralCode.builder()
                            .userId(userId)
                            .code(code)
                            .active(true)
                            .build();
                    return toReferralCodeDto(referralCodeRepository.save(rc));
                });
    }

    /**
     * Registers a referral when a new user signs up with a referral code.
     * Issues coupon to the referrer.
     *
     * @param referralCode Referral code used by new user
     * @param newUserId    New user ID
     * @param newUserEmail New user email
     * @return ReferralRewardDto
     */
    @Override
    public ReferralRewardDto registerReferral(String referralCode,
                                              Long newUserId,
                                              String newUserEmail) {

        if (referralCode == null || referralCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Referral code required");
        }
        if (newUserId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New user id required");
        }

        ReferralCode rc = referralCodeRepository.findByCodeAndActiveTrue(referralCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid referral code"));

        if (rc.getUserId().equals(newUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User cannot refer themselves");
        }

        // Save reward record
        ReferralReward reward = ReferralReward.builder()
                .referrerUserId(rc.getUserId())
                .referredUserId(newUserId)
                .referredUserEmail(newUserEmail)
                .referralCode(referralCode)
                .status("PENDING_REWARD")
                .build();

        ReferralReward saved = referralRewardRepository.save(reward);

        // Issue coupon to referrer
        try {
            Coupon referralCoupon = Coupon.builder()
                    .code("REF-" + saved.getId())
                    .discountPercentage(10.0)
                    .userId(rc.getUserId())
                    .assignedUserId(rc.getUserId())
                    .expiryDate(LocalDateTime.now().plusDays(30))
                    .active(true)
                    .build();

            couponService.saveCoupon(referralCoupon);

            saved.setStatus("COUPON_ISSUED");
            referralRewardRepository.save(saved);
        } catch (Exception ex) {
            log.warn("Failed to issue referral coupon for referrer {}", rc.getUserId(), ex);
        }

        log.info("Referral registered: referrer={}, newUser={}, code={}", rc.getUserId(), newUserId, referralCode);

        return toReferralRewardDto(saved);
    }

    // ---------------- HELPER METHODS ----------------

    private void validateOfferValue(OfferDiscountType type, BigDecimal value) {
        if (type == null || value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Offer type and value required");
        }
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Offer value must be > 0");
        }
    }

    private boolean isWithinDate(Instant start, Instant end, Instant now) {
        if (start != null && now.isBefore(start)) return false;
        if (end != null && now.isAfter(end)) return false;
        return true;
    }

    private BigDecimal calculateDiscount(OfferDiscountType type, BigDecimal value, BigDecimal price) {
        if (type == null || value == null || price == null) return BigDecimal.ZERO;

        switch (type) {
            case PERCENT:
                return price.multiply(value.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            case FLAT:
                return value;
            default:
                return BigDecimal.ZERO;
        }
    }

    private int compareDiscount(ProductOffer a, ProductOffer b, BigDecimal price) {
        return calculateDiscount(a.getDiscountType(), a.getDiscountValue(), price)
                .compareTo(calculateDiscount(b.getDiscountType(), b.getDiscountValue(), price));
    }

    private int compareDiscount(CategoryOffer a, CategoryOffer b, BigDecimal price) {
        return calculateDiscount(a.getDiscountType(), a.getDiscountValue(), price)
                .compareTo(calculateDiscount(b.getDiscountType(), b.getDiscountValue(), price));
    }

    private Instant toInstant(LocalDateTime dt) {
        return dt != null ? dt.toInstant(ZoneOffset.UTC) : null;
    }

    private LocalDateTime toLocalDateTime(Instant i) {
        return i != null ? LocalDateTime.ofInstant(i, ZoneOffset.UTC) : null;
    }

    private ProductOfferDto toProductDto(ProductOffer offer) {
        return ProductOfferDto.builder()
                .id(offer.getId())
                .productId(offer.getProductId())
                .discountType(offer.getDiscountType())
                .discountValue(offer.getDiscountValue())
                .active(offer.getActive())
                .startAt(toLocalDateTime(offer.getStartAt()))
                .endAt(toLocalDateTime(offer.getEndAt()))
                .build();
    }

    private CategoryOfferDto toCategoryDto(CategoryOffer offer) {
        return CategoryOfferDto.builder()
                .id(offer.getId())
                .categoryId(offer.getCategoryId())
                .discountType(offer.getDiscountType())
                .discountValue(offer.getDiscountValue())
                .active(offer.getActive())
                .startAt(toLocalDateTime(offer.getStartAt()))
                .endAt(toLocalDateTime(offer.getEndAt()))
                .build();
    }

    private ReferralCodeDto toReferralCodeDto(ReferralCode rc) {
        return ReferralCodeDto.builder()
                .id(rc.getId())
                .userId(rc.getUserId())
                .code(rc.getCode())
                .active(rc.getActive())
                .createdAt(toLocalDateTime(rc.getCreatedAt()))
                .build();
    }

    private ReferralRewardDto toReferralRewardDto(ReferralReward rr) {
        return ReferralRewardDto.builder()
                .id(rr.getId())
                .referrerUserId(rr.getReferrerUserId())
                .referredUserId(rr.getReferredUserId())
                .referralCode(rr.getReferralCode())
                .status(rr.getStatus())
                .referredUserEmail(rr.getReferredUserEmail())
                .createdAt(toLocalDateTime(rr.getCreatedAt()))
                .build();
    }

    // ---------------- LIST ALL OFFERS & REWARDS ----------------

    @Override
    public List<ProductOfferDto> getAllProductOffers() {
        return productOfferRepository.findAll()
                .stream()
                .map(this::toProductDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CategoryOfferDto> getAllCategoryOffers() {
        return categoryOfferRepository.findAll()
                .stream()
                .map(this::toCategoryDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReferralRewardDto> getRewardsForReferrer(Long referrerId) {
        return referralRewardRepository.findByReferrerUserId(referrerId)
                .stream()
                .map(this::toReferralRewardDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CouponDto> getCouponsForUser(Long userId) {
        return couponService.findActiveCouponsForUser(userId)
                .stream()
                .map(this::toCouponDto)
                .collect(Collectors.toList());
    }

    private CouponDto toCouponDto(Coupon c) {
        if (c == null) return null;

        return CouponDto.builder()
                .id(c.getId())
                .code(c.getCode())
                .description(null)
                .discountType(DiscountType.PERCENT)
                .discountValue(c.getDiscountPercentage() != null
                        ? BigDecimal.valueOf(c.getDiscountPercentage())
                        : null)
                .maxDiscountAmount(null)
                .minOrderAmount(null)
                .usageLimitPerCoupon(null)
                .usageLimitPerUser(null)
                .startAt(null)
                .endAt(c.getExpiryDate())
                .active(c.isActive())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
