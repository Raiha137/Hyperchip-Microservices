package com.hyperchip.master.service.impl;

import com.hyperchip.common.dto.CategoryDto;
import com.hyperchip.master.model.Category;
import com.hyperchip.master.repository.CategoryRepository;
import com.hyperchip.master.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service implementation for Category entity.
 * Provides CRUD operations, soft delete, pagination, and image handling for categories.
 * Also includes basic event publishing logic (sendEvent method) for audit/logging.
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    // Topic name for event logging/messaging (can be integrated with Kafka or other messaging system)
    private static final String TOPIC = "category-events";

    // ----------------------------------- CREATE CATEGORY -----------------------------------
    /**
     * Saves a new category to the database.
     * Performs duplicate name check (case-insensitive) and handles image upload if provided.
     * Marks category as active and not deleted.
     * Sends a "CATEGORY_CREATED" event after saving.
     *
     * @param category Category entity to save
     * @param file     Optional image file for the category
     * @return Saved Category entity
     * @throws IOException if image upload fails
     */
    @Override
    public Category saveCategory(Category category, MultipartFile file) throws IOException {
        if (categoryRepository.existsByNameIgnoreCaseAndDeletedFalse(category.getName())) {
            throw new RuntimeException("Category already exists!");
        }

        category.setDeleted(false);
        category.setActive(true);

        // Handle image upload if file is provided
        if (file != null && !file.isEmpty()) {
            category.setImageName(storeImage(file));
        }

        Category saved = categoryRepository.save(category);
        sendEvent("CATEGORY_CREATED", saved); // log or publish event
        return saved;
    }

    // ----------------------------------- UPDATE CATEGORY -----------------------------------
    /**
     * Updates an existing category.
     * Updates name, active status, and optionally image.
     * Sends a "CATEGORY_UPDATED" event after saving.
     *
     * @param category Category entity with updated values
     * @param file     Optional new image file
     * @return Updated Category entity
     * @throws IOException if image upload fails
     */
    @Override
    public Category updateCategory(Category category, MultipartFile file) throws IOException {
        Category existing = categoryRepository.findById(category.getId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        existing.setName(category.getName());
        existing.setActive(category.isActive());

        if (file != null && !file.isEmpty()) {
            existing.setImageName(storeImage(file));
        }

        Category updated = categoryRepository.save(existing);
        sendEvent("CATEGORY_UPDATED", updated);
        return updated;
    }

    // ----------------------------------- SOFT DELETE CATEGORY -----------------------------------
    /**
     * Soft deletes a category by setting the "deleted" flag to true.
     * Sends a "CATEGORY_DELETED" event after saving.
     *
     * @param id ID of the category to delete
     * @return Soft-deleted Category entity
     */
    @Override
    public Category softDeleteCategory(Long id) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        existing.setDeleted(true);
        Category deleted = categoryRepository.save(existing);
        sendEvent("CATEGORY_DELETED", deleted);
        return deleted;
    }

    // ----------------------------------- LIST CATEGORIES WITH PAGINATION & SEARCH -----------------------------------
    /**
     * Retrieves paginated list of active categories, optionally filtered by search query.
     *
     * @param q    Optional search query for category name
     * @param page Page number (0-based)
     * @param size Page size
     * @return Page of categories
     */
    @Override
    public Page<Category> listCategories(String q, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (q == null || q.isEmpty()) {
            return categoryRepository.findAllByDeletedFalse(pageable);
        } else {
            return categoryRepository.searchActive(q, pageable);
        }
    }

    // ----------------------------------- GET CATEGORY BY ID -----------------------------------
    /**
     * Retrieves a category by ID if it is not soft-deleted.
     *
     * @param id Category ID
     * @return Optional containing the Category entity or empty if not found/deleted
     */
    @Override
    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .filter(cat -> !cat.isDeleted());
    }

    // ----------------------------------- HELPER METHOD: STORE IMAGE -----------------------------------
    /**
     * Stores the uploaded image file on disk under "uploads/categories".
     * Prepends current timestamp to filename to avoid collisions.
     *
     * @param file MultipartFile to store
     * @return Stored filename
     * @throws IOException if saving file fails
     */
    private String storeImage(MultipartFile file) throws IOException {
        String uploadDir = "uploads/categories";
        Path uploadPath = Paths.get(uploadDir);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return fileName;
    }

    // ----------------------------------- HELPER METHOD: SEND EVENT -----------------------------------
    /**
     * Sends a simple event with category details.
     * Currently implemented as a stub; can be integrated with messaging system or logging.
     *
     * @param eventType Event type string (e.g., CATEGORY_CREATED, CATEGORY_UPDATED, CATEGORY_DELETED)
     * @param category  Category entity involved in the event
     */
    private void sendEvent(String eventType, Category category) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", eventType);
        event.put("id", category.getId());
        event.put("name", category.getName());
        event.put("deleted", category.isDeleted());
        event.put("active", category.isActive());
        event.put("timestamp", System.currentTimeMillis());

        // TODO: Integrate with Kafka, RabbitMQ, or log system if needed
    }

    // ----------------------------------- GET ALL CATEGORIES AS DTO -----------------------------------
    /**
     * Retrieves all categories and converts them to CategoryDto for frontend or API consumption.
     *
     * @return List of CategoryDto
     */
    @Override
    public List<CategoryDto> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();

        return categories.stream()
                .map(cat -> {
                    CategoryDto dto = new CategoryDto();
                    dto.setId(cat.getId());
                    dto.setName(cat.getName());
                    dto.setActiveName(cat.getActiveName());
                    return dto;
                })
                .collect(Collectors.toList());

    }
}
