package com.hyperchip.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceOrderResponse {
    private boolean success;
    private Long orderId;
    private String orderNumber;
    private String status;

    // added fields required by OrderServiceImpl
    private Double subtotal;
    private Double total;
    private Integer totalItems;

    private String message;
    // keep both names to match different callers
    private List<OrderItemResponse> items;
    private List<OrderItemResponse> orderItems;

    private Instant createdAt;
}
