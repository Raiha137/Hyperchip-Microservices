package com.hyperchip.master.controller;

import com.hyperchip.common.dto.BestOfferRequest;
import com.hyperchip.common.dto.BestOfferResponse;
import com.hyperchip.master.service.OfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * OfferPricingController
 *
 * Purpose:
 * - Provides REST API to calculate the best applicable offer price for a product.
 * - This includes evaluating both product-specific and category-wide offers to return
 *   the lowest final price.
 */
@RestController
@RequestMapping("/api/offers") // Base path for offer-related operations
@RequiredArgsConstructor
public class OfferPricingController {

    private final OfferService offerService; // Service containing offer calculation logic

    // ===========================
    // CALCULATE BEST OFFER PRICE
    // ===========================
    /**
     * POST /api/offers/best-price
     *
     * Purpose:
     * - Calculate the best price for a given product based on product and category offers.
     * - Typically used during checkout or product display to show discounted price.
     *
     * @param request Contains:
     *                - productId: ID of the product to evaluate
     *                - categoryId: ID of the product's category
     *                - originalPrice: Original product price before offers
     * @return BestOfferResponse containing:
     *         - originalPrice
     *         - discountedPrice
     *         - appliedOffer details (product or category)
     */
    @PostMapping("/best-price")
    public ResponseEntity<BestOfferResponse> calculateBestPrice(@RequestBody BestOfferRequest request) {
        BestOfferResponse resp = offerService.calculateBestOffer(
                request.getProductId(),   // Product ID to check offers
                request.getCategoryId(),  // Category ID to check category offers
                request.getOriginalPrice() // Original product price
        );
        return ResponseEntity.ok(resp); // Return 200 OK with calculated best offer
    }
}
