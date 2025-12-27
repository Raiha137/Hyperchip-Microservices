package com.hyperchip.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesReportRowDto {
    private Long orderId;
    private String orderNumber;
    private LocalDateTime orderDate;

    private Long userId;
    private String userEmail;

    private String paymentMethod;
    private String paymentStatus;
    private String orderStatus;

    // money
    private BigDecimal subTotal;        // before coupon
    private BigDecimal couponDiscount;  // coupon only
    private BigDecimal finalAmount;     // after coupon (totalAmount)
}
