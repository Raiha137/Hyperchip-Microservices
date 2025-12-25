package com.hyperchip.master.controller;

import com.hyperchip.common.dto.CategoryOfferDto;
import com.hyperchip.common.dto.ProductOfferDto;
import com.hyperchip.master.service.OfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AdminOfferController
 *
 * Purpose:
 * - Provide REST APIs for admin management of Product and Category offers.
 * - Supports CRUD operations: Create, Read, Update, Delete, and List.
 * - Follows RESTful conventions with clear URL structure.
 */
@RestController
@RequestMapping("/api/admin/offers")
@RequiredArgsConstructor
public class AdminOfferController {

    private final OfferService offerService;

    // ===========================
    // PRODUCT OFFERS
    // ===========================

    /**
     * Create a new Product Offer.
     * HTTP POST /api/admin/offers/product
     * @param dto ProductOfferDto object in request body
     * @return Created ProductOfferDto with generated ID
     */
    @PostMapping("/product")
    public ResponseEntity<ProductOfferDto> createProductOffer(@RequestBody ProductOfferDto dto) {
        ProductOfferDto created = offerService.createProductOffer(dto);
        return ResponseEntity.ok(created);
    }

    /**
     * Update an existing Product Offer by ID.
     * HTTP PUT /api/admin/offers/product/{id}
     * @param id Product offer ID
     * @param dto Updated ProductOfferDto
     * @return Updated ProductOfferDto
     */
    @PutMapping("/product/{id}")
    public ResponseEntity<ProductOfferDto> updateProductOffer(@PathVariable Long id,
                                                              @RequestBody ProductOfferDto dto) {
        ProductOfferDto updated = offerService.updateProductOffer(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Get a single Product Offer by ID.
     * HTTP GET /api/admin/offers/product/{id}
     * @param id Product offer ID
     * @return ProductOfferDto
     */
    @GetMapping("/product/{id}")
    public ResponseEntity<ProductOfferDto> getProductOffer(@PathVariable Long id) {
        ProductOfferDto offer = offerService.getProductOffer(id);
        return ResponseEntity.ok(offer);
    }

    /**
     * Delete a Product Offer by ID.
     * HTTP DELETE /api/admin/offers/product/{id}
     * @param id Product offer ID
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/product/{id}")
    public ResponseEntity<Void> deleteProductOffer(@PathVariable Long id) {
        offerService.deleteProductOffer(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * List all Product Offers.
     * HTTP GET /api/admin/offers/product
     * @return List of ProductOfferDto
     */
    @GetMapping("/product")
    public ResponseEntity<List<ProductOfferDto>> getAllProductOffers() {
        List<ProductOfferDto> offers = offerService.getAllProductOffers();
        return ResponseEntity.ok(offers);
    }

    // ===========================
    // CATEGORY OFFERS
    // ===========================

    /**
     * Create a new Category Offer.
     * HTTP POST /api/admin/offers/category
     * @param dto CategoryOfferDto object in request body
     * @return Created CategoryOfferDto
     */
    @PostMapping("/category")
    public ResponseEntity<CategoryOfferDto> createCategoryOffer(@RequestBody CategoryOfferDto dto) {
        CategoryOfferDto created = offerService.createCategoryOffer(dto);
        return ResponseEntity.ok(created);
    }

    /**
     * Update an existing Category Offer by ID.
     * HTTP PUT /api/admin/offers/category/{id}
     * @param id Category offer ID
     * @param dto Updated CategoryOfferDto
     * @return Updated CategoryOfferDto
     */
    @PutMapping("/category/{id}")
    public ResponseEntity<CategoryOfferDto> updateCategoryOffer(@PathVariable Long id,
                                                                @RequestBody CategoryOfferDto dto) {
        CategoryOfferDto updated = offerService.updateCategoryOffer(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Get a single Category Offer by ID.
     * HTTP GET /api/admin/offers/category/{id}
     * @param id Category offer ID
     * @return CategoryOfferDto
     */
    @GetMapping("/category/{id}")
    public ResponseEntity<CategoryOfferDto> getCategoryOffer(@PathVariable Long id) {
        CategoryOfferDto offer = offerService.getCategoryOffer(id);
        return ResponseEntity.ok(offer);
    }

    /**
     * Delete a Category Offer by ID.
     * HTTP DELETE /api/admin/offers/category/{id}
     * @param id Category offer ID
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/category/{id}")
    public ResponseEntity<Void> deleteCategoryOffer(@PathVariable Long id) {
        offerService.deleteCategoryOffer(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * List all Category Offers.
     * HTTP GET /api/admin/offers/category
     * @return List of CategoryOfferDto
     */
    @GetMapping("/category")
    public ResponseEntity<List<CategoryOfferDto>> getAllCategoryOffers() {
        List<CategoryOfferDto> offers = offerService.getAllCategoryOffers();
        return ResponseEntity.ok(offers);
    }
}
