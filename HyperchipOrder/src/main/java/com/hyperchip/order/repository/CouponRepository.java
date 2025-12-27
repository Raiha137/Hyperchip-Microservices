package com.hyperchip.order.repository;

import com.hyperchip.order.model.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for managing Coupon entities.
 * Provides CRUD operations and custom query methods.
 */
@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    /**
     * Find a coupon by its unique code.
     *
     * @param code the coupon code
     * @return an Optional containing the Coupon if found, or empty otherwise
     */
    Optional<Coupon> findByCode(String code);
}
