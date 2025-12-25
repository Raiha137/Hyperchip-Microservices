package com.hyperchip.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

/**
 * DeliveryProxyController
 * -----------------------
 * Acts as a proxy between the User Service and the Order Service's delivery endpoints.
 *
 * Purpose:
 * - Allows frontend/user service to call delivery charge endpoint without exposing internal service URL directly.
 * - Encodes parameters safely and forwards GET request.
 */
@RestController
@RequestMapping("/user/api/proxy/delivery")
@RequiredArgsConstructor
public class DeliveryProxyController {

    /**
     * RestTemplate for making HTTP requests to Order Service
     */
    private final RestTemplate restTemplate;

    /**
     * Base URL of the order service
     * - Configurable via application.properties
     */
    @Value("${order.service.url:http://localhost:8092}")
    private String orderServiceBase;

    /**
     * charge
     * ------
     * GET /user/api/proxy/delivery/charge?pinCode={pinCode}
     *
     * Purpose:
     * - Accepts a pin code from the user frontend
     * - Encodes it safely for URL usage
     * - Calls the corresponding Order Service delivery charge endpoint
     * - Returns the raw response body as-is to the client
     */
    @GetMapping("/charge")
    public ResponseEntity<String> charge(@RequestParam String pinCode) {
        String pin = (pinCode == null) ? "" : pinCode.trim();

        // Normalize base URL to avoid double slashes
        String url = orderServiceBase;
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);

        // Construct target URL and encode query param
        String target = url + "/api/delivery/charge?pinCode=" + UriUtils.encode(pin, StandardCharsets.UTF_8);

        // Forward GET request to Order Service
        return restTemplate.getForEntity(target, String.class);
    }
}
