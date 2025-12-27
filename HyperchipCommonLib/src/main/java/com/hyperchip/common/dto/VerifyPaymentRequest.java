    package com.hyperchip.common.dto;

    import lombok.Data;

    @Data
    public class VerifyPaymentRequest {
        private Long appOrderId;
        private String razorpayOrderId;
        private String razorpayPaymentId;
        private String razorpaySignature;
    }
