package com.hyperchip.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactDto {
    @NotBlank @Size(max = 100)
    private String name;

    @NotBlank @Email @Size(max = 254)
    private String email;

    @Size(max = 32)
    private String phone;

    @Size(max = 64)
    private String orderId;

    @NotBlank @Size(min = 5, max = 200)
    private String subject;

    @NotBlank @Size(min = 10, max = 2000)
    private String message;

    // optional flags/metadata
    private Boolean urgent;
}

