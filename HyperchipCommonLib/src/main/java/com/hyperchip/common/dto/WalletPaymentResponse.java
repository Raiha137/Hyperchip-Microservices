package com.hyperchip.common.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletPaymentResponse {
    private boolean success;
    private String message;
    private Double remainingBalance;
    private Double paidAmount;
    private Long userId;
    private Long orderId;
}
