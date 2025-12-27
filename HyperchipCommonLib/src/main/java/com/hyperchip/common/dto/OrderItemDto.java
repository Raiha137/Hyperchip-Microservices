package com.hyperchip.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for items inside PlaceOrderRequest (user → order service).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemDto {

    private Long productId;
    private String productTitle;
    private String productImage;

    private Integer quantity;
    private Double unitPrice;
    private Double total;

    // Optional extras coming from cart
    private Integer stock;
    private Integer availableQty;
    private Boolean purchasable;
    private Double originalPrice;
    private Double discountedPrice;
}
