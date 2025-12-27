    package com.hyperchip.common.dto;

    import lombok.Data;

    import java.util.List;

    @Data
    public class CartProductDto {
        private Long id;
        private String title;                // master product title (name)
        private Double price;
        private Integer stock;               // available stock
        private Boolean blocked;             // product-level blocked
        private Boolean categoryBlocked;     // optional
        private List<String> imageNames;     // optional - first element used for img
        private Double discountPrice;
        // Offer-aware pricing fields
        private Double originalPrice;   // base price before offers (optional)
        private Double finalPrice;      // best price after product/category offers
        private Double offerDiscount;   // discount percentage or amount (optional)
        private String appliedScope;    // e.g. "PRODUCT", "CATEGORY", or null


    }
