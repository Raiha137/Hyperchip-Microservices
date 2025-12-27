package com.hyperchip.wishlist.service.impl;

import com.hyperchip.wishlist.model.WishlistItem;
import com.hyperchip.wishlist.repository.WishlistItemRepository;
import com.hyperchip.wishlist.service.WishlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import com.hyperchip.common.exception.LimitExceededException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

// -------------------------
// WishlistServiceImpl
// -------------------------
// Service implementation for managing WishlistItems.
// Handles CRUD operations, quantity updates, and product availability checks.
// Integrates with external Product Service via RestTemplate.
//
// Developer Notes:
// - All public methods are transactional.
// - addToWishlist enforces maximum 3 items per product per user.
// - listByUserId fetches all items for a given user.
// - ensureAbsoluteImageUrl converts relative product images to absolute URLs.
// - Exception handling uses custom LimitExceededException and RuntimeExceptions.

@Service
@RequiredArgsConstructor
@Slf4j
public class WishlistServiceImpl implements WishlistService {

    private final WishlistItemRepository repo;
    private final RestTemplate restTemplate;

    @Value("${uploads.base.url:http://localhost:8086}")
    private String uploadsBaseUrl;

    @Value("${product.service.url:http://localhost:8086}")
    private String productServiceBase;

    @Value("${master.service.url:http://localhost:8086}")
    private String masterServiceBase;

    @Value("${product.service.url:${master.service.url:http://localhost:8086}}")
    private String productServiceBaseUrl;


    // -------------------------
    // Add a product to user's wishlist
    // -------------------------
    @Override
    @Transactional
    public WishlistItem addToWishlist(Long userId, Long productId) {

        // Check if wishlist item already exists
        Optional<WishlistItem> existingOpt = repo.findByUserIdAndProductId(userId, productId);

        if (existingOpt.isPresent()) {
            WishlistItem existing = existingOpt.get();

            // Enforce maximum 3 items per product
            if (existing.getQuantity() >= 3) {
                throw new LimitExceededException("Maximum 3 items allowed per product");
            }

            existing.setQuantity(existing.getQuantity() + 1);
            return repo.save(existing);
        }

        // Product availability check via RestTemplate
        String[] candidatePaths = new String[] {
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
            } catch (Exception ignore) {}
        }

        if (product == null) {
            throw new RuntimeException("Product not found");
        }

        // Extract stock and validate
        Object stockObj = product.get("stock");
        int stock = 0;
        if (stockObj instanceof Number) stock = ((Number) stockObj).intValue();
        else if (stockObj != null) {
            try { stock = Integer.parseInt(String.valueOf(stockObj)); } catch (NumberFormatException ignored) {}
        }

        if (stock <= 0) throw new RuntimeException("Product is out of stock");

        // Create new wishlist item with quantity 1
        WishlistItem it = WishlistItem.builder()
                .userId(userId)
                .productId(productId)
                .quantity(1)
                .build();

        return repo.save(it);
    }

    // -------------------------
    // Save or update a wishlist item
    // -------------------------
    @Override
    @Transactional
    public WishlistItem save(WishlistItem item) {
        return repo.save(item);
    }

    // -------------------------
    // Remove wishlist item for user and product
    // -------------------------
    @Override
    @Transactional
    public void removeWishlistItem(Long userId, Long productId) {
        repo.findByUserIdAndProductId(userId, productId).ifPresent(repo::delete);
    }

    // -------------------------
    // List all wishlist items for a specific user
    // -------------------------
    @Override
    @Transactional(readOnly = true)
    public List<WishlistItem> listByUserId(Long userId) {
        return repo.findByUserId(userId);
    }

    // -------------------------
    // Convert relative image path to absolute URL
    // -------------------------
    public String ensureAbsoluteImageUrl(Object imageObj) {
        if (imageObj == null) return "/img/default-product.png";

        String t = String.valueOf(imageObj).trim();
        if (t.isEmpty()) return "/img/default-product.png";
        if (t.startsWith("http://") || t.startsWith("https://")) return t;

        String[] bases = new String[]{uploadsBaseUrl, productServiceBase, masterServiceBase};
        for (String base : bases) {
            if (base == null || base.trim().isEmpty()) continue;
            base = base.trim();
            if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
            try {
                String encoded = java.net.URLEncoder.encode(t, java.nio.charset.StandardCharsets.UTF_8);
                return base + "/uploads/products/" + encoded;
            } catch (Exception e) {
                return base + "/uploads/products/" + t;
            }
        }
        return t;
    }

}
