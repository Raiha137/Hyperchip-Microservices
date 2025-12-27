package com.hyperchip.common.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletPaymentRequest {
    private Long userId;
    private Double amount;
    private Long orderId;        // for reference
    private String description;  // e.g. "ORDER_PAY"
}
