package com.hyperchip.master.repository;

import com.hyperchip.master.model.CategoryOffer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository interface for CategoryOffer entity.
 * Extends JpaRepository to provide CRUD operations and custom queries.
 */
public interface CategoryOfferRepository extends JpaRepository<CategoryOffer, Long> {

    /**
     * Retrieves all active offers for a specific category.
     *
     * @param categoryId ID of the category
     * @return List of active CategoryOffer entities associated with the given category
     */
    List<CategoryOffer> findByCategoryIdAndActiveTrue(Long categoryId);
}
