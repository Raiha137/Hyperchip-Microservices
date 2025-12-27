package com.hyperchip.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productImage;
    private Integer quantity;
    private Double unitPrice;
    private Double total;
    private String productTitle;
    private Double subtotal;
    // inside class OrderItemResponse
    private Boolean cancelled;
    private String cancelReason;

}
