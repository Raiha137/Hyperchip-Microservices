package com.hyperchip.master.repository;

import com.hyperchip.master.model.Brand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository interface for Brand entity.
 * Extends JpaRepository to provide CRUD operations and custom queries.
 */
public interface BrandRepository extends JpaRepository<Brand, Long> {

    /**
     * Checks if a brand exists with the given name (case-insensitive) and is not soft-deleted.
     *
     * @param name Brand name to check
     * @return true if such a brand exists, false otherwise
     */
    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);

    /**
     * Retrieves all brands that are active and not deleted, with pagination support.
     *
     * @param pageable Pageable object containing page number, size, and sorting
     * @return Page of active, non-deleted brands
     */
    Page<Brand> findAllByDeletedFalseAndActiveTrue(Pageable pageable);

    /**
     * Searches for active, non-deleted brands whose names contain the given query string (case-insensitive).
     *
     * @param q        Search query
     * @param pageable Pageable object containing page number, size, and sorting
     * @return Page of brands matching the search criteria
     */
    @Query("SELECT b FROM Brand b WHERE b.deleted = false AND b.active = true " +
            "AND LOWER(b.name) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Brand> searchActive(@Param("q") String q, Pageable pageable);

    /**
     * Retrieves all non-deleted brands ordered by creation date descending.
     *
     * @return List of brands
     */
    List<Brand> findAllByDeletedFalseOrderByCreatedAtDesc();

    /**
     * Finds a brand by name (case-insensitive) if it is not deleted.
     *
     * @param name Brand name to search
     * @return Brand entity if found, null otherwise
     */
    Brand findByNameIgnoreCaseAndDeletedFalse(String name);
}
