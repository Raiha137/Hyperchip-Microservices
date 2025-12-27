package com.hyperchip.common.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BestOfferRequest {
    private Long productId;
    private Long categoryId;          // nullable if not known
    private BigDecimal originalPrice; // product base price
}
