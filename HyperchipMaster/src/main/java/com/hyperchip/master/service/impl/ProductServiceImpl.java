package com.hyperchip.master.service.impl;

import com.hyperchip.common.dto.*;
import com.hyperchip.master.model.Brand;
import com.hyperchip.master.model.Category;
import com.hyperchip.master.model.Product;
import com.hyperchip.master.repository.BrandRepository;
import com.hyperchip.master.repository.CategoryRepository;
import com.hyperchip.master.repository.ProductRepository;
import com.hyperchip.master.service.OfferService;
import com.hyperchip.master.service.ProductService;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ProductServiceImpl provides business logic for product management.
 *
 * Responsibilities include:
 *  - Creating and updating Product entities
 *  - Handling image processing (crop/resize/save)
 *  - Listing and filtering products with pagination
 *  - Converting Product -> ProductDto and integrating with OfferService
 *  - Simple stock management helpers
 *  - Emitting product events (placeholder)
 *
 * Note: Payment-related methods are intentionally commented out and belong
 * to a dedicated PaymentService. See commented section near the bottom.
 */
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final OfferService offerService; // used to calculate best applicable offer for a product
    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    @Value("${upload.dir:${upload.products.dir:uploads/products}}")
    private String uploadDir;

    private static final int TARGET_IMAGE_SIZE = 600; // px (square)
    private static final String IMAGE_FORMAT = "jpg"; // output image format

    // --------------------- SAVE (CREATE / UPDATE) PRODUCT ---------------------

    /**
     * Save or update a product and optional images.
     *
     * Purpose: Persist a Product entity using the incoming DTO and
     * optionally process and save uploaded images to the filesystem.
     *
     * Usage:
     *  - If productDto.id is null -> create new Product.
     *  - Else -> load existing Product and update fields.
     *  - Uploaded files are center-cropped, resized to TARGET_IMAGE_SIZE and
     *    saved to uploadDir. Filenames are stored on the Product entity.
     *
     * @param productDto Product data to save
     * @param uploadedImages array of images from controller; can be null
     * @return ProductDto representation of saved product
     * @throws IOException if image processing or file write fails
     */
    @Override
    @Transactional
    public ProductDto saveProduct(ProductDto productDto, MultipartFile[] uploadedImages) throws IOException {
        Product product;

        // Load existing product when id provided, otherwise create new
        if (productDto.getId() != null) {
            product = productRepository.findById(productDto.getId()).orElseGet(Product::new);
        } else {
            product = new Product();
            product.setDeleted(false);
            product.setActive(productDto.getActive() != null ? productDto.getActive() : true);
        }

        // --- Populate core fields from DTO ---
        product.setTitle(productDto.getTitle());
        product.setDescription(productDto.getDescription());
        product.setPrice(productDto.getPrice());
        // product.setDiscount(productDto.getDiscount()); // if using discount field
        product.setStock(productDto.getStock());
        product.setActive(productDto.getActive() != null ? productDto.getActive() : true);
        product.setDeleted(productDto.getDeleted() != null ? productDto.getDeleted() : false);

        // --- Resolve category and brand references ---
        if (productDto.getCategoryId() != null) {
            Category category = categoryRepository.findById(productDto.getCategoryId()).orElse(null);
            product.setCategory(category);
        } else {
            product.setCategory(null);
        }

        if (productDto.getBrandId() != null) {
            Brand brand = brandRepository.findById(productDto.getBrandId()).orElse(null);
            product.setBrand(brand);
        } else {
            product.setBrand(null);
        }

        // Ensure upload directory exists
        Path uploadDirPath = Paths.get(uploadDir != null && !uploadDir.isBlank() ? uploadDir : "uploads/products");
        if (!Files.exists(uploadDirPath)) {
            Files.createDirectories(uploadDirPath);
        }

        // Keep existing images and append new ones
        List<String> filenames = product.getImages() != null ? new ArrayList<>(product.getImages()) : new ArrayList<>();

        // Process uploaded images: crop center, resize, save as jpg, add filenames
        if (uploadedImages != null) {
            for (MultipartFile mf : uploadedImages) {
                if (mf == null || mf.isEmpty()) continue;

                byte[] processed = cropCenterAndResize(mf.getInputStream(), TARGET_IMAGE_SIZE, TARGET_IMAGE_SIZE);

                String original = mf.getOriginalFilename() != null ? mf.getOriginalFilename() : ("img-" + System.currentTimeMillis() + ".jpg");
                String safeName = UUID.randomUUID().toString() + "-" + original.replaceAll("\\s+", "_");
                if (!safeName.toLowerCase().endsWith(".jpg") && !safeName.toLowerCase().endsWith(".jpeg")) {
                    safeName += ".jpg";
                }

                Path target = uploadDirPath.resolve(safeName);
                Files.write(target, processed, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                filenames.add(safeName);
            }
        }

        // Set thumbnail and images list on product
        if (!filenames.isEmpty()) {
            product.setImageName(filenames.get(0));
            product.setImages(filenames);
        }

        // Persist product entity
        Product saved = productRepository.save(product);

        // Convert to DTO including offer calculation and return
        return convertToDto(saved);
    }


    // --------------------- UPDATE PRODUCT (IMAGES + REMOVALS) ---------------------

    /**
     * Update an existing product including adding new images and removing specified images.
     *
     * Purpose: Edit product metadata and manage its images atomically.
     *
     * Usage:
     *  - Provide a Product entity containing updated fields (must include id)
     *  - `removedImageNames` lists filenames to delete from disk and product record
     *  - `uploadedImages` are processed and appended
     *
     * @param product product object with updated core fields
     * @param uploadedImages new images to add
     * @param removedImageNames filenames to remove from product
     * @return updated Product entity
     * @throws IOException when image processing or deletion fails unexpectedly
     */
    @Override
    @Transactional
    public Product updateProduct(Product product, List<MultipartFile> uploadedImages, List<String> removedImageNames) throws IOException {
        Product existing = productRepository.findById(product.getId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Update core scalar fields
        updateCoreFields(existing, product);

        // Ensure upload directory exists
        Path uploadDirPath = Paths.get(uploadDir != null && !uploadDir.isBlank() ? uploadDir : "uploads/products");
        if (!Files.exists(uploadDirPath)) Files.createDirectories(uploadDirPath);

        List<String> filenames = existing.getImages() != null ? new ArrayList<>(existing.getImages()) : new ArrayList<>();

        // Remove requested image files and remove from filenames list
        if (removedImageNames != null && !removedImageNames.isEmpty()) {
            for (String rem : removedImageNames) {
                if (rem == null) continue;
                filenames.removeIf(fn -> fn.equals(rem));
                try {
                    Files.deleteIfExists(uploadDirPath.resolve(rem));
                } catch (Exception ex) {
                    // Log if desired; ignore to allow partial resilience
                }
            }
        }

        // Add new uploaded images
        if (uploadedImages != null) {
            for (MultipartFile file : uploadedImages) {
                if (file == null || file.isEmpty()) continue;
                byte[] processed = cropCenterAndResize(file.getInputStream(), TARGET_IMAGE_SIZE, TARGET_IMAGE_SIZE);
                String name = UUID.randomUUID() + "-" + (file.getOriginalFilename() != null ? file.getOriginalFilename().replaceAll("\\s+", "_") : ("img-" + System.currentTimeMillis()));
                if (!name.toLowerCase().endsWith(".jpg") && !name.toLowerCase().endsWith(".jpeg")) name += ".jpg";
                Path target = uploadDirPath.resolve(name);
                Files.write(target, processed, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                filenames.add(name);
            }
        }

        // Update image fields on entity
        if (!filenames.isEmpty()) {
            existing.setImageName(filenames.get(0));
            existing.setImages(filenames);
        } else {
            existing.setImageName(null);
            existing.setImages(Collections.emptyList());
        }

        Product updated = productRepository.save(existing);

        // Emit an update event for external consumers (analytics, search index, cache invalidation)
        sendEvent("PRODUCT_UPDATED", updated);
        return updated;
    }


    // --------------------- UPDATE WITHOUT IMAGE CHANGES ---------------------

    /**
     * Update only core product fields (no image handling).
     *
     * Purpose: Use when front-end only edits textual/numeric fields and images
     * remain unchanged. This avoids touching the filesystem.
     *
     * @param product product containing updated fields (must have id)
     * @return updated Product entity
     */
    @Override
    @Transactional
    public Product updateProductWithoutImages(Product product) {
        Product existing = productRepository.findById(product.getId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        updateCoreFields(existing, product);
        Product updated = productRepository.save(existing);
        sendEvent("PRODUCT_UPDATED", updated);
        return updated;
    }


    // --------------------- SOFT DELETE ---------------------

    /**
     * Soft delete a product by setting deleted=true. Does not remove images.
     *
     * Purpose: Keep historical data while hiding product from user-facing pages.
     * Use when you want reversible deletion and to preserve references.
     *
     * @param id product id
     * @return soft-deleted product entity
     */
    @Override
    @Transactional
    public Product softDeleteProduct(Long id) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        existing.setDeleted(true);
        Product deleted = productRepository.save(existing);
        sendEvent("PRODUCT_DELETED", deleted);
        return deleted;
    }


    // --------------------- LISTING / FILTERING ---------------------

    /**
     * List products with optional filters and pagination.
     *
     * Purpose: Central place to fetch paginated products based on search
     * keyword, category, brand, and price range. Delegates filtering to repository
     * which contains the JPQL optimized for these conditions.
     *
     * @return a Page of Product matching filters
     */
    @Override
    public Page<Product> listProducts(String keyword, Category category, Brand brand,
                                      Double minPrice, Double maxPrice,
                                      int page, int size, String sortBy, String sortDir) {

        Sort sort = Sort.by(sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);


        String categoryName = normalizeName(category != null ? category.getName() : null);
        String brandName    = normalizeName(brand != null ? brand.getName() : null);
        String keywordNorm  = normalizeName(keyword);

        log.info("FILTERS -> category='{}' | brand='{}' | keyword='{}' | min={} | max={}",
                categoryName, brandName, keywordNorm, minPrice, maxPrice);

        return productRepository.searchWithFilters(
                keywordNorm != null && !keywordNorm.isBlank() ? keywordNorm : null,
                categoryName  != null && !categoryName.isBlank() ? categoryName : null,
                brandName     != null && !brandName.isBlank() ? brandName : null,
                minPrice,
                maxPrice,
                pageable
        );


    }


    // --------------------- READ / GET ---------------------

    /**
     * Get product by id only if active and not deleted.
     *
     * Purpose: Provide a safe lookup for controllers that should only show
     * available products to end users.
     */
    @Override
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id)
                .filter(p -> !p.isDeleted() && p.isActive());
    }


    /**
     * Check if a non-deleted product exists by title (case-insensitive).
     *
     * Purpose: Useful for validation when creating/updating products to avoid duplicates.
     */
    @Override
    public boolean existsByTitle(String title) {
        return productRepository.existsByTitleIgnoreCaseAndDeletedFalse(title);
    }


    // --------------------- HELPER / PRIVATE METHODS ---------------------

    /**
     * Copy core scalar fields from incoming product to existing product entity.
     *
     * Purpose: Single place to keep field mappings consistent between create/update flows.
     */
    private void updateCoreFields(Product existing, Product product) {
        existing.setTitle(product.getTitle());
        existing.setDescription(product.getDescription());
        existing.setPrice(product.getPrice());
        // existing.setDiscount(product.getDiscount()); // enable if discount used
        existing.setStock(product.getStock());
        existing.setCategory(product.getCategory());
        existing.setBrand(product.getBrand());
        existing.setActive(product.isActive());
        existing.setDeleted(product.isDeleted());
    }

    /**
     * Crop the center square of the source image and resize to the target dimensions.
     *
     * Purpose: Ensure consistent square thumbnails and reasonable file sizes for product images.
     */
    private byte[] cropCenterAndResize(InputStream inputStream, int targetWidth, int targetHeight) throws IOException {
        BufferedImage src = ImageIO.read(inputStream);
        if (src == null) throw new IOException("Unsupported or corrupt image.");

        int w = src.getWidth();
        int h = src.getHeight();
        int square = Math.min(w, h);
        int x = (w - square) / 2;
        int y = (h - square) / 2;
        BufferedImage cropped = src.getSubimage(x, y, square, square);

        Image scaled = cropped.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        BufferedImage out = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = out.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(scaled, 0, 0, null);
        g2d.dispose();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(out, IMAGE_FORMAT, baos);
            baos.flush();
            return baos.toByteArray();
        }
    }

    /**
     * Convert Product entity to ProductDto including best-offer calculation.
     *
     * Purpose: Assemble DTO for API responses. This method queries OfferService
     * to compute the best applicable promotional price and related metadata.
     */
    private ProductDto convertToDto(Product product) {
        ProductDto dto = new ProductDto();

        dto.setId(product.getId());
        dto.setTitle(product.getTitle());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setStock(product.getStock());

        // category / brand
        Long categoryId = (product.getCategory() != null ? product.getCategory().getId() : null);
        dto.setCategoryId(categoryId);
        dto.setCategoryName(product.getCategory() != null ? product.getCategory().getName() : null);

        Long brandId = (product.getBrand() != null ? product.getBrand().getId() : null);
        dto.setBrandId(brandId);
        dto.setBrandName(product.getBrand() != null ? product.getBrand().getName() : null);

        dto.setActive(product.isActive());
        dto.setDeleted(product.isDeleted());
        dto.setImageNames(product.getImages());

        // ---------- BEST OFFER ----------
        if (product.getPrice() != null) {
            BigDecimal base = BigDecimal.valueOf(product.getPrice());
            dto.setOriginalPrice(base);

            try {
                BestOfferResponse best = offerService.calculateBestOffer(
                        product.getId(),
                        categoryId,
                        base
                );

                if (best != null && best.getFinalPrice() != null) {
                    dto.setOfferDiscount(best.getDiscountAmount());
                    dto.setFinalPrice(best.getFinalPrice());
                    dto.setAppliedScope(best.getAppliedScope());
                    dto.setAppliedOfferId(best.getAppliedOfferId());
                } else {
                    dto.setFinalPrice(base);
                    dto.setOfferDiscount(BigDecimal.ZERO);
                    dto.setAppliedScope(OfferScope.NONE);
                    dto.setAppliedOfferId(null);
                }
            } catch (Exception ex) {
                // On failure, fall back to base price
                dto.setFinalPrice(base);
                dto.setOfferDiscount(BigDecimal.ZERO);
                dto.setAppliedScope(OfferScope.NONE);
                dto.setAppliedOfferId(null);
            }
        }

        return dto;
    }

    /**
     * Send lightweight product event for external consumers (search indexing, cache eviction, analytics).
     *
     * Purpose: Decouple side-effects from synchronous request by producing an event.
     * Implementation should integrate with Kafka/RabbitMQ/other messaging.
     */
    private void sendEvent(String eventType, Product product) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", eventType);
        event.put("id", product.getId());
        event.put("title", product.getTitle());
        event.put("deleted", product.isDeleted());
        event.put("active", product.isActive());
        event.put("timestamp", System.currentTimeMillis());
        // TODO: connect to message broker (Kafka / RabbitMQ / etc.)
    }


    // --------------------- DTO LIST / PAGINATED DTO ---------------------

    @Override
    public ProductDto getProductDtoById(Long id) {
        Product product = productRepository.findById(id)
                .filter(p -> !p.isDeleted() && p.isActive())
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return convertToDto(product);
    }

    @Override
    public List<ProductDto> listProductDtos(String keyword, Category category, Brand brand,
                                            Double minPrice, Double maxPrice,
                                            int page, int size, String sortBy, String sortDir) {
        Page<Product> productsPage = listProducts(keyword, category, brand, minPrice, maxPrice, page, size, sortBy, sortDir);
        return productsPage.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public PageProductDto listProductDtosPage(String keyword, Category category, Brand brand,
                                              Double minPrice, Double maxPrice, int page, int size,
                                              String sortBy, String sortDir) {
        Page<Product> productsPage = listProducts(keyword, category, brand, minPrice, maxPrice, page, size, sortBy, sortDir);

        List<ProductDto> content = productsPage.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        PageProductDto dto = new PageProductDto();
        dto.setContent(content);
        dto.setTotalPages(productsPage.getTotalPages());
        dto.setNumber(productsPage.getNumber());
        dto.setSize(productsPage.getSize());
        dto.setTotalElements(productsPage.getTotalElements());
        dto.setFirst(productsPage.isFirst());
        dto.setLast(productsPage.isLast());

        return dto;
    }


    // --------------------- STOCK MANAGEMENT ---------------------

    /**
     * Increase stock for a given product.
     *
     * Purpose: Simple helper to adjust inventory (e.g., from purchase returns or admin restock).
     */
    @Transactional
    public void incrementStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setStock((product.getStock() == null ? 0 : product.getStock()) + quantity);
        productRepository.save(product);
    }

    /**
     * Decrease stock for a given product (never negative).
     *
     * Purpose: Reduce inventory when orders are placed. Ensures stock floor at 0.
     */
    @Transactional
    public void decrementStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        int newStock = (product.getStock() == null ? 0 : product.getStock()) - quantity;
        product.setStock(Math.max(newStock, 0));
        productRepository.save(product);
    }


    // --------------------- IMAGE REMOVAL ---------------------

    /**
     * Remove image file from disk and update product image list.
     *
     * Purpose: Support explicit image deletion API for admins.
     */
    @Override
    public void removeImageFromProduct(Product product, String filename) throws IOException {
        if (product == null || filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("Product or filename cannot be null or empty");
        }

        Path uploadDirPath = Paths.get(uploadDir != null && !uploadDir.isBlank() ? uploadDir : "uploads/products");
        Path imagePath = uploadDirPath.resolve(filename);

        if (Files.exists(imagePath)) {
            Files.delete(imagePath);
            System.out.println("Deleted image file: " + imagePath);
        } else {
            System.out.println("Image file not found: " + imagePath);
        }

        if (product.getImages() != null && product.getImages().contains(filename)) {
            product.getImages().remove(filename);
        }

        productRepository.save(product);
    }
    private String normalizeName(String s) {
        if (s == null) return null;
        try {
            // URL-decode if needed
            s = java.net.URLDecoder.decode(s, java.nio.charset.StandardCharsets.UTF_8.name());
        } catch (Exception ignored) {}
        // replace non-breaking space with normal space, collapse multiple spaces, trim
        s = s.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
        // fix common HTML-encoding artifacts that might come through UI params
        s = s.replace("&amp;", "&");
        return s.isBlank() ? null : s;
    }
}
