package com.hyperchip.user.service.impl;

import com.hyperchip.common.dto.CategoryDto;
import com.hyperchip.common.dto.PageCategoryDto;
import com.hyperchip.user.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Service implementation for fetching categories from the Master service.
 * This service acts as a client to the master service and provides user-facing category data.
 */
@Service
@RequiredArgsConstructor // Lombok annotation to generate constructor for final fields
public class CategoryServiceImpl implements CategoryService {

    /**
     * RestTemplate to make HTTP requests to the Master service.
     */
    private final RestTemplate restTemplate;

    /**
     * Base URL of the Master service.
     * Injected from application properties (e.g., application.yml or application.properties).
     */
    @Value("${master.service.url}")
    private String masterBaseUrl;

    /**
     * Fetch all categories from the Master service.
     *
     * Purpose:
     * - Calls the master service admin API to retrieve all categories.
     * - Converts the paged response into a list of CategoryDto objects.
     * - Handles null responses safely by returning an empty list.
     *
     * Usage:
     * - Used in user-facing modules to display category lists in menus, filters, or dropdowns.
     *
     * @return List of CategoryDto containing all categories
     */
    @Override
    public List<CategoryDto> getAllCategories() {
        // Construct URL to fetch categories with a large page size
        String url = masterBaseUrl + "/api/admin/categories?page=0&size=100";
        System.out.println("DEBUG - CategoryServiceImpl calling: " + url);

        // Make GET request to the master service and map response to PageCategoryDto
        PageCategoryDto page = restTemplate.getForObject(url, PageCategoryDto.class);

        // Return empty list if response or content is null
        if (page == null || page.getContent() == null) {
            return List.of();
        }

        // Return the actual list of categories
        return page.getContent();
    }
}
