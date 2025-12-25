package com.hyperchip.master.repository;

import com.hyperchip.master.model.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Coupon entity.
 * Extends JpaRepository to provide CRUD operations and custom queries.
 */
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    /**
     * Finds a coupon by its unique code.
     *
     * @param code Coupon code
     * @return Optional containing the Coupon if found, or empty if not found
     */
    Optional<Coupon> findByCode(String code);

    /**
     * Retrieves all active coupons assigned to a specific user.
     *
     * @param assignedUserId ID of the assigned user
     * @return List of active Coupon entities for the user
     */
    List<Coupon> findByAssignedUserIdAndActiveTrue(Long assignedUserId);

    /**
     * Retrieves all coupons (active or inactive) assigned to a specific user.
     *
     * @param assignedUserId ID of the assigned user
     * @return List of Coupon entities for the user
     */
    List<Coupon> findByAssignedUserId(Long assignedUserId);
}
