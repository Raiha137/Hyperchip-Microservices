package com.hyperchip.payment.service;

import com.hyperchip.common.dto.PaymentResponse;

/**
 * PaymentService
 *
 * Defines core payment operations used by controllers.
 * Implementations handle communication with payment gateway,
 * persist payment records, and notify the Order service.
 */
public interface PaymentService {

    /**
     * Create a payment order for the given application orderId.
     *
     * Flow (simple):
     * 1. Read order details from Order Service
     * 2. Create gateway order (Razorpay)
     * 3. Save Payment record in DB (PENDING)
     * 4. Return data frontend needs to start checkout
     *
     * @param orderId application order id
     * @return PaymentResponse with gateway/order details
     * @throws Exception when creation or external calls fail
     */
    PaymentResponse createOrder(Long orderId) throws Exception;

    /**
     * Verify a gateway payment and finalize the order.
     *
     * Flow (simple):
     * 1. Validate inputs
     * 2. Find latest Payment row for order
     * 3. (Optionally) verify signature with gateway
     * 4. Notify Order Service (mark-paid)
     * 5. Update Payment.status -> PAID
     *
     * @param orderId application order id
     * @param paymentId gateway payment id
     * @param signature gateway signature for verification
     * @throws Exception when verification fails or external calls fail
     */
    void verifyPayment(Long orderId, String paymentId, String signature) throws Exception;

    /**
     * Mark a payment as failed and notify the order service.
     *
     * Common use:
     * - user cancelled payment
     * - gateway returned failure
     *
     * @param orderId application order id
     * @param reason short reason text
     */
    void markPaymentFailed(Long orderId, String reason);
}
