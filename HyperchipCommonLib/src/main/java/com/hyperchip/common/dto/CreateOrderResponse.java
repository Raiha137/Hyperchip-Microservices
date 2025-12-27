package com.hyperchip.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * DTO returned by PaymentService.createRazorpayOrder(...)
 * Uses Lombok to reduce boilerplate and includes a raw map field for gateway response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Razorpay order id (eg. "order_XXXX") */
    private String razorpayOrderId;

    /** Amount in smallest unit (e.g., fils/paise) */
    private Long amount;

    /** Currency code, e.g. "AED" */
    private String currency;

    /** Public key id to expose to frontend (rzp_xxx) */
    private String keyId;

    /** Raw response from gateway (optional) */
    private Map<String, Object> raw;
}
