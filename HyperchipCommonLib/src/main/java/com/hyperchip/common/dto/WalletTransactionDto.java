package com.hyperchip.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransactionDto {
    private Long id;
    private Long userId;
    private Long orderId;
    private String type;      // CREDIT / DEBIT
    private Double amount;
    private String description;
    private String source;    // ORDER_CANCELLED, RETURN_APPROVED, WALLET_PAYMENT
    private LocalDateTime createdAt;
}
