package com.hyperchip.common.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferralRegistrationRequest {
    private String referralCode;    // code entered by new user during signup
    private Long newUserId;         // id of new user
    private String newUserEmail;    // optional
}
