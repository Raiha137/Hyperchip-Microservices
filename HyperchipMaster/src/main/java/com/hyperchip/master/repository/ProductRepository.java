package com.hyperchip.master.repository;

import com.hyperchip.master.model.Product;
import com.hyperchip.master.model.Category;
import com.hyperchip.master.model.Brand;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository interface for managing Product entities.
 * Includes custom queries for search, filtering, and validation.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    // ---------------- Search active products with optional filters ----------------
    @EntityGraph(attributePaths = {"category", "brand"})
    @Query("""
 SELECT p FROM Product p 
 WHERE p.deleted = false 
 AND ( :keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) 
       OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) 
       OR (p.category IS NOT NULL AND LOWER(p.category.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) 
       OR (p.brand IS NOT NULL AND LOWER(p.brand.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
     )
            AND (:categoryName IS NULL OR LOWER(TRIM(p.category.name)) = LOWER(TRIM(:categoryName)))
            AND (:brandName IS NULL OR LOWER(TRIM(p.brand.name)) = LOWER(TRIM(:brandName)))
 AND (:minPrice IS NULL OR p.price >= :minPrice)
 AND (:maxPrice IS NULL OR p.price <= :maxPrice)
 """)
    Page<Product> searchWithFilters(
            @Param("keyword") String keyword,
            @Param("categoryName") String categoryName,
            @Param("brandName") String brandName,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            Pageable pageable
    );






    // ---------------- Find all active products with pagination in descending creation order ----------------
    @EntityGraph(attributePaths = {"category", "brand"})
    Page<Product> findAllByDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    // ---------------- Find all active products by category in descending ID order ----------------
    @EntityGraph(attributePaths = {"category", "brand"})
    List<Product> findAllByCategoryAndDeletedFalseOrderByIdDesc(Category category);

    // ---------------- Find all active products by brand in descending ID order ----------------
    @EntityGraph(attributePaths = {"category", "brand"})
    List<Product> findAllByBrandAndDeletedFalseOrderByIdDesc(Brand brand);

    // ---------------- Check existence by title (case-insensitive) for validation ----------------
    boolean existsByTitleIgnoreCaseAndDeletedFalse(String title);

    // ---------------- Find a product by ID if not deleted ----------------
    @EntityGraph(attributePaths = {"category", "brand"})
    Product findByIdAndDeletedFalse(Long id);

    // ---------------- Optional: Find all active products with stock > 0 ----------------
    @EntityGraph(attributePaths = {"category", "brand"})
    List<Product> findAllByDeletedFalseAndActiveTrueAndStockGreaterThanOrderByIdDesc(int stock);

    // ---------------- Optional: Search by title (ignore case) for autocomplete or suggestions ----------------
    @EntityGraph(attributePaths = {"category", "brand"})
    List<Product> findByTitleIgnoreCaseContainingAndDeletedFalse(String keyword);
}
