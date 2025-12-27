package com.hyperchip.order.service.impl;

import com.hyperchip.common.dto.*;
import com.hyperchip.order.model.Coupon;
import com.hyperchip.order.model.CouponUsage;
import com.hyperchip.order.model.Order;
import com.hyperchip.order.repository.CouponRepository;
import com.hyperchip.order.repository.CouponUsageRepository;
import com.hyperchip.order.repository.OrderRepository;
import com.hyperchip.order.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * Implementation of CouponService.
 *
 * This service is responsible for:
 * - Creating coupons
 * - Retrieving coupon details
 * - Applying coupons to orders
 * - Removing applied coupons
 *
 * All business rules and persistence logic are encapsulated here.
 */
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final OrderRepository orderRepository;

    // ======================================================
    // CREATE COUPON
    // ======================================================
    /**
     * Create a new coupon in the system.
     *
     * Validates the request for required fields and uniqueness.
     *
     * @param request Coupon creation request containing all coupon details.
     * @return Response DTO with created coupon details.
     */
    @Override
    @Transactional
    public CreateCouponResponse createCoupon(CreateCouponRequest request) {
        // Validation
        if (request == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request cannot be null");
        if (request.getCode() == null || request.getCode().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon code is required");
        if (request.getDiscountType() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Discount type is required");
        if (request.getDiscountValue() == null || request.getDiscountValue().doubleValue() <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Discount value must be greater than zero");

        // Ensure coupon code is unique
        if (couponRepository.findByCode(request.getCode()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Coupon code already exists");
        }

        // Build and save the coupon
        Coupon coupon = Coupon.builder()
                .code(request.getCode().trim())
                .description(request.getDescription())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .minOrderAmount(request.getMinOrderAmount())
                .startAt(request.getStartAt() != null ? request.getStartAt().toInstant(ZoneOffset.UTC) : null)
                .endAt(request.getEndAt() != null ? request.getEndAt().toInstant(ZoneOffset.UTC) : null)
                .usageLimitPerCoupon(request.getUsageLimitPerCoupon())
                .usageLimitPerUser(request.getUsageLimitPerUser())
                .active(request.getActive() != null ? request.getActive() : Boolean.TRUE)
                .build();

        Coupon saved = couponRepository.save(coupon);

        // Return response
        return CreateCouponResponse.builder()
                .id(saved.getId())
                .code(saved.getCode())
                .message("Coupon created successfully")
                .createdAt(LocalDateTime.ofInstant(saved.getCreatedAt(), ZoneOffset.UTC))
                .updatedAt(LocalDateTime.ofInstant(saved.getUpdatedAt(), ZoneOffset.UTC))
                .build();
    }

    // ======================================================
    // GET COUPON BY CODE
    // ======================================================
    /**
     * Retrieve a coupon by its code.
     *
     * @param code Coupon code.
     * @return Coupon DTO if found.
     */
    @Override
    @Transactional(readOnly = true)
    public CouponDto getCouponByCode(String code) {
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coupon not found"));
        return toDto(coupon);
    }

    // ======================================================
    // DELETE COUPON
    // ======================================================
    /**
     * Soft delete a coupon by marking it inactive.
     *
     * @param id Coupon ID.
     */
    @Override
    @Transactional
    public void deleteCoupon(Long id) {
        if (id == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon ID is required");

        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coupon not found"));

        // Mark coupon inactive instead of hard delete
        coupon.setActive(false);
        couponRepository.save(coupon);
    }

    // ======================================================
    // APPLY COUPON TO ORDER
    // ======================================================
    /**
     * Apply a coupon to an order.
     *
     * Validates order existence, coupon validity, usage limits,
     * minimum order amount, and then calculates the discount.
     *
     * @param request Coupon application request containing orderId, userId, and coupon code.
     * @return Response DTO with discount details and new total.
     */
    @Override
    @Transactional
    public ApplyCouponResponse applyCoupon(ApplyCouponRequest request) {
        // Validate input
        if (request == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request cannot be null");
        Long orderId = request.getOrderId();
        Long userId = request.getUserId();
        String code = request.getCouponCode();
        if (orderId == null || userId == null || code == null || code.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order ID, user ID, and coupon code are required");

        // Fetch order and coupon
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coupon not found"));

        Instant now = Instant.now();

        // Validate coupon status, dates, min order, and usage limits
        validateCoupon(order, coupon, userId, now);

        // Calculate discount
        BigDecimal orderTotal = BigDecimal.valueOf(order.getTotalAmount() != null ? order.getTotalAmount() : order.getSubtotal());
        BigDecimal discount = calculateDiscount(coupon, orderTotal);

        // Update order total
        BigDecimal newTotal = orderTotal.subtract(discount);
        order.setTotalAmount(newTotal.doubleValue());
        orderRepository.save(order);

        // Record coupon usage
        CouponUsage usage = CouponUsage.builder()
                .coupon(coupon)
                .orderId(orderId)
                .userId(userId)
                .discountAmount(discount)
                .usedAt(now)
                .build();
        couponUsageRepository.save(usage);

        return ApplyCouponResponse.builder()
                .success(true)
                .message("Coupon applied successfully")
                .orderId(orderId)
                .couponCode(code)
                .discountAmount(discount)
                .totalBeforeDiscount(orderTotal)
                .totalAfterDiscount(newTotal)
                .appliedAt(LocalDateTime.ofInstant(now, ZoneOffset.UTC))
                .build();
    }

    private void validateCoupon(Order order, Coupon coupon, Long userId, Instant now) {
        if (coupon.getActive() == null || !coupon.getActive()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon is inactive");
        if (coupon.getStartAt() != null && now.isBefore(coupon.getStartAt())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon is not yet valid");
        if (coupon.getEndAt() != null && now.isAfter(coupon.getEndAt())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon has expired");

        BigDecimal orderTotal = BigDecimal.valueOf(order.getTotalAmount() != null ? order.getTotalAmount() : order.getSubtotal());
        if (coupon.getMinOrderAmount() != null && orderTotal.compareTo(coupon.getMinOrderAmount()) < 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order amount is below minimum required");

        if (couponUsageRepository.existsByOrderId(order.getId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A coupon has already been applied to this order");

        if (coupon.getUsageLimitPerCoupon() != null && couponUsageRepository.countByCouponId(coupon.getId()) >= coupon.getUsageLimitPerCoupon())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This coupon has reached its overall usage limit");

        if (coupon.getUsageLimitPerUser() != null && couponUsageRepository.countByCouponIdAndUserId(coupon.getId(), userId) >= coupon.getUsageLimitPerUser())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You have reached the usage limit for this coupon");
    }

    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal orderTotal) {
        BigDecimal discount;
        if (coupon.getDiscountType() == DiscountType.PERCENT) {
            discount = orderTotal.multiply(coupon.getDiscountValue().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            if (coupon.getMaxDiscountAmount() != null) discount = discount.min(coupon.getMaxDiscountAmount());
        } else { // FLAT
            discount = coupon.getDiscountValue();
        }
        return discount.min(orderTotal);
    }

    // ======================================================
    // REMOVE COUPON FROM ORDER
    // ======================================================
    /**
     * Remove a previously applied coupon from an order.
     *
     * Restores the order total and deletes the coupon usage record.
     *
     * @param request Remove coupon request containing orderId and userId.
     * @return Response DTO with restored total.
     */
    @Override
    @Transactional
    public RemoveCouponResponse removeCoupon(RemoveCouponRequest request) {
        if (request == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request cannot be null");
        Long orderId = request.getOrderId();
        Long userId = request.getUserId();
        if (orderId == null || userId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order ID and user ID are required");

        CouponUsage usage = couponUsageRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No coupon applied to this order"));
        if (!usage.getUserId().equals(userId)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You did not apply this coupon");

        BigDecimal discount = usage.getDiscountAmount();
        couponUsageRepository.delete(usage);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        BigDecimal restoredTotal = BigDecimal.valueOf(order.getTotalAmount() != null ? order.getTotalAmount() : 0.0).add(discount);
        order.setTotalAmount(restoredTotal.doubleValue());
        orderRepository.save(order);

        return RemoveCouponResponse.builder()
                .success(true)
                .message("Coupon removed successfully")
                .orderId(orderId)
                .totalBeforeDiscount(BigDecimal.valueOf(order.getTotalAmount()))
                .totalAfterDiscount(restoredTotal)
                .build();
    }

    // ======================================================
    // HELPER: CONVERT ENTITY TO DTO
    // ======================================================
    /** Converts a Coupon entity to its DTO representation. */
    private CouponDto toDto(Coupon coupon) {
        if (coupon == null) return null;
        return CouponDto.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .description(coupon.getDescription())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .minOrderAmount(coupon.getMinOrderAmount())
                .startAt(coupon.getStartAt() != null ? LocalDateTime.ofInstant(coupon.getStartAt(), ZoneOffset.UTC) : null)
                .endAt(coupon.getEndAt() != null ? LocalDateTime.ofInstant(coupon.getEndAt(), ZoneOffset.UTC) : null)
                .usageLimitPerCoupon(coupon.getUsageLimitPerCoupon())
                .usageLimitPerUser(coupon.getUsageLimitPerUser())
                .active(coupon.getActive())
                .createdAt(LocalDateTime.ofInstant(coupon.getCreatedAt(), ZoneOffset.UTC))
                .updatedAt(LocalDateTime.ofInstant(coupon.getUpdatedAt(), ZoneOffset.UTC))
                .build();
    }
}
