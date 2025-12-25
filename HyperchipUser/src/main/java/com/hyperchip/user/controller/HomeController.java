package com.hyperchip.user.controller;

import com.hyperchip.common.dto.ProductDto;
import com.hyperchip.common.dto.CategoryDto;
import com.hyperchip.user.service.ProductService;
import com.hyperchip.user.service.CategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * HomeController
 * --------------
 * Handles the home page of the e-commerce application.
 *
 * Purpose:
 * - Load all categories and products for display on the home page
 * - Prepare model attributes for Thymeleaf templates
 * - Log basic statistics for debugging and monitoring
 */
@Controller
public class HomeController {

    private final Logger log = LoggerFactory.getLogger(HomeController.class);

    /**
     * Service to fetch product data.
     */
    private final ProductService productService;

    /**
     * Service to fetch category data.
     */
    private final CategoryService categoryService;

    public HomeController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    /**
     * home
     * ----
     * GET / or /user/home
     *
     * Purpose:
     * - Fetch all categories and products from the service layer
     * - Add them to the model for rendering on the home page
     * - Log the number of categories and products loaded
     */
    @GetMapping({"/", "/user/home"})
    public String home(Model model) {
        List<CategoryDto> categories = categoryService.getAllCategories();
        List<ProductDto> products = productService.getAllProducts();

        log.info("HomeController: categories.size={} products.size={}"
                , categories == null ? 0 : categories.size()
                , products == null ? 0 : products.size());

        model.addAttribute("categories", categories);
        model.addAttribute("products", products);

        return "user/index"; // returns Thymeleaf template for home page
    }
}