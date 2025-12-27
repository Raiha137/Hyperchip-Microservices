package com.hyperchip.common.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BestOfferResponse {
    private BigDecimal originalPrice;
    private BigDecimal discountAmount;
    private BigDecimal finalPrice;

    private OfferScope appliedScope;  // NONE, PRODUCT, CATEGORY
    private Long appliedOfferId;      // id of product/category offer, if applied
}
