package com.hyperchip.wishlist.controller;

import com.hyperchip.common.dto.WishlistItemDto;
import com.hyperchip.wishlist.model.WishlistItem;
import com.hyperchip.wishlist.service.WishlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * -------------------------
 * WishlistController
 * -------------------------
 * Handles all Wishlist related operations for the Hyperchip e-commerce platform.
 * This includes:
 * - Adding items to wishlist
 * - Updating item quantity
 * - Listing wishlist items
 * - Removing items from wishlist
 *
 * Developer Notes:
 * - Uses RestTemplate to fetch product details from Product Service.
 * - Quantity is restricted to 1..3 per item.
 * - Returns proper HTTP status codes (200, 404, 409, 500)
 */
@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@Slf4j
public class WishlistController {

    // -------------------------
    // Dependencies
    // -------------------------
    private final WishlistService wishlistService;
    private final RestTemplate restTemplate;

    // -------------------------
    // Product Service Base URL
    // -------------------------
    @Value("${product.service.url:${master.service.url:http://localhost:8086}}")
    private String productServiceBaseUrl;

    // -------------------------
    // Add product to user's wishlist
    // POST /api/wishlist/{userId}/items/{productId}
    // -------------------------
    @PostMapping("/{userId}/items/{productId}")
    public ResponseEntity<?> add(@PathVariable Long userId, @PathVariable Long productId) {

        /**
         * Developer Notes:
         * - Fetch product from public/admin endpoints
         * - Check stock availability
         * - Add to wishlist if available
         * - Returns 404 if product not found
         * - Returns 409 if out of stock
         * - Logs error and returns 500 for unexpected failures
         */
        try {
            String[] candidatePaths = new String[]{
                    productServiceBaseUrl.endsWith("/") ? productServiceBaseUrl + "api/products/" + productId
                            : productServiceBaseUrl + "/api/products/" + productId,
                    productServiceBaseUrl.endsWith("/") ? productServiceBaseUrl + "api/admin/products/" + productId
                            : productServiceBaseUrl + "/api/admin/products/" + productId
            };

            Map<String, Object> product = null;
            for (String url : candidatePaths) {
                try {
                    product = restTemplate.getForObject(url, Map.class);
                    if (product != null) break;
                } catch (Exception ignore) {
                    // Developer: try next URL if product not found
                }
            }

            if (product == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Product not found"));
            }

            Object stockObj = product.get("stock");
            int stock = 0;
            if (stockObj instanceof Number) {
                stock = ((Number) stockObj).intValue();
            } else if (stockObj != null) {
                try { stock = Integer.parseInt(String.valueOf(stockObj)); } catch (NumberFormatException ignored) {}
            }

            if (stock <= 0) {
                return ResponseEntity.status(409).body(Map.of("error", "Product is out of stock"));
            }

            wishlistService.addToWishlist(userId, productId);
            return ResponseEntity.ok(Map.of("result", "OK"));

        } catch (Exception ex) {
            log.error("Failed to add wishlist item userId={} productId={}", userId, productId, ex);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to add wishlist item",
                    "detail", ex.getMessage()
            ));
        }
    }

    // -------------------------
    // Set quantity for a wishlist item (1..3)
    // POST /api/wishlist/{userId}/items/{productId}/set-quantity
    // -------------------------
    @PostMapping("/{userId}/items/{productId}/set-quantity")
    public ResponseEntity<?> setQuantity(@PathVariable Long userId,
                                         @PathVariable Long productId,
                                         @RequestBody Map<String, Object> body) {

        /**
         * Developer Notes:
         * - Restricts quantity between 1 and 3
         * - Creates item if not exists, updates otherwise
         * - Returns the current quantity in response
         * - Logs error and returns 500 for unexpected failures
         */
        try {
            int qty = 1;
            if (body != null && body.containsKey("quantity")) {
                try {
                    Object q = body.get("quantity");
                    if (q instanceof Number) qty = ((Number) q).intValue();
                    else qty = Integer.parseInt(String.valueOf(q));
                } catch (Exception ignore) { qty = 1; }
            }

            if (qty < 1) qty = 1;
            if (qty > 3) qty = 3;

            List<WishlistItem> items = wishlistService.listByUserId(userId);
            WishlistItem existing = items.stream()
                    .filter(it -> productId != null && productId.equals(it.getProductId()))
                    .findFirst()
                    .orElse(null);

            if (existing == null) {
                WishlistItem it = WishlistItem.builder()
                        .userId(userId)
                        .productId(productId)
                        .quantity(qty)
                        .build();
                wishlistService.save(it);
                return ResponseEntity.ok(Map.of("result", "OK", "quantity", it.getQuantity()));
            } else {
                existing.setQuantity(qty);
                wishlistService.save(existing);
                return ResponseEntity.ok(Map.of("result", "OK", "quantity", existing.getQuantity()));
            }

        } catch (Exception ex) {
            log.error("Failed to update wishlist quantity userId={} productId={}", userId, productId, ex);
            return ResponseEntity.status(500).body(Map.of("error", ex.getMessage()));
        }
    }

    // -------------------------
    // Alias endpoint for updating quantity
    // POST /api/wishlist/{userId}/items/{productId}/updateQuantity
    // -------------------------
    @PostMapping("/{userId}/items/{productId}/updateQuantity")
    public ResponseEntity<?> updateQuantity(@PathVariable Long userId,
                                            @PathVariable Long productId,
                                            @RequestBody Map<String, Object> body) {
        /**
         * Developer Notes:
         * - Reuses setQuantity method
         * - Keeps single point of quantity logic
         */
        return setQuantity(userId, productId, body);
    }

    // -------------------------
    // List all wishlist items for a user
    // GET /api/wishlist/{userId}
    // -------------------------
    @GetMapping("/{userId}")
    public ResponseEntity<?> listByUser(@PathVariable Long userId) {

        /**
         * Developer Notes:
         * - Fetch product details from product service
         * - Builds WishlistItemDto for each item
         * - Handles missing or unknown product gracefully
         * - Returns 500 on unexpected failures
         */
        try {
            List<WishlistItem> items = wishlistService.listByUserId(userId);

            List<WishlistItemDto> result = items.stream().map(item -> {
                Long pid = item.getProductId();
                String title = "[Unknown]";
                double price = 0.0;
                String image = null;

                try {
                    String publicUrl = productServiceBaseUrl.endsWith("/") ? productServiceBaseUrl + "api/products/" + pid
                            : productServiceBaseUrl + "/api/products/" + pid;
                    String adminUrl  = productServiceBaseUrl.endsWith("/") ? productServiceBaseUrl + "api/admin/products/" + pid
                            : productServiceBaseUrl + "/api/admin/products/" + pid;

                    Map<String, Object> product = null;
                    try {
                        product = restTemplate.getForObject(publicUrl, Map.class);
                    } catch (Exception e) {
                        try {
                            product = restTemplate.getForObject(adminUrl, Map.class);
                        } catch (Exception ex) {
                            log.warn("Product {} not found via public/admin endpoints", pid);
                            product = null;
                        }
                    }

                    if (product == null) {
                        return WishlistItemDto.builder()
                                .productId(pid)
                                .productTitle("[Unknown]")
                                .unitPrice(0.0)
                                .productImage(null)
                                .quantity(item.getQuantity())
                                .build();
                    }

                    title = String.valueOf(product.getOrDefault("title", "[Unknown]"));

                    Object priceObj = product.get("price");
                    if (priceObj instanceof Number) price = ((Number) priceObj).doubleValue();
                    else if (priceObj != null) {
                        try { price = Double.parseDouble(String.valueOf(priceObj)); } catch (NumberFormatException ignored) {}
                    }

                    Object imgObj = product.get("imageNames");
                    if (imgObj instanceof List<?>) {
                        List<?> imgList = (List<?>) imgObj;
                        if (!imgList.isEmpty() && imgList.get(0) != null) {
                            String fname = String.valueOf(imgList.get(0));
                            String enc = URLEncoder.encode(fname, StandardCharsets.UTF_8);
                            String base = productServiceBaseUrl.endsWith("/") ? productServiceBaseUrl.substring(0, productServiceBaseUrl.length() - 1)
                                    : productServiceBaseUrl;
                            image = base + "/uploads/products/" + enc;
                        }
                    }

                    if (image == null) {
                        Object fallback = product.get("productImage");
                        if (fallback == null) fallback = product.get("image");
                        if (fallback != null) image = String.valueOf(fallback);
                    }

                    return WishlistItemDto.builder()
                            .productId(pid)
                            .productTitle(title)
                            .unitPrice(price)
                            .productImage(image)
                            .quantity(item.getQuantity())
                            .build();

                } catch (Exception e) {
                    log.error("Failed to fetch product details for id {}", pid, e);
                    return WishlistItemDto.builder()
                            .productId(pid)
                            .productTitle("[Unknown]")
                            .unitPrice(0.0)
                            .productImage(null)
                            .quantity(item.getQuantity())
                            .build();
                }
            }).collect(Collectors.toList());

            return ResponseEntity.ok(result);

        } catch (Exception ex) {
            log.error("Failed to list wishlist items for userId={}", userId, ex);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to fetch wishlist items",
                    "detail", ex.getMessage()
            ));
        }
    }

    // -------------------------
    // Remove an item from user's wishlist
    // DELETE /api/wishlist/{userId}/items/{productId}
    // -------------------------
    @DeleteMapping("/{userId}/items/{productId}")
    public ResponseEntity<?> remove(@PathVariable Long userId, @PathVariable Long productId) {

        /**
         * Developer Notes:
         * - Removes wishlist item for user
         * - Returns 204 on success
         * - Logs error and returns 500 if removal fails
         */
        try {
            wishlistService.removeWishlistItem(userId, productId);
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            log.error("Failed to remove wishlist item userId={} productId={}", userId, productId, ex);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to remove wishlist item",
                    "detail", ex.getMessage()
            ));
        }
    }
}
