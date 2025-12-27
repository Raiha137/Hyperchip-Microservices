package com.hyperchip.payment.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Payment Entity
 *
 * Purpose:
 * - Stores payment details for an order
 * - Used to track payment status (created / paid / failed)
 *
 * Developer Note:
 * - This table is written by PaymentService
 * - It is NOT responsible for wallet logic or order logic
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    /**
     * Primary key for payments table
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Order ID from Order Service
     * Used to connect payment with an order
     */
    private Long orderId;

    /**
     * Order ID returned by Razorpay
     * Used for verification
     */
    private String razorpayOrderId;

    /**
     * Payment ID returned by Razorpay after payment success
     */
    private String razorpayPaymentId;

    /**
     * Payment status
     * Examples:
     * - CREATED
     * - PAID
     * - FAILED
     */
    private String status;

    /**
     * Time when payment record was created
     * Automatically set when object is created
     */
    private Instant createdAt = Instant.now();

    /**
     * Payment amount
     */
    private BigDecimal amount;

    /**
     * Currency code (example: INR)
     */
    private String currency;
}
