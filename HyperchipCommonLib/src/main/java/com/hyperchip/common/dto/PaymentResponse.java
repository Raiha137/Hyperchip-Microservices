package com.hyperchip.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response sent from the payment service to the user-service/frontend.
 * Contains both general status information and Razorpay-specific fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

    // generic status
    private boolean success;
    private String message;

    // which provider was used (RAZORPAY, PAYPAL, etc.)
    private String provider;

    // app order identifiers
    private Long orderId;
    private String orderNumber;

    // Razorpay-specific fields used by checkout.js
    private String razorpayKey;      // key_id
    private String razorpayOrderId;  // razorpay order id

    // amounts
    private Double amount;          // e.g. 392.18
    private Long amountMinor;       // e.g. 39218

    // currency (INR, AED, USD, etc.)
    private String currency;

    // test/fake mode flag
    private boolean fake;

    // optional fields if you need them
    private String receipt;
    private Map<String, Object> prefill;
}
