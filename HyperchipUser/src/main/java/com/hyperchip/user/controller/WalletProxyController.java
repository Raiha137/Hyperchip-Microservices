package com.hyperchip.user.controller;

import com.hyperchip.common.dto.WalletBalanceDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

/**
 * WalletProxyController
 * ----------------------
 * Acts as a proxy between the user module and the wallet microservice.
 * Allows fetching the wallet balance for a specific user via REST.
 */
@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletProxyController {

    private final RestTemplate restTemplate;

    /**
     * Base URL for the wallet service. Can be configured in application.properties.
     * Default: http://localhost:8095
     */
    @Value("${wallet.service.url:http://localhost:8095}")
    private String walletServiceBase;

    /**
     * GET /api/wallet/{userId}
     * -------------------------
     * Fetches the wallet balance for the given user.
     * Acts as a proxy call to the wallet microservice.
     *
     * @param userId ID of the user whose wallet balance is requested
     * @return WalletBalanceDto containing userId and current balance
     */
    @GetMapping("/{userId}")
    public WalletBalanceDto getWalletBalance(@PathVariable Long userId) {
        // Ensure wallet service URL does not end with slash
        String base = walletServiceBase;
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        // Construct the final URL to call the wallet microservice
        String url = base + "/api/wallet/" + userId;

        // Call wallet-service and retrieve response
        WalletBalanceDto response = restTemplate.getForObject(url, WalletBalanceDto.class);

        // If the wallet does not exist yet, return a default balance of 0.0
        if (response == null) {
            response = WalletBalanceDto.builder()
                    .userId(userId)
                    .balance(0.0)
                    .build();
        }

        return response;
    }
}
