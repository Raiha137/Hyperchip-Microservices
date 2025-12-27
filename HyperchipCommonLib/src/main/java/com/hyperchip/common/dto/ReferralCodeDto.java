package com.hyperchip.common.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferralCodeDto {
    private Long id;
    private Long userId;            // existing user
    private String code;            // unique referral code
    private Boolean active;
    private LocalDateTime createdAt;
}
