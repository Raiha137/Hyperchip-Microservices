package com.hyperchip.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UserDto
 * - Data Transfer Object (DTO) for sending user info in APIs
 * - Keeps only essential fields (no password)
 * - Can be used in REST responses or mapping to frontend
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {

    private Long id;         // user ID
    private String fullName; // full name of user
    private String email;    // email address
    private String phone;    // phone number
    private String role;     // user role, e.g., ROLE_USER, ROLE_ADMIN
}
