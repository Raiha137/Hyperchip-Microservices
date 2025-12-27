package com.hyperchip.common.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateOrderRequest {
    private Long appOrderId;     // order id from your order service
    private BigDecimal amount;   // amount in major units (e.g., 100.50)
    private String currency;     // e.g., INR
}
