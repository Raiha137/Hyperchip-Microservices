package com.hyperchip.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {
    private Long productId;
    private String productTitle;   // full title
    private String productImage;   // image URL or path usable in <img src=...>
    private Integer quantity;
    private Double unitPrice;
    private Double total;

    // New fields to help frontend render availability & enforce rules
    private Integer stock;           // available stock at product service (nullable)
    private Boolean blocked;         // product-level blocked
    private Boolean categoryBlocked; // category blocked
    private Integer availableQty;    // min(stock, MAX_QUANTITY_PER_ITEM)
    private Boolean purchasable;     // computed boolean (true if item can be purchased)
    private Double originalPrice;
    private Double discountedPrice;

}
