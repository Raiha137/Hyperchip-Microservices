package com.hyperchip.user.service.impl;

import com.hyperchip.common.dto.PageProductDto;
import com.hyperchip.common.dto.ProductDto;
import com.hyperchip.user.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

/**
 * Service implementation for fetching product data from the Master service.
 * Provides methods to fetch all products, fetch by ID, search/filter products, and get latest products.
 */
@Service
@RequiredArgsConstructor // Generates constructor for final fields
public class ProductServiceImpl implements ProductService {

    /**
     * RestTemplate to make HTTP requests to the Master service.
     */
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Base URL of the Master service.
     * Default fallback is localhost for local development.
     */
    @Value("${master.service.url:http://localhost:8086}")
    private String masterBase;

    /**
     * Fetch all products from Master service.
     *
     * Purpose:
     * - Retrieves a large list of products (up to 1000) to display in user-facing modules.
     * - Returns empty list if Master service is unreachable or response is null.
     *
     * @return List of ProductDto objects
     */
    @Override
    public List<ProductDto> getAllProducts() {
        try {
            PageProductDto page = restTemplate.getForObject(
                    masterBase + "/public/products?page=0&size=1000", PageProductDto.class
            );

            return page != null && page.getContent() != null
                    ? page.getContent()
                    : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * Fetch a single product by its ID.
     *
     * Purpose:
     * - Retrieve detailed information about a single product for product detail pages.
     * - Returns null if product is not found or Master service fails.
     *
     * @param id Product ID
     * @return ProductDto object or null
     */
    @Override
    public ProductDto getProductById(Long id) {
        try {
            return restTemplate.getForObject(
                    masterBase + "/public/products/" + id, ProductDto.class
            );
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Search and filter products with multiple optional parameters.
     *
     * Purpose:
     * - Allows keyword search, category, brand, price range, pagination, and sorting.
     * - If keyword is numeric, it is treated as exact price search.
     * - Provides safe fallback PageProductDto if Master service fails or response is null.
     *
     * @param keyword      Search keyword (name or numeric price)
     * @param categoryName Category name filter
     * @param brandName    Brand name filter
     * @param minPrice     Minimum price filter
     * @param maxPrice     Maximum price filter
     * @param page         Page number (pagination)
     * @param size         Page size (pagination)
     * @param sortBy       Field to sort by
     * @param sortDir      Sorting direction ("asc" or "desc")
     * @return PageProductDto containing filtered products
     */
    @Override
    public PageProductDto searchProducts(String keyword,
                                         String categoryName,
                                         String brandName,
                                         Double minPrice,
                                         Double maxPrice,
                                         int page,
                                         int size,
                                         String sortBy,
                                         String sortDir) {

        // normalize incoming params before building URL
        final java.util.function.Function<String, String> normalizeParam = s -> {
            if (s == null) return null;
            try { s = java.net.URLDecoder.decode(s, java.nio.charset.StandardCharsets.UTF_8.name()); } catch (Exception ignored) {}
            s = s.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
            return s.isBlank() ? null : s;
        };

        String keywordNorm = normalizeParam.apply(keyword);
        String categoryNorm = normalizeParam.apply(categoryName);
        String brandNorm = normalizeParam.apply(brandName);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(masterBase + "/public/products")
                .queryParam("page", page)
                .queryParam("size", size)
                .queryParam("sortBy", sortBy != null ? sortBy : "id")
                .queryParam("sortDir", sortDir != null ? sortDir : "desc");

// Keyword handling: numeric = price filter, else text search
        if (keywordNorm != null && !keywordNorm.isBlank()) {
            String cleaned = keywordNorm.replaceAll("[^0-9\\.\\-]", "");
            if (!cleaned.isBlank()) {
                try {
                    Double numeric = Double.parseDouble(cleaned);
                    builder.queryParam("minPrice", numeric);
                    builder.queryParam("maxPrice", numeric);
                } catch (NumberFormatException e) {
                    builder.queryParam("keyword", keywordNorm);
                }
            } else {
                builder.queryParam("keyword", keywordNorm);
            }
        }

// Add optional filters if provided
        if (categoryNorm != null && !categoryNorm.isBlank()) builder.queryParam("categoryName", categoryNorm);
        if (brandNorm != null && !brandNorm.isBlank()) builder.queryParam("brandName", brandNorm);
        if (minPrice != null) builder.queryParam("minPrice", minPrice);
        if (maxPrice != null) builder.queryParam("maxPrice", maxPrice);
        String url = builder.toUriString();
        System.out.println("CALLING MASTER PRODUCTS => " + url); // debug log

        try {
            PageProductDto response = restTemplate.getForObject(url, PageProductDto.class);

            if (response == null) {
                // Return an empty PageProductDto with pagination defaults
                PageProductDto empty = new PageProductDto();
                empty.setContent(Collections.emptyList());
                empty.setTotalPages(0);
                empty.setNumber(0);
                empty.setSize(size);
                empty.setTotalElements(0);
                empty.setFirst(true);
                empty.setLast(true);
                return empty;
            }
            return response;
        } catch (Exception ex) {
            ex.printStackTrace();
            // Return empty page on error
            PageProductDto empty = new PageProductDto();
            empty.setContent(Collections.emptyList());
            empty.setTotalPages(0);
            empty.setNumber(0);
            empty.setSize(size);
            empty.setTotalElements(0);
            empty.setFirst(true);
            empty.setLast(true);
            return empty;
        }
    }

    /**
     * Fetch latest products based on creation date.
     *
     * Purpose:
     * - Returns a limited list of most recently added products.
     * - Used in "New Arrivals" sections on the homepage or product listing pages.
     *
     * @param limit Maximum number of products to fetch
     * @return List of latest ProductDto objects
     */
    @Override
    public List<ProductDto> getLatestProducts(int limit) {
        try {
            String url = masterBase + "/public/products?page=0&size=" + limit
                    + "&sortBy=createdAt&sortDir=desc";

            PageProductDto page = restTemplate.getForObject(url, PageProductDto.class);

            return (page != null && page.getContent() != null)
                    ? page.getContent()
                    : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
    @Override
    public List<String> getAllCategories() {
        // Fetch all products and extract distinct category names
        return getAllProducts().stream()
                .map(ProductDto::getCategoryName)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }

    @Override
    public List<String> getAllBrands() {
        return getAllProducts().stream()
                .map(ProductDto::getBrandName)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }
}
