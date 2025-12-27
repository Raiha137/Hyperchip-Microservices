package com.hyperchip.common.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferralRewardDto {
    private Long id;
    private Long referrerUserId;
    private Long referredUserId;
    private String referralCode;
    private String status;     // PENDING_REWARD, COUPON_ISSUED, USED, EXPIRED
    private LocalDateTime createdAt;
    private String referredUserEmail;

}
