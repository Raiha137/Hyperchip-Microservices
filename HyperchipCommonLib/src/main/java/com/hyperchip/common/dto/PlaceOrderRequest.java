package com.hyperchip.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceOrderRequest {

    private Long userId;
    private Long addressId;
    private String paymentMethod;

    // optional – frontend can send
    private String userEmail;

    // optional – frontend sends, server can recompute if needed
    private Double totalAmount;

    // coupon support (optional)
    private String couponCode;
    private Double discountAmount;



    // cart items coming from frontend
    private List<OrderItemDto> items;
}
