package com.hyperchip.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WishlistProductDto {
    private Long id;             // wishlist item id
    private Long userId;
    private Long productId;
    private String productName;  // map from ProductDto title
    private Double price;        // map from ProductDto price
    private String imageUrl;     // map from ProductDto image
    private Integer quantity;
}
