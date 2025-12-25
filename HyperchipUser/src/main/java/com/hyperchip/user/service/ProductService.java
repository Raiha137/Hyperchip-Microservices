package com.hyperchip.user.service;

import com.hyperchip.common.dto.PageProductDto;
import com.hyperchip.common.dto.ProductDto;

import java.util.List;

/**
 * Service interface for fetching and searching product data for user-facing modules.
 *
 * Purpose:
 * - Defines the contract for product-related operations.
 * - Supports retrieving products, searching/filtering, fetching details, and getting latest products.
 * - Promotes loose coupling between controllers and service implementations.
 */
public interface ProductService {

    /**
     * Retrieve all products.
     *
     * Purpose:
     * - Fetches a complete list of products for general display or caching.
     * - Implementation may fetch data from a master service or database.
     *
     * @return List of ProductDto representing all products
     */
    List<ProductDto> getAllProducts();

    /**
     * Retrieve a single product by its ID.
     *
     * Purpose:
     * - Provides detailed information about a product for product detail pages.
     * - Returns null if product is not found.
     *
     * @param id Product ID
     * @return ProductDto or null if not found
     */
    ProductDto getProductById(Long id);

    /**
     * Search and filter products with multiple optional parameters.
     *
     * Purpose:
     * - Supports keyword search, category, brand, price range, pagination, and sorting.
     * - Allows flexible querying of products for listing pages.
     *
     * @param keyword      Keyword for text search or numeric price
     * @param categoryName Filter by category
     * @param brandName    Filter by brand
     * @param minPrice     Minimum price filter
     * @param maxPrice     Maximum price filter
     * @param page         Page number (pagination)
     * @param size         Page size (pagination)
     * @param sortBy       Field to sort by
     * @param sortDir      Sorting direction ("asc" or "desc")
     * @return PageProductDto containing the search results
     */
    PageProductDto searchProducts(String keyword,
                                  String categoryName,
                                  String brandName,
                                  Double minPrice,
                                  Double maxPrice,
                                  int page,
                                  int size,
                                  String sortBy,
                                  String sortDir);

    /**
     * Retrieve the latest products based on creation date.
     *
     * Purpose:
     * - Returns a limited list of most recently added products.
     * - Used in "New Arrivals" sections on homepages or product listing pages.
     *
     * @param limit Maximum number of products to fetch
     * @return List of latest ProductDto objects
     */
    List<ProductDto> getLatestProducts(int limit);
    List<String> getAllCategories();
    List<String> getAllBrands();

}
