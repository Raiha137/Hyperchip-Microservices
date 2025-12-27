package com.hyperchip.order.service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Client service for interacting with the Master Service's Product API.
 * Used to increment or decrement product stock when orders are placed or cancelled.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceClient {

    private final RestTemplate restTemplate;

    /**
     * Base URL for the Master Service.
     * 🔒 DEV: Hard-coded for local development to avoid configuration mistakes.
     *
     * @return base URL as a String
     */
    private String baseUrl() {
        String base = "http://localhost:8086"; // master-service port
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }

    /**
     * Increment the stock of a product in the Master Service.
     *
     * @param productId ID of the product
     * @param quantity  quantity to increment
     */
    public void incrementStock(Long productId, Integer quantity) {
        if (productId == null || quantity == null) return;

        String url = baseUrl() + "/api/master/inventory/" + productId + "/increment?qty=" + quantity;
        try {
            log.info("CALL incrementStock url={}", url);
            restTemplate.postForObject(url, null, Void.class);
        } catch (Exception ex) {
            log.error("ERROR incrementStock productId={} qty={} : {}", productId, quantity, ex.getMessage());
            throw ex;
        }
    }

    /**
     * Decrement the stock of a product in the Master Service.
     *
     * @param productId ID of the product
     * @param quantity  quantity to decrement
     */
    public void decrementStock(Long productId, Integer quantity) {
        if (productId == null || quantity == null) return;

        String url = baseUrl() + "/api/master/inventory/" + productId + "/decrement?qty=" + quantity;
        try {
            log.info("CALL decrementStock url={}", url);
            restTemplate.postForObject(url, null, Void.class);
        } catch (Exception ex) {
            log.error("ERROR decrementStock productId={} qty={} : {}", productId, quantity, ex.getMessage());
            throw ex;
        }
    }
}
