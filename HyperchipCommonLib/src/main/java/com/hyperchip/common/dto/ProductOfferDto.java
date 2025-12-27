package com.hyperchip.common.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductOfferDto {
    private Long id;
    private Long productId;
    private OfferDiscountType discountType;
    private BigDecimal discountValue;
    private Boolean active;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
}
