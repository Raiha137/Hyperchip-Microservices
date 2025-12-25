package com.hyperchip.user.controller;

import com.hyperchip.common.dto.PageProductDto;
import com.hyperchip.common.dto.ProductDto;
import com.hyperchip.user.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * ProductController
 * ----------------
 * Handles product-related pages for the user-facing site.
 *
 * Purpose:
 * - Display product lists with filtering, sorting, and pagination
 * - Display detailed product pages with related products
 * - Support global header search
 */
@Controller
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;


    /**
     * listProducts
     * ------------
     * GET /user/products
     *
     * Purpose:
     * - List products with optional filters: keyword, category, brand, price range
     * - Supports pagination and sorting
     * - Prepares model attributes for Thymeleaf product-list page
     */
    @GetMapping("/user/products")
    public String listProducts(
            Model model,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "brand", required = false) String brand,
            @RequestParam(name = "minPrice", required = false) Double minPrice,
            @RequestParam(name = "maxPrice", required = false) Double maxPrice,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size,
            @RequestParam(name = "sort", required = false) String sort
    ) {
        // Determine sorting field and direction
        String sortBy = "id";
        String sortDir = "desc";
        if ("priceAsc".equalsIgnoreCase(sort)) { sortBy = "price"; sortDir = "asc"; }
        else if ("priceDesc".equalsIgnoreCase(sort)) { sortBy = "price"; sortDir = "desc"; }
        else if ("alphaAsc".equalsIgnoreCase(sort)) { sortBy = "title"; sortDir = "asc"; }
        else if ("alphaDesc".equalsIgnoreCase(sort)) { sortBy = "title"; sortDir = "desc"; }

        PageProductDto pageDto = productService.searchProducts(keyword, category, brand, minPrice, maxPrice, page, size, sortBy, sortDir);

        List<ProductDto> products = Optional.ofNullable(pageDto.getContent()).orElse(Collections.emptyList());

        // Add product list and pagination info
        model.addAttribute("products", products);
        model.addAttribute("page", pageDto);
        model.addAttribute("pageNo", pageDto.getNumber());
        model.addAttribute("pageSize", pageDto.getSize());
        model.addAttribute("totalPages", pageDto.getTotalPages());

        // Add filters and search parameters back to the model
        model.addAttribute("category", category);
        model.addAttribute("brand", brand);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("sort", sort);
        model.addAttribute("keyword", keyword);

        // Build dynamic category and brand lists for UI filters
        List<String> categories;
        List<String> brands;

// ✅ FIX: if category is selected, keep ONLY that category
        if (category != null && !category.isBlank()) {
            categories = List.of(category);
        } else {
            categories = productService.getAllCategories();
        }

// ✅ FIX: if brand is selected, keep ONLY that brand
        if (brand != null && !brand.isBlank()) {
            brands = List.of(brand);
        } else {
            brands = productService.getAllBrands();
        }

        model.addAttribute("categories", categories);
        model.addAttribute("brands", brands);

        return "user/product-list";
    }

    /**
     * viewProduct
     * -----------
     * GET /user/product/{id}
     *
     * Purpose:
     * - Display a single product's detail page
     * - Validate if product is active and not deleted
     * - Fetch a few related products from the same category
     */
    @GetMapping("/user/product/{id}")
    public String viewProduct(@PathVariable("id") Long id, Model model) {
        ProductDto product = productService.getProductById(id);
        if (product == null || (product.getDeleted() != null && product.getDeleted()) || (product.getActive() != null && !product.getActive())) {
            return "redirect:/user/products";
        }
        model.addAttribute("product", product);

        // Fetch related products (same category, limit 4, exclude current product)
        PageProductDto pageDto = productService.searchProducts(null, product.getCategoryName(), null, null, null, 0, 8, "id", "desc");
        List<ProductDto> related = Optional.ofNullable(pageDto.getContent()).orElse(Collections.emptyList()).stream()
                .filter(p -> p.getId() != null && !p.getId().equals(id))
                .limit(4)
                .toList();
        model.addAttribute("relatedProducts", related);

        return "user/product-detail";
    }

    /**
     * globalSearch
     * ------------
     * GET /user/search
     *
     * Purpose:
     * - Handle search requests from the global header
     * - No category/brand/price filters applied
     * - Supports pagination and basic sorting (default by id desc)
     */
    @GetMapping("/user/search")
    public String globalSearch(
            Model model,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size
    ) {
        PageProductDto pageDto = productService.searchProducts(q, null, null, null, null, page, size, "id", "desc");

        List<ProductDto> products = Optional.ofNullable(pageDto.getContent()).orElse(Collections.emptyList());

        // Add products and pagination info
        model.addAttribute("products", products);
        model.addAttribute("page", pageDto);
        model.addAttribute("pageNo", pageDto.getNumber());
        model.addAttribute("pageSize", pageDto.getSize());
        model.addAttribute("totalPages", pageDto.getTotalPages());

        // Add header search query
        model.addAttribute("q", q);

        // Clear filters as this is a global search
        model.addAttribute("category", null);
        model.addAttribute("brand", null);
        model.addAttribute("minPrice", null);
        model.addAttribute("maxPrice", null);
        model.addAttribute("sort", null);
        model.addAttribute("keyword", null);

        // Build categories and brands from results for UI filters
        List<String> categories = products.stream().map(ProductDto::getCategoryName).filter(Objects::nonNull).distinct().toList();
        List<String> brands = products.stream().map(ProductDto::getBrandName).filter(Objects::nonNull).distinct().toList();
        model.addAttribute("categories", categories);
        model.addAttribute("brands", brands);

        return "user/product-list";
    }
}