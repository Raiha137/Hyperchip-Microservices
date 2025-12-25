package com.hyperchip.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperchip.common.dto.*;
import com.hyperchip.common.util.MultipartInputStreamFileResource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * ProductController
 *
 * Purpose:
 * Handles Admin-side operations for managing Products in Hyperchip.
 * - List, add, edit, delete, and reset products
 * - Supports image upload, crop, and resize
 * - Fetches categories and brands from Master service
 */
@Controller
@RequestMapping("/admin/products")
public class ProductController {

    private static final int TARGET_IMAGE_SIZE = 600; // Standard image size for uploaded product images

    @Value("${api.base.url}")
    private String API_BASE; // Base API URL for Admin/Product service, e.g., http://localhost:8086/api/admin

    private final RestTemplate rest = new RestTemplate();

    // ===================================================
    // LIST PRODUCTS PAGE
    // ===================================================
    /**
     * Purpose:
     * Display all products in paginated and searchable list for admin.
     *
     * What it does:
     * - Fetches paginated products from the Product API
     * - Supports search by keyword and sorting by creation date
     * - Adds categories and brands for filter dropdowns in the UI
     */
    @GetMapping
    public String listProducts(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size,
            @RequestParam(name = "sort", defaultValue = "desc") String sort,
            Model model,
            HttpServletResponse response
    ) {
        // Disable caching for admin pages
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        String url = API_BASE + "/products?page=" + page + "&size=" + size + "&sortBy=createdAt&sortDir=" + sort;
        if (q != null && !q.trim().isEmpty()) {
            url += "&keyword=" + URLEncoder.encode(q.trim(), StandardCharsets.UTF_8);
        }

        PageProductDto pageProducts;
        try {
            pageProducts = rest.getForObject(url, PageProductDto.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            pageProducts = new PageProductDto();
        }

        if (pageProducts.getContent() == null) pageProducts.setContent(Collections.emptyList());

        model.addAttribute("products", pageProducts);
        model.addAttribute("q", q);

        addCategoriesAndBrandsToModel(model); // Add dropdown options

        return "admin/product-list";
    }

    // ===================================================
    // SHOW ADD PRODUCT FORM
    // ===================================================
    /**
     * Purpose:
     * Display form to create a new product.
     *
     * What it does:
     * - Adds empty ProductDto object for form binding
     * - Adds categories and brands for dropdowns
     */
    @GetMapping("/add")
    public String showAddProductForm(Model model) {
        model.addAttribute("product", new ProductDto());
        addCategoriesAndBrandsToModel(model);
        return "admin/product-form";
    }

    // ===================================================
    // SHOW EDIT PRODUCT FORM
    // ===================================================
    /**
     * Purpose:
     * Display form to edit an existing product.
     *
     * What it does:
     * - Fetches the product by ID
     * - Populates categories and brands for dropdowns
     * - Handles error if product cannot be fetched
     */
    @GetMapping("/edit/{id}")
    public String editProduct(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            ProductDto product = rest.getForObject(API_BASE + "/products/" + id, ProductDto.class);
            model.addAttribute("product", product);
            addCategoriesAndBrandsToModel(model);
            return "admin/product-form";
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMsg", "Unable to load product for editing.");
            return "redirect:/admin/products";
        }
    }

