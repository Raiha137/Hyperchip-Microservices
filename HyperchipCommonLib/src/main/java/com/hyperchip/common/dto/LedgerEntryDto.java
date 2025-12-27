package com.hyperchip.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntryDto {

    // ORDER / WALLET
    private String type;

    // CREDIT / DEBIT
    private String direction;

    private Long orderId;
    private String orderNumber;

    private Long userId;
    private String userEmail;

    // For wallet or orders
    private String paymentMethod;   // RAZORPAY / WALLET / COD / null for wallet tx
    private String source;          // ORDER_CANCELLED, RETURN_APPROVED, WALLET_PAYMENT, etc.

    private Double amount;

    // When this money movement happened
    private Instant createdAt;
}
