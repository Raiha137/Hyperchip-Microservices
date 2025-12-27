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
public class BestSellerBrandDto {

    private Long brandId;
    private String brandName;

    private Long totalQuantity;
    private BigDecimal totalAmount;
}
