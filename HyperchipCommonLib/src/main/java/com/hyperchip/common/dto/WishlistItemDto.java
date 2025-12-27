// src/main/java/com/hyperchip/common/dto/WishListItemDto.java
package com.hyperchip.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO sent to frontend for rendering wishlist items
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistItemDto {
    private Long productId;       // Product ID
    private String productTitle;  // Product name/title for display
    private Double unitPrice;     // Product price
    private String productImage;  // Product image URL
    private Integer quantity;
}
