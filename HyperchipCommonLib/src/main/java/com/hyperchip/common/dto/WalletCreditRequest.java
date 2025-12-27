package com.hyperchip.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Used by order-service / admin-service to refund money
 * to the user's wallet (cancel / return).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletCreditRequest {
    private Long userId;
    private Long orderId;
    private Double amount;
    private String reason;   // "Order cancelled", "Return approved"
    private String source;   // "ORDER_CANCELLED", "RETURN_APPROVED"
}
