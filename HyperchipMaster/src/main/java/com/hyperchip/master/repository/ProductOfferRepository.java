package com.hyperchip.master.repository;

import com.hyperchip.master.model.ProductOffer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository interface for ProductOffer entity.
 * Extends JpaRepository to provide CRUD operations and custom queries.
 */
public interface ProductOfferRepository extends JpaRepository<ProductOffer, Long> {

    /**
     * Retrieves all active offers for a specific product.
     *
     * @param productId ID of the product
     * @return List of active ProductOffer entities associated with the given product
     */
    List<ProductOffer> findByProductIdAndActiveTrue(Long productId);
}
