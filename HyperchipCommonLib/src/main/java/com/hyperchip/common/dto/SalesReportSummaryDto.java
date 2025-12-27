package com.hyperchip.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesReportSummaryDto {
    private Long totalOrders;
    private BigDecimal totalOrderAmount;     // sum of subTotal
    private BigDecimal totalCouponDiscount;  // sum of coupon discount
    private BigDecimal totalFinalAmount;     // sum of finalAmount
    private BigDecimal overallDiscount;      // totalOrderAmount - totalFinalAmount
}