    // ===================================================
    // SAVE PRODUCT (CREATE or UPDATE)
    // ===================================================
    /**
     * Purpose:
     * Save or update product including images.
     *
     * What it does:
     * - Validates that at least one image exists for new products
     * - Supports uploading new images and removing old ones
     * - Crops and resizes images before upload
     * - Calls Product API to save or update
     * - Adds success or error messages
     */
    @PostMapping("/save")
    public String saveProduct(@ModelAttribute("product") ProductDto product,
                              @RequestParam(name = "uploadedImages", required = false) List<MultipartFile> uploadedImages,
                              @RequestParam(name = "removedImageNames", required = false) List<String> removedImageNames,
                              RedirectAttributes redirectAttributes,
                              Model model) {

        boolean isNew = (product.getId() == null);
        boolean hasNewImages = uploadedImages != null && uploadedImages.stream().anyMatch(f -> f != null && !f.isEmpty());

        // Validation: New products must have at least one image
        if (isNew && !hasNewImages) {
            redirectAttributes.addFlashAttribute("errorMsg", "Please upload at least 1 image for a new product.");
            addCategoriesAndBrandsToModel(model);
            model.addAttribute("product", product);
            return "admin/product-form";
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();

            ObjectMapper mapper = new ObjectMapper();
            String productJson = mapper.writeValueAsString(product);
            HttpHeaders jsonHeaders = new HttpHeaders();
            jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
            map.add("product", new HttpEntity<>(productJson, jsonHeaders));

            if (product.getCategoryId() != null) map.add("categoryId", String.valueOf(product.getCategoryId()));
            if (product.getBrandId() != null) map.add("brandId", String.valueOf(product.getBrandId()));

            // Add removed images
            if (removedImageNames != null) removedImageNames.forEach(rn -> map.add("removedImageNames", rn));

            // Process new images (crop & resize)
            if (hasNewImages) {
                for (MultipartFile mf : uploadedImages) {
                    if (mf == null || mf.isEmpty()) continue;
                    byte[] processed = cropCenterAndResize(mf.getInputStream(), TARGET_IMAGE_SIZE, TARGET_IMAGE_SIZE);
                    String name = mf.getOriginalFilename() != null ? mf.getOriginalFilename() : ("img-" + System.currentTimeMillis() + ".jpg");
                    if (!name.toLowerCase().endsWith(".jpg") && !name.toLowerCase().endsWith(".jpeg")) name += ".jpg";
                    map.add("uploadedImages", new MultipartInputStreamFileResource(new ByteArrayInputStream(processed), name));
                }
            }

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(map, headers);

            // Call API to save or update
            if (isNew) rest.postForObject(API_BASE + "/products", requestEntity, String.class);
            else rest.put(API_BASE + "/products/" + product.getId(), requestEntity, String.class);

        } catch (Exception ex) {
            ex.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to save product: " + ex.getMessage());
            addCategoriesAndBrandsToModel(model);
            model.addAttribute("product", product);
            return "admin/product-form";
        }

        String action = isNew ? "added" : "updated";
        redirectAttributes.addFlashAttribute("successMsg", (product.getTitle() != null ? product.getTitle() : "Product") + " " + action + " successfully.");
        return "redirect:/admin/products";
    }

    // ===================================================
    // DELETE PRODUCT
    // ===================================================
    /**
     * Purpose:
     * Delete product by ID.
     *
     * What it does:
     * - Calls API to delete product
     * - Adds success/error messages
     */
    @GetMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            ProductDto dto = rest.getForObject(API_BASE + "/products/" + id, ProductDto.class);
            rest.delete(API_BASE + "/products/" + id);
            redirectAttributes.addFlashAttribute("successMsg",
                    (dto != null && dto.getTitle() != null ? dto.getTitle() : "Product") + " deleted successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to delete product.");
        }
        return "redirect:/admin/products";
    }

    // ===================================================
    // RESET SEARCH
    // ===================================================
    /**
     * Purpose:
     * Reset search/filter for products page.
     */
    @GetMapping("/reset")
    public String resetSearch() {
        return "redirect:/admin/products";
    }

    // ===================================================
    // HELPER METHODS
    // ===================================================
    /**
     * Purpose:
     * Add categories and brands to the model for dropdowns in product form.
     */
    private void addCategoriesAndBrandsToModel(Model model) {
        List<CategoryDto> categories = Collections.emptyList();
        List<BrandDto> brands = Collections.emptyList();

        try {
            PageCategoryDto pageCategories = rest.getForObject(API_BASE + "/categories?page=0&size=200", PageCategoryDto.class);
            categories = pageCategories != null ? pageCategories.getContent() : Collections.emptyList();
        } catch (Exception ignored) {}

        try {
            PageBrandDto pageBrands = rest.getForObject(API_BASE + "/brands?page=0&size=200", PageBrandDto.class);
            brands = pageBrands != null ? pageBrands.getContent() : Collections.emptyList();
        } catch (Exception ignored) {}

        model.addAttribute("categories", categories);
        model.addAttribute("brands", brands);
    }

    /**
     * Purpose:
     * Crop the image to a square centered on the original image and resize to target size.
     *
     * @param in InputStream of original image
     * @param targetWidth Target width
     * @param targetHeight Target height
     * @return byte[] of processed image
     */
    private byte[] cropCenterAndResize(InputStream in, int targetWidth, int targetHeight) throws IOException {
        BufferedImage src = ImageIO.read(in);
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

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(out, "jpg", baos);
        baos.flush();
        byte[] bytes = baos.toByteArray();
        baos.close();
        return bytes;
    }
}
