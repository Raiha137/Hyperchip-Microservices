package com.hyperchip.user.controller;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * UserPaymentController
 * ---------------------
 * Handles user payment-related endpoints.
 * Currently supports redirecting to PayPal for order payment.
 */
@Controller
@RequestMapping("/user/payment")
@RequiredArgsConstructor
public class UserPaymentController {

    private static final Logger log = LoggerFactory.getLogger(UserPaymentController.class);

    // Base URL of the payment service (can be overridden in application.properties)
    @Value("${payment.service.url:http://localhost:8094/api/payments}")
    private String paymentServiceUrl;

    private final RestTemplate restTemplate;

    /**
     * Redirect user to PayPal payment page for the given order.
     *
     * @param orderId  ID of the order
     * @param amount   Total amount to pay
     * @param currency Payment currency (e.g., USD)
     * @return Redirect URL to PayPal, or fallback to order failure page
     */
    @GetMapping("/paypal")
    public String redirectToPayPal(@RequestParam("orderId") Long orderId,
                                   @RequestParam("amount") BigDecimal amount,
                                   @RequestParam("currency") String currency) {
        try {
            // Build the payload to send to payment service
            Map<String, Object> request = new HashMap<>();
            request.put("appOrderId", orderId);
            request.put("amount", amount);
            request.put("currency", currency);

            // Call the payment service to create a PayPal order
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    paymentServiceUrl + "/createPaypalOrder",
                    request,
                    Map.class
            );

            // If successful, extract redirect URL from response and redirect user
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null
                    && Boolean.TRUE.equals(response.getBody().get("success"))) {
                String redirectUrl = (String) response.getBody().get("data");
                return "redirect:" + redirectUrl;
            }

        } catch (Exception e) {
            log.error("Failed to create PayPal order for orderId={}", orderId, e);
        }

        // Fallback: redirect to order failure page
        return "redirect:/user/order/payment-failed?orderId=" + orderId;
    }
}
