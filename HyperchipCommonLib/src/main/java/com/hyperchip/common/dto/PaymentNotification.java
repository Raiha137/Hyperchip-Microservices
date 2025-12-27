package com.hyperchip.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload used to verify a Razorpay-like payment success.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentNotification {
    // provider (Razorpay) order id
    private String providerOrderId;

    // provider (Razorpay) payment id
    private String providerPaymentId;

    // provider signature
    private String signature;

    // app order id (optional)
    private Long appOrderId;
}
