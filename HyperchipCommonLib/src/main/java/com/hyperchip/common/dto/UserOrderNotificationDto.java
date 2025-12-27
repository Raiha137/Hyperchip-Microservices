// src/main/java/com/hyperchip/common/dto/UserOrderNotificationDto.java
package com.hyperchip.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserOrderNotificationDto {
    private Long userId;
    private Long orderId;
    private String orderNumber;
    private String status;
    private String note;
    private Double total;
    private String userEmail;
}
