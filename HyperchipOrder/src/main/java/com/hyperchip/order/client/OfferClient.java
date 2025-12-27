package com.hyperchip.order.client;

import com.hyperchip.common.dto.BestOfferRequest;
import com.hyperchip.common.dto.BestOfferResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

/**
 * OfferClient
 *
 * Purpose:
 * - Calls the Master/Offer service to fetch the best available discount for a product.
 * - Provides a safe fallback when the offer service is unavailable or returns null.
 *
 * Developer notes:
 * - This is a simple HTTP client using RestTemplate. It intentionally keeps logic minimal:
 *      1) build request DTO
 *      2) call offer service endpoint
 *      3) return response or a safe fallback (no discount)
 * - Prefer to add retry/circuit-breaker (Resilience4j or Spring Retry) if the offer service
 *   is expected to be unreliable or slow. For now, failures are handled by returning the original price.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OfferClient {

    private final RestTemplate restTemplate;

    // Base URL for the master service (offers). Default points to localhost for local dev.
    @Value("${master.service.url:http://localhost:8086}")
    private String masterBaseUrl;

    /**
     * getBestPrice
     *
     * Flow:
     * 1. Build a BestOfferRequest with product, category and original price
     * 2. POST it to /api/offers/best-price on master service
     * 3. Return the BestOfferResponse from master service
     * 4. If anything goes wrong (exception or null response), return a fallback
     *    indicating no discount (finalPrice == originalPrice)
     *
     * Notes:
     * - This method is defensive: it never propagates upstream errors — it logs and returns a fallback.
     * - The fallback ensures callers will still receive a valid BestOfferResponse object.
     *
     * @param productId     the id of the product (nullable)
     * @param categoryId    the id of the product's category (nullable)
     * @param originalPrice the original price before any discounts
     * @return BestOfferResponse with finalPrice and discountAmount populated
     */
    public BestOfferResponse getBestPrice(Long productId, Long categoryId, BigDecimal originalPrice) {
        try {
            // Compose the target URL
            String url = masterBaseUrl + "/api/offers/best-price";

            // Build request DTO expected by the master service
            BestOfferRequest req = BestOfferRequest.builder()
                    .productId(productId)
                    .categoryId(categoryId)
                    .originalPrice(originalPrice)
                    .build();

            // Call the master service (synchronous call)
            BestOfferResponse resp =
                    restTemplate.postForObject(url, req, BestOfferResponse.class);

            // If service returned null, treat it as "no discount" (safe fallback)
            if (resp == null) {
                return BestOfferResponse.builder()
                        .originalPrice(originalPrice)
                        .discountAmount(BigDecimal.ZERO)
                        .finalPrice(originalPrice)
                        .build();
            }

            // Return the successful response as-is
            return resp;

        } catch (Exception ex) {
            // Log the failure and return a safe fallback (no discount)
            // Keeping the system resilient by not throwing exceptions for offer lookup failures.
            log.warn("Offer service call failed: {}", ex.getMessage());

            return BestOfferResponse.builder()
                    .originalPrice(originalPrice)
                    .discountAmount(BigDecimal.ZERO)
                    .finalPrice(originalPrice)
                    .build();
        }
    }
}
