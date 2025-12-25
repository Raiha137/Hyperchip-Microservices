package com.hyperchip.master.service.impl;

import com.hyperchip.master.model.Brand;
import com.hyperchip.master.repository.BrandRepository;
import com.hyperchip.master.service.BrandService;
import com.hyperchip.common.dto.BrandDto;
import com.hyperchip.common.dto.PageBrandDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service implementation for Brand entity.
 * Provides CRUD operations, soft delete, pagination, and image handling for brands.
 * Also includes basic event publishing logic (sendEvent method) for audit/logging.
 */
@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;

    // Topic name for event logging/messaging (can be integrated with Kafka or other messaging system)
    private static final String TOPIC = "brand-events";

    // ----------------------------------- CREATE BRAND -----------------------------------
    /**
     * Saves a new brand to the database.
     * Performs duplicate name check (case-insensitive) and handles image upload if provided.
     * Marks brand as active and not deleted.
     * Sends a "BRAND_CREATED" event after saving.
     *
     * @param brand Brand entity to save
     * @param file  Optional image file for the brand
     * @return Saved Brand entity
     * @throws IOException if image upload fails
     */
    @Override
    public Brand saveBrand(Brand brand, MultipartFile file) throws IOException {
        // Check for duplicate brand name
        if (brandRepository.existsByNameIgnoreCaseAndDeletedFalse(brand.getName())) {
            throw new RuntimeException("Brand already exists!");
        }

        // Initialize brand status
        brand.setDeleted(false);
        brand.setActive(true);

        // Handle image upload
        if (file != null && !file.isEmpty()) {
            brand.setImageName(storeImage(file)); // store image and set filename
        }

        Brand saved = brandRepository.save(brand);
        sendEvent("BRAND_CREATED", saved); // log or publish event
        return saved;
    }

    // ----------------------------------- UPDATE BRAND -----------------------------------
    /**
     * Updates an existing brand.
     * Updates name, active status, and optionally image.
     * Sends a "BRAND_UPDATED" event after saving.
     *
     * @param brand Brand entity with updated values
     * @param file  Optional new image file
     * @return Updated Brand entity
     * @throws IOException if image upload fails
     */
    @Override
    public Brand updateBrand(Brand brand, MultipartFile file) throws IOException {
        Brand existing = brandRepository.findById(brand.getId())
                .orElseThrow(() -> new RuntimeException("Brand not found"));

        // Update fields
        existing.setName(brand.getName());
        existing.setActive(brand.isActive());

        // Update image if provided
        if (file != null && !file.isEmpty()) {
            existing.setImageName(storeImage(file));
        }

        Brand updated = brandRepository.save(existing);
        sendEvent("BRAND_UPDATED", updated);
        return updated;
    }

    // ----------------------------------- SOFT DELETE BRAND -----------------------------------
    /**
     * Soft deletes a brand by setting the "deleted" flag to true.
     * Sends a "BRAND_DELETED" event after saving.
     *
     * @param id ID of the brand to delete
     * @return Soft-deleted Brand entity
     */
    @Override
    public Brand softDeleteBrand(Long id) {
        Brand existing = brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Brand not found"));

        existing.setDeleted(true);
        Brand deleted = brandRepository.save(existing);
        sendEvent("BRAND_DELETED", deleted);
        return deleted;
    }

    // ----------------------------------- LIST BRANDS WITH PAGINATION & SEARCH -----------------------------------
    /**
     * Retrieves paginated list of active brands, optionally filtered by search query.
     * Converts Brand entities to BrandDto and returns a PageBrandDto with pagination metadata.
     *
     * @param q    Optional search query for brand name
     * @param page Page number (0-based)
     * @param size Page size
     * @return PageBrandDto containing list of BrandDto and pagination info
     */
    @Override
    public PageBrandDto listBrands(String q, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

        Page<Brand> pageResult;
        if (q == null || q.isEmpty()) {
            pageResult = brandRepository.findAllByDeletedFalseAndActiveTrue(pageable);
        } else {
            pageResult = brandRepository.searchActive(q, pageable);
        }

        // Convert Brand entities to DTOs
        List<BrandDto> content = pageResult.getContent().stream().map(b -> {
            BrandDto dto = new BrandDto();
            dto.setId(b.getId());
            dto.setName(b.getName());
            dto.setImageName(b.getImageName());
            dto.setActive(b.isActive());
            return dto;
        }).collect(Collectors.toList());

        // Build PageBrandDto with pagination metadata
        PageBrandDto out = new PageBrandDto();
        out.setContent(content);
        out.setTotalElements(pageResult.getTotalElements());
        out.setTotalPages(pageResult.getTotalPages());
        out.setNumber(pageResult.getNumber());
        out.setSize(pageResult.getSize());
        out.setFirst(pageResult.isFirst());
        out.setLast(pageResult.isLast());

        return out;
    }

    // ----------------------------------- GET BRAND BY ID -----------------------------------
    /**
     * Retrieves a brand by ID if it is not soft-deleted.
     *
     * @param id Brand ID
     * @return Optional containing the Brand entity or empty if not found/deleted
     */
    @Override
    public Optional<Brand> getBrandById(Long id) {
        return brandRepository.findById(id)
                .filter(brand -> !brand.isDeleted());
    }

    // ----------------------------------- HELPER METHOD: STORE IMAGE -----------------------------------
    /**
     * Stores the uploaded image file on disk under "uploads/brands".
     * Prepends current timestamp to filename to avoid collisions.
     *
     * @param file MultipartFile to store
     * @return Stored filename
     * @throws IOException if saving file fails
     */
    private String storeImage(MultipartFile file) throws IOException {
        String uploadDir = "uploads/brands";
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
     * Sends a simple event with brand details.
     * Currently implemented as a stub; can be integrated with messaging system or logging.
     *
     * @param eventType Event type string (e.g., BRAND_CREATED, BRAND_UPDATED, BRAND_DELETED)
     * @param brand     Brand entity involved in the event
     */
    private void sendEvent(String eventType, Brand brand) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", eventType);
        event.put("id", brand.getId());
        event.put("name", brand.getName());
        event.put("deleted", brand.isDeleted());
        event.put("active", brand.isActive());
        event.put("timestamp", System.currentTimeMillis());

        // TODO: Integrate with Kafka, RabbitMQ, or log system if needed
    }
}
