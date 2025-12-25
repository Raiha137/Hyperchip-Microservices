package com.hyperchip.master.repository;

import com.hyperchip.master.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository interface for Category entity.
 * Extends JpaRepository to provide CRUD operations and custom queries.
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Searches for categories that are not deleted and whose names contain the given query (case-insensitive).
     *
     * @param q        Search query
     * @param pageable Pageable object containing page number, size, and sorting
     * @return Page of categories matching the search criteria
     */
    @Query("SELECT c FROM Category c WHERE c.deleted = false " +
            "AND LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Category> searchActive(@Param("q") String q, Pageable pageable);

    /**
     * Retrieves all categories that are not deleted with pagination support.
     *
     * @param pageable Pageable object containing page number, size, and sorting
     * @return Page of non-deleted categories
     */
    Page<Category> findAllByDeletedFalse(Pageable pageable);

    /**
     * Checks if a category exists with the given name (case-insensitive) and is not deleted.
     *
     * @param name Category name to check
     * @return true if such a category exists, false otherwise
     */
    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);

    /**
     * Retrieves all non-deleted categories ordered by ID descending.
     *
     * @return List of categories
     */
    List<Category> findAllByDeletedFalseOrderByIdDesc();

    /**
     * Finds a category by name (case-insensitive) if it is not deleted.
     *
     * @param name Category name to search
     * @return Category entity if found, null otherwise
     */
    Category findByNameIgnoreCaseAndDeletedFalse(String name);

    /**
     * Checks if a category exists with the given name (case-insensitive).
     * Unlike existsByNameIgnoreCaseAndDeletedFalse, this does not consider the deleted flag.
     *
     * @param name Category name to check
     * @return true if such a category exists, false otherwise
     */
    boolean existsByNameIgnoreCase(String name);
}
