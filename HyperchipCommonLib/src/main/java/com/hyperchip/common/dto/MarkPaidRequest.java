package com.hyperchip.common.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MarkPaidRequest {
    private String paymentReference;
    private String paymentMethod;
    private Double amount;
}
