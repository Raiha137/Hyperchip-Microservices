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
public class BestSellerProductDto {

    private Long productId;
    private String productName;
    /** Total quantity sold in selected period */
    private Long totalQuantity;

    /** Total sales amount for this product in selected period */
    private BigDecimal totalAmount;
}
