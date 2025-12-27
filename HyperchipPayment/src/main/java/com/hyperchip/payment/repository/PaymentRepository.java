package com.hyperchip.payment.repository;

import com.hyperchip.payment.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * PaymentRepository
 *
 * Purpose:
 * - Talks to the payments table
 * - Used to save and fetch payment records
 *
 * Developer Note:
 * - This repository is used only by PaymentService
 * - No business logic should be written here
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Find a payment by orderId
     *
     * Used when:
     * - You know there is only one payment per order
     * - Simple fetch is enough
     */
    Optional<Payment> findByOrderId(Long orderId);

    /**
     * Find the latest payment for an order
     *
     * Used when:
     * - An order can have multiple payment attempts
     * - You want the most recent payment record
     *
     * Example:
     * - First attempt FAILED
     * - Second attempt PAID
     * → this method returns the PAID one
     */
    Optional<Payment> findTopByOrderIdOrderByCreatedAtDesc(Long orderId);

}
