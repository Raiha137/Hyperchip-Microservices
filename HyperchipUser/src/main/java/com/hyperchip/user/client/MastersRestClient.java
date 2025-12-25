//package com.hyperchip.user.client;
//
//import com.hyperchip.user.dto.PageResponseDto;
//import com.hyperchip.user.dto.ProductDto;
//import com.hyperchip.user.dto.CategoryDto;
//import com.hyperchip.user.dto.BrandDto;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Component;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.Arrays;
//import java.util.List;
//
//@Component
//@RequiredArgsConstructor
//public class MastersRestClient {
//
//    private final RestTemplate restTemplate = new RestTemplate();
//    private final String mastersBaseUrl = "http://localhost:8086/api";
//
//
//    // ✅ Get product by ID
//    public ProductDto getProductById(Long id) {
//        return restTemplate.getForObject(
//                mastersBaseUrl + "/products/" + id,
//                ProductDto.class
//        );
//    }
//
//    // ✅ Search + filter products (with pagination)
//    public PageResponseDto<ProductDto> searchAndFilterProducts(
//            int pageNo, int pageSize,
//            String category, String brand,
//            String keyword, Double minPrice,
//            Double maxPrice, String sort) {
//
//        String url = mastersBaseUrl + "/products/search?"
//                + "pageNo=" + pageNo
//                + "&pageSize=" + pageSize
//                + (category != null ? "&category=" + category : "")
//                + (brand != null ? "&brand=" + brand : "")
//                + (keyword != null ? "&keyword=" + keyword : "")
//                + "&minPrice=" + minPrice
//                + "&maxPrice=" + maxPrice
//                + (sort != null ? "&sort=" + sort : "");
//
//        return restTemplate.getForObject(url, PageResponseDto.class);
//    }
//
//    // ✅ Active categories
//    public List<CategoryDto> getAllActiveCategory() {
//        CategoryDto[] result = restTemplate.getForObject(
//                mastersBaseUrl + "/categories/active",
//                CategoryDto[].class
//        );
//        return Arrays.asList(result);
//    }
//
//    // ✅ Active brands
//    public List<BrandDto> getAllActiveBrands() {
//        BrandDto[] result = restTemplate.getForObject(
//                mastersBaseUrl + "/brands/active",
//                BrandDto[].class
//        );
//        return Arrays.asList(result);
//    }
//}
