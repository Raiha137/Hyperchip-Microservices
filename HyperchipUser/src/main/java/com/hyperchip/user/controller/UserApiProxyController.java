package com.hyperchip.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperchip.common.dto.*;
import com.hyperchip.user.model.Address;
import jakarta.servlet.http.HttpSession;
import com.hyperchip.user.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Simplified proxy controller for user -> other microservices.
 * Removed direct local DB/address-model manipulation to avoid compile-time coupling.
 */
@RestController
@RequestMapping("/user/api/proxy")
@RequiredArgsConstructor
public class UserApiProxyController {

    private static final Logger log = LoggerFactory.getLogger(UserApiProxyController.class);

    private final RestTemplate restTemplate;
    private final AddressRepository addressRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();


    @Value("${cart.service.url:http://localhost:8091/api/cart}")
    private String cartServiceBase;

    @Value("${address.service.url:http://localhost:8090}")
    private String addressServiceBase;

    @Value("${order.service.url:http://localhost:8092}")
    private String orderServiceBase;

//    @Value("${order.service.url:http://localhost:8092/api/user/orders}")
//    private String orderServiceUrl;


    @Value("${payment.service.url:http://localhost:8094}")
    private String paymentServiceUrl;


    @Value("${master.service.url:http://localhost:8086}")
    private String masterServiceBase;

    @Value("${product.service.url:http://localhost:8086}")
    private String productServiceBase;

    @Value("${uploads.base.url:http://localhost:8086}")
    private String uploadsBaseUrl;


    // app-level fallback currency (can override in application.properties)
    @Value("${app.currency:INR}")
    private String appCurrency;


    private String orderBase() {
        String base = (orderServiceBase == null || orderServiceBase.isBlank())
                ? "http://localhost:8092"
                : orderServiceBase.trim();

        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        // ✅ always attach /api/orders
        return base + "/api/orders";
    }


    // CART
    @GetMapping("/cart/{userId}")
    public ResponseEntity<?> proxyCart(@PathVariable("userId") Long userId) {
        try {
            String base = (cartServiceBase == null || cartServiceBase.isBlank()) ? "http://localhost:8091/api/cart" : cartServiceBase;
            if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
            String url = base + "/" + userId;
            ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class);

            // enrich response body (add absolute productImage and price) before returning
            if (resp != null && resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> body = resp.getBody();
                    enrichCartMap(body);
                } catch (Exception e) {
                    log.debug("Failed to enrich cart response: {}", e.getMessage());
                }
            }

            return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Failed to fetch cart", "detail", ex.getMessage()));
        }
    }



    // Enrich cart map in-place: expects structure { items: [ { productId, productImage?, quantity?, unitPrice?, total? } ] }
    private void enrichCartMap(Map<String, Object> cart) {
        if (cart == null) return;
        Object itemsObj = cart.get("items");
        if (!(itemsObj instanceof Collection)) return;

        String master = productServiceBase != null ? productServiceBase.trim() : "";
        if (master.isBlank()) master = System.getProperty("master.service.url", System.getenv("MASTER_SERVICE_URL"));
        if (master == null || master.isBlank()) master = "http://localhost:8086";
        if (master.endsWith("/")) master = master.substring(0, master.length() - 1);

        for (Object obj : (Collection<?>) itemsObj) {
            if (!(obj instanceof Map)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> it = (Map<String, Object>) obj;

            Object pidObj = it.get("productId");
            Long pid = null;
            if (pidObj instanceof Number) pid = ((Number) pidObj).longValue();
            else if (pidObj instanceof String) {
                try { pid = Long.parseLong((String) pidObj); } catch (Exception ignored) {}
            }
            if (pid == null) continue;

            try {
                String prodUrl = master + "/public/products/" + pid;
                ResponseEntity<Map> pResp = restTemplate.getForEntity(prodUrl, Map.class);
                if (pResp != null && pResp.getStatusCode().is2xxSuccessful() && pResp.getBody() != null) {
                    Map<?, ?> prod = pResp.getBody();

                    // price -> unitPrice
                    if (prod.get("price") != null) it.put("unitPrice", prod.get("price"));
                    else if (prod.get("discountPrice") != null) it.put("unitPrice", prod.get("discountPrice"));

                    if (prod.get("discountPrice") != null) it.put("discountPrice", prod.get("discountPrice"));

                    // images
                    Object firstImage = null;
                    if (prod.get("imageName") != null) firstImage = prod.get("imageName");
                    else if (prod.get("imageNames") instanceof java.util.List &&
                            !((java.util.List<?>)prod.get("imageNames")).isEmpty())
                        firstImage = ((java.util.List<?>)prod.get("imageNames")).get(0);

                    if (firstImage != null) {
                        String fname = String.valueOf(firstImage);
                        String imageUrl;
                        if (fname.startsWith("http://") || fname.startsWith("https://")) {
                            imageUrl = fname;
                        } else {
                            imageUrl = master + "/public/products/images/" +
                                    java.net.URLEncoder.encode(fname, java.nio.charset.StandardCharsets.UTF_8);
                        }
                        it.put("productImage", imageUrl);
                        it.put("imageUrl", imageUrl);
                    }

                    // compute item total if quantity present and unitPrice available
                    Object qtyObj = it.get("quantity");
                    int qty = 0;
                    if (qtyObj instanceof Number) qty = ((Number) qtyObj).intValue();
                    else if (qtyObj instanceof String) {
                        try { qty = Integer.parseInt((String) qtyObj); } catch (Exception ignored) {}
                    }
                    Object up = it.get("unitPrice");
                    Double unitPrice = null;
                    if (up instanceof Number) unitPrice = ((Number) up).doubleValue();
                    else if (up instanceof String) {
                        try { unitPrice = Double.parseDouble((String) up); } catch (Exception ignored) {}
                    }
                    if (unitPrice != null) {
                        it.put("total", unitPrice * Math.max(1, qty));
                    }
                }
            } catch (Exception ignored) {
                // leave item as-is on failure
            }
        }
    }

    // ADDRESSES (remote first, fallback to local DB)
    @GetMapping("/addresses/{userId}")
    public ResponseEntity<?> proxyAddresses(@PathVariable("userId") Long userId) {
        try {
            String base = (addressServiceBase == null || addressServiceBase.isBlank()) ? "http://localhost:8090" : addressServiceBase;
            String addrUrl = base.endsWith("/") ? base + "api/addresses" : base + "/api/addresses";

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Id", String.valueOf(userId));
            HttpEntity<Void> ent = new HttpEntity<>(headers);

            ResponseEntity<AddressDto[]> resp = restTemplate.exchange(addrUrl, HttpMethod.GET, ent, AddressDto[].class);
            AddressDto[] body = resp.getBody();
            if (resp.getStatusCode().is2xxSuccessful() && body != null && body.length > 0) {
                return ResponseEntity.ok(body);
            }
        } catch (Exception ignored) {
            // fallthrough to DB fallback
        }

        // Fallback to local DB
        try {
            List<Address> local;
            try {
                local = addressRepository.findByUserIdAndDeletedFalse(userId);
            } catch (Throwable t) {
                local = addressRepository.findByUser_IdAndDeletedFalse(userId);
            }

            List<AddressDto> dtoList = local.stream().map(a -> AddressDto.builder()
                    .id(a.getId())
                    .userId(a.getUser() != null ? a.getUser().getId() : null)
                    .label(a.getLabel())
                    .addressLine1(a.getAddressLine1())
                    .addressLine2(a.getAddressLine2())
                    .city(a.getCity())
                    .state(a.getState())
                    .pincode(a.getPincode())
                    .country(a.getCountry())
                    .contactName(a.getContactName())
                    .contactPhone(a.getContactPhone())
                    .isDefault(a.getIsDefault())
                    .build()
            ).collect(Collectors.toList());
            return ResponseEntity.ok(dtoList);
        } catch (Exception ex) {
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    @PostMapping("/addresses/{userId}")
    public ResponseEntity<?> proxyAddAddress(@PathVariable("userId") Long userId, @RequestBody AddressDto addressDto) {
        try {
            String base = (addressServiceBase == null || addressServiceBase.isBlank()) ? "http://localhost:8090" : addressServiceBase;
            String url = base.endsWith("/") ? base + "api/addresses" : base + "/api/addresses";

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Id", String.valueOf(userId));
            HttpEntity<AddressDto> ent = new HttpEntity<>(addressDto, headers);

            ResponseEntity<AddressDto> resp = restTemplate.postForEntity(url, ent, AddressDto.class);
            return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Failed to add address", "detail", ex.getMessage()));
        }
    }

    @PutMapping("/addresses/{userId}/{addressId}")
    public ResponseEntity<?> proxyUpdateAddress(@PathVariable("userId") Long userId,
                                                @PathVariable("addressId") Long addressId,
                                                @RequestBody AddressDto addressDto) {
        try {
            String base = (addressServiceBase == null || addressServiceBase.isBlank()) ? "http://localhost:8090" : addressServiceBase;
            String url = base.endsWith("/") ? base + "api/addresses/" + addressId : base + "/api/addresses/" + addressId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Id", String.valueOf(userId));
            HttpEntity<AddressDto> ent = new HttpEntity<>(addressDto, headers);

            ResponseEntity<AddressDto> resp = restTemplate.exchange(url, HttpMethod.PUT, ent, AddressDto.class);
            return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Failed to update address", "detail", ex.getMessage()));
        }
    }

    @DeleteMapping("/addresses/{userId}/{addressId}")
    public ResponseEntity<?> proxyDeleteAddress(@PathVariable("userId") Long userId,
                                                @PathVariable("addressId") Long addressId) {
        try {
            String base = (addressServiceBase == null || addressServiceBase.isBlank()) ? "http://localhost:8090" : addressServiceBase;
            String url = base.endsWith("/") ? base + "api/addresses/" + addressId : base + "/api/addresses/" + addressId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Id", String.valueOf(userId));
            HttpEntity<Void> ent = new HttpEntity<>(headers);

            ResponseEntity<Void> resp = restTemplate.exchange(url, HttpMethod.DELETE, ent, Void.class);
            return ResponseEntity.status(resp.getStatusCode()).body(Map.of("success", resp.getStatusCode().is2xxSuccessful()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Failed to delete address", "detail", ex.getMessage()));
        }
    }

    @PostMapping("/checkout/place")
    public ResponseEntity<?> proxyPlaceOrder(@RequestBody Map<String,Object> req) {
        if (req == null || req.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing order details"));
        }

        // base = http://localhost:8092
        String base = safeBase(orderServiceBase, "http://localhost:8092");
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        // Call order-service: POST http://localhost:8092/api/orders/place
        String url = base + "/api/orders/place";

        log.info("Proxying place order to: {} payloadKeys={}", url, req.keySet());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(req, headers);

        try {
            ResponseEntity<Object> response = restTemplate.postForEntity(url, entity, Object.class);

            HttpHeaders respHeaders = new HttpHeaders();
            if (response.getHeaders() != null) {
                respHeaders.putAll(response.getHeaders());
            }

            return ResponseEntity
                    .status(response.getStatusCode())
                    .headers(respHeaders)
                    .body(response.getBody());

        } catch (HttpStatusCodeException ex) {
            String body = ex.getResponseBodyAsString();
            HttpHeaders errHeaders = new HttpHeaders();
            if (ex.getResponseHeaders() != null) {
                errHeaders.putAll(ex.getResponseHeaders());
            }
            Object respBody = (body != null && !body.isBlank())
                    ? body
                    : Map.of("error", "Order service returned error");
            log.error("Order service error {} body={}", ex.getStatusCode(), body);
            return ResponseEntity
                    .status(ex.getStatusCode())
                    .headers(errHeaders)
                    .body(respBody);

        } catch (Exception ex) {
            log.error("Failed to contact order service", ex);
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "error", "Failed to contact order service",
                            "detail", ex.getMessage()
                    ));
        }
    }



    @GetMapping("/orders/user/{userId}")
    public ResponseEntity<?> proxyListOrdersForUser(
            @PathVariable("userId") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return fetchOrdersForUser(userId, page, size);
    }



//    // POST /user/api/proxy/payment/create/{appOrderId}
//    @PostMapping("/payment/create/{appOrderId}")
//    public ResponseEntity<?> proxyCreateRazor(@PathVariable Long appOrderId) {
//        try {
//
//            String ordersBase = orderServiceUrl.endsWith("/")
//                    ? orderServiceUrl.substring(0, orderServiceUrl.length()-1)
//                    : orderServiceUrl;
//            String orderUrl = ordersBase.replace("/api/orders", "/api/user/orders")
//                    + "/by-id/" + appOrderId;
//
//
//            Map<?,?> orderDto = restTemplate.getForObject(orderUrl, Map.class);
//            if (orderDto == null) {
//                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
//                        .body(Map.of("success", false, "message", "Order not found"));
//            }
//
//            // 2) pick amount: totalAmount -> total -> subtotal
//            double total = 0.0;
//            Object a = orderDto.get("totalAmount");
//            if (a == null) a = orderDto.get("total");
//            if (a == null) a = orderDto.get("subtotal");
//            if (a instanceof Number) total = ((Number) a).doubleValue();
//            else if (a instanceof String) try { total = Double.parseDouble((String) a); } catch (Exception ignored) {}
//
//            if (total <= 0.0) {
//                return ResponseEntity.badRequest()
//                        .body(Map.of("success", false, "message", "Invalid amount. Must be greater than zero."));
//            }
//
//            int amountMinor = (int)Math.round(total * 100); // paise/fils
//            String payBase = paymentServiceUrl.endsWith("/")
//                    ? paymentServiceUrl.substring(0, paymentServiceUrl.length()-1)
//                    : paymentServiceUrl; // e.g. http://localhost:8094
//            String createUrl = payBase + "/payment/create"; // payment endpoint
//
//            Map<String, Object> body = new HashMap<>();
//            body.put("appOrderId", appOrderId);
//            body.put("amount", amountMinor);
//            body.put("currency", "INR"); // or "₹" if you use ₹
//
//            ResponseEntity<Map> upstream = restTemplate.postForEntity(createUrl, body, Map.class);
//            return ResponseEntity.status(upstream.getStatusCode()).body(upstream.getBody());
//        } catch (HttpStatusCodeException ex) {
//            String msg = ex.getResponseBodyAsString();
//            return ResponseEntity.status(ex.getStatusCode())
//                    .body((msg == null || msg.isBlank())
//                            ? Map.of("success", false, "message", "Failed to create order on payment service")
//                            : msg);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
//                    .body(Map.of("success", false, "message", "Failed to create order on payment service",
//                            "detail", e.getMessage()));
//        }
//    }

    // ADD THIS: POST /user/api/proxy/payment/verify/{appOrderId}
//    @PostMapping("/payment/verify/{appOrderId}")
//    public ResponseEntity<?> proxyVerifyRazor(@PathVariable Long appOrderId,
//                                              @RequestBody Map<String,Object> payload) {
//        // Payment service path MUST include the orderId
//        String url = paymentServiceUrl + "/api/payments/verify/" + appOrderId;
//        try {
//            ResponseEntity<Object> upstream =
//                    restTemplate.postForEntity(url, payload == null ? Map.of() : payload, Object.class);
//            return ResponseEntity.status(upstream.getStatusCode()).body(upstream.getBody());
//        } catch (HttpStatusCodeException ex) {
//            return ResponseEntity.status(ex.getStatusCode())
//                    .body((ex.getResponseBodyAsString() == null || ex.getResponseBodyAsString().isBlank())
//                            ? Map.of("error","payment verify failed")
//                            : ex.getResponseBodyAsString());
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
//                    .body(Map.of("error","Failed to contact payment service","detail",e.getMessage()));
//        }
//    }
//


    // LIST ORDERS FOR USER (proxied) — use Object.class to accept List OR Page OR Map
//    @GetMapping("/orders/user")
//    public ResponseEntity<?> proxyListOrdersForUser(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size,
//            Authentication authentication,
//            Principal principal,
//            HttpSession session) {
//
//        Long userId = null;
//
//        // Try session
//        Object su = session != null ? session.getAttribute("currentUser") : null;
//        userId = extractUserIdFromObject(su);
//
//        // Try Spring Security auth
//        if (userId == null && authentication != null && authentication.isAuthenticated()) {
//            Object p = authentication.getPrincipal();
//            userId = extractUserIdFromObject(p);
//            if (userId == null) {
//                try { userId = tryParseLong(authentication.getName()); } catch (Exception ignored) {}
//            }
//        }
//
//        // Try principal
//        if (userId == null && principal != null) {
//            userId = tryParseLong(principal.getName());
//        }
//
//        if (userId == null) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(Map.of("error", "User not authenticated"));
//        }
//
//        try {
//            String base = safeBase(orderServiceBase, "http://localhost:8092/api/orders");
//            if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
//
//            String url = UriComponentsBuilder.fromHttpUrl(base)
//                    .path("/user/" + userId)
//                    .queryParam("page", page)
//                    .queryParam("size", size)
//                    .toUriString();
//
//            ResponseEntity<Object> resp = restTemplate.getForEntity(url, Object.class);
//
//            Object body = resp.getBody();
//            if (body == null) return ResponseEntity.status(resp.getStatusCode()).body(body);
//
//            // Enrich: if paged object with content[]
//            if (body instanceof Map) {
//                Map<?, ?> map = (Map<?, ?>) body;
//                Object content = map.get("content");
//                if (content instanceof java.util.Collection) {
//                    // convert content to List of Map and enrich
//                    @SuppressWarnings("unchecked")
//                    List<Map<String, Object>> list = ((java.util.Collection<Object>) content)
//                            .stream()
//                            .map(o -> (Map<String, Object>) o)
//                            .collect(java.util.stream.Collectors.toList());
//                    enrichOrderListWithProducts(list);
//                    // return original map but updated content
//                    ((Map) map).put("content", list);
//                    return ResponseEntity.status(resp.getStatusCode()).body(map);
//                }
//            }
//
//            // If body is a list
//            if (body instanceof java.util.Collection) {
//                @SuppressWarnings("unchecked")
//                List<Map<String, Object>> list = ((java.util.Collection<Object>) body)
//                        .stream()
//                        .map(o -> (Map<String, Object>) o)
//                        .collect(java.util.stream.Collectors.toList());
//                enrichOrderListWithProducts(list);
//                return ResponseEntity.status(resp.getStatusCode()).body(list);
//            }
//
//            // fallback: return original response
//            return ResponseEntity.status(resp.getStatusCode()).body(body);
//
//        } catch (Exception ex) {
//            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
//                    .body(Map.of("error", "Failed to fetch orders", "detail", ex.getMessage()));
//        }
//    }




    // helper: enrich order list items in-place (adds price, imageName, productTitle fields if available)
    private void enrichOrderListWithProducts(List<Map<String, Object>> orders) {
        if (orders == null || orders.isEmpty()) return;

        String master = productServiceBase != null ? productServiceBase.trim() : "";
        if (master.isBlank()) master = "http://localhost:8086";
        if (master.endsWith("/")) master = master.substring(0, master.length() - 1);

        for (Map<String, Object> order : orders) {
            // items may be in "items" or "orderItems"
            Object itemsObj = order.getOrDefault("items", order.get("orderItems"));
            if (!(itemsObj instanceof java.util.Collection)) continue;

            for (Object itObj : (java.util.Collection<?>) itemsObj) {
                if (!(itObj instanceof Map)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> item = (Map<String, Object>) itObj;

                Object pid = item.get("productId");
                Long productId = null;
                if (pid instanceof Number) productId = ((Number) pid).longValue();
                else if (pid instanceof String) {
                    try { productId = Long.parseLong((String) pid); } catch (Exception ignored) {}
                }
                if (productId == null) continue;

                try {
                    String prodUrl = master + "/public/products/" + productId;
                    ResponseEntity<Map> pResp = restTemplate.getForEntity(prodUrl, Map.class);
                    if (pResp != null && pResp.getStatusCode().is2xxSuccessful() && pResp.getBody() != null) {
                        Map<?, ?> prod = pResp.getBody();

                        // title
                        if (prod.get("title") != null) item.put("productTitle", prod.get("title"));

                        // unitPrice (prefer price, fallback to discountPrice)
                        if (prod.get("price") != null) item.put("unitPrice", prod.get("price"));
                        else if (prod.get("discountPrice") != null) item.put("unitPrice", prod.get("discountPrice"));

                        // discountPrice
                        if (prod.get("discountPrice") != null) item.put("discountPrice", prod.get("discountPrice"));

                        // images -> productImage and imageUrl
                        Object firstImage = null;
                        if (prod.get("imageName") != null) firstImage = prod.get("imageName");
                        else if (prod.get("imageNames") instanceof java.util.List &&
                                !((java.util.List<?>)prod.get("imageNames")).isEmpty())
                            firstImage = ((java.util.List<?>)prod.get("imageNames")).get(0);

                        if (firstImage != null) {
                            String fname = String.valueOf(firstImage);
                            String imageUrl;
                            if (fname.startsWith("http://") || fname.startsWith("https://")) {
                                imageUrl = fname;
                            } else {
                                imageUrl = master + "/public/products/images/" +
                                        java.net.URLEncoder.encode(fname, java.nio.charset.StandardCharsets.UTF_8);
                            }
                            item.put("productImage", imageUrl);
                            item.put("imageUrl", imageUrl);
                        }

                        // compute item total if quantity present and unitPrice set
                        Object qtyObj = item.get("quantity");
                        int qty = 0;
                        if (qtyObj instanceof Number) qty = ((Number) qtyObj).intValue();
                        else if (qtyObj instanceof String) {
                            try { qty = Integer.parseInt((String) qtyObj); } catch (Exception ignored) {}
                        }

                        Object up = item.get("unitPrice");
                        Double unitPrice = null;
                        if (up instanceof Number) unitPrice = ((Number) up).doubleValue();
                        else if (up instanceof String) {
                            try { unitPrice = Double.parseDouble((String) up); } catch (Exception ignored) {}
                        }
                        if (unitPrice != null) {
                            item.put("total", unitPrice * Math.max(1, qty));
                        }
                    }
                } catch (Exception ignored) {
                    // ignore enrichment failures
                }
            }
        }
    }

//    @GetMapping("/orders/user/{userId}")
//    public ResponseEntity<?> proxyListOrdersForUser(
//            @PathVariable("userId") Long userId,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size) {
//        try {
//            String base = (orderServiceBase == null || orderServiceBase.isBlank()) ? "http://localhost:8092/api/orders" : orderServiceBase;
//            if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
//
//            String url = UriComponentsBuilder.fromHttpUrl(base)
//                    .path("/user/" + userId)
//                    .queryParam("page", page)
//                    .queryParam("size", size)
//                    .toUriString();
//
//            ResponseEntity<Object> resp = restTemplate.getForEntity(url, Object.class);
//            return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
//        } catch (Exception ex) {
//            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
//                    .body(Map.of("error", "Failed to fetch orders", "detail", ex.getMessage()));
//        }
//    }

    // GET single order (accept orderId as string so frontend can pass numeric or string safely)
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<?> proxyGetOrder(@PathVariable("orderId") String orderId) {
        if (orderId == null || orderId.trim().isEmpty()
                || "undefined".equals(orderId) || "null".equals(orderId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid order id"));
        }
        try {
            String url = orderBase() + "/" + orderId;   // -> http://localhost:8092/api/orders/{id}
            ResponseEntity<Object> resp = restTemplate.getForEntity(url, Object.class);
            return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Failed to fetch order", "detail", ex.getMessage()));
        }
    }


//    @PostMapping("/orders/{orderId}/cancel")
//    public ResponseEntity<?> proxyCancelOrder(
//            @PathVariable("orderId") String orderId,
//            @RequestParam(value = "reason", required = false) String reason) {
//
//        if (orderId == null || orderId.isBlank()
//                || "undefined".equals(orderId) || "null".equals(orderId)) {
//            return ResponseEntity.badRequest().body(Map.of("error", "Invalid order id"));
//        }
//
//        try {
//            String url = orderBase() + "/" + orderId + "/cancel";
//            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
//            if (reason != null && !reason.isBlank()) {
//                builder.queryParam("reason", reason);
//            }
//
//            ResponseEntity<Void> resp = restTemplate.exchange(
//                    builder.toUriString(),
//                    HttpMethod.POST,            // 👈 POST (not PUT)
//                    HttpEntity.EMPTY,
//                    Void.class
//            );
//            return ResponseEntity.status(resp.getStatusCode()).build();
//
//        } catch (HttpStatusCodeException ex) {
//            return ResponseEntity.status(ex.getStatusCode())
//                    .body(Map.of(
//                            "error", "Failed to cancel order",
//                            "detail", ex.getStatusCode().value() + " : " + ex.getResponseBodyAsString()
//                    ));
//        } catch (Exception ex) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(Map.of("error", "Failed to cancel order",
//                            "detail", ex.getMessage()));
//        }
//    }

    @GetMapping("/orders/{orderId}/invoice")
    public ResponseEntity<?> proxyDownloadInvoice(@PathVariable("orderId") String orderId) {
        if (orderId == null || orderId.isBlank()
                || "undefined".equals(orderId) || "null".equals(orderId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid order id"));
        }

        try {
            String url = orderBase() + "/" + orderId + "/invoice";

            ResponseEntity<byte[]> resp = restTemplate.getForEntity(url, byte[].class);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);

            if (resp.getHeaders().getContentDisposition() != null &&
                    resp.getHeaders().getContentDisposition().getFilename() != null) {
                headers.setContentDisposition(resp.getHeaders().getContentDisposition());
            } else {
                headers.setContentDisposition(
                        ContentDisposition.attachment()
                                .filename("invoice-" + orderId + ".pdf")
                                .build()
                );
            }

            return new ResponseEntity<>(resp.getBody(), headers, resp.getStatusCode());

        } catch (HttpStatusCodeException ex) {
            return ResponseEntity.status(ex.getStatusCode())
                    .body(Map.of(
                            "error", "Failed to fetch invoice",
                            "detail", ex.getStatusCode().value() + " : " + ex.getResponseBodyAsString()
                    ));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch invoice", "detail", ex.getMessage()));
        }
    }


    // PROXY INVOICE
//    @GetMapping("/orders/{orderId}/invoice")
//    public ResponseEntity<?> proxyInvoice(@PathVariable("orderId") String orderId) {
//        if (orderId == null || orderId.trim().isEmpty()
//                || "undefined".equals(orderId) || "null".equals(orderId)) {
//            return ResponseEntity.badRequest().body(Map.of("error", "Invalid order id"));
//        }
//        try {
//            String base = safeBase(orderServiceBase, "http://localhost:8092");
//            if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
//
//            // GET http://localhost:8092/api/orders/{orderId}/invoice
//            String url = base + "/api/orders/" + orderId + "/invoice";
//
//            HttpHeaders headers = new HttpHeaders();
//            HttpEntity<Void> ent = new HttpEntity<>(headers);
//
//            ResponseEntity<byte[]> resp = restTemplate.exchange(url, HttpMethod.GET, ent, byte[].class);
//
//            HttpHeaders outHeaders = new HttpHeaders();
//            if (resp.getHeaders().getContentType() != null) outHeaders.setContentType(resp.getHeaders().getContentType());
//            if (resp.getHeaders().containsKey(HttpHeaders.CONTENT_DISPOSITION)) {
//                outHeaders.put(HttpHeaders.CONTENT_DISPOSITION, resp.getHeaders().get(HttpHeaders.CONTENT_DISPOSITION));
//            }
//            return new ResponseEntity<>(resp.getBody(), outHeaders, resp.getStatusCode());
//        } catch (Exception ex) {
//            log.warn("proxyInvoice failed for order {}: {}", orderId, ex.getMessage());
//            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
//                    .body(Map.of("error", "Failed to fetch invoice", "detail", ex.getMessage()));
//        }
//    }


    @GetMapping("/cart/count")
    @ResponseBody
    public ResponseEntity<Integer> getCartCount(HttpSession session,
                                                Authentication authentication,
                                                Principal principal) {
        try {
            Long userId = null;
            Object su = session != null ? session.getAttribute("currentUser") : null;
            userId = extractUserIdFromObject(su);

            if (userId == null && authentication != null && authentication.isAuthenticated()) {
                Object p = authentication.getPrincipal();
                userId = extractUserIdFromObject(p);
                if (userId == null) {
                    try {
                        String nm = authentication.getName();
                        userId = tryParseLong(nm);
                    } catch (Exception ignored) {}
                }
            }

            if (userId == null && principal != null) {
                userId = tryParseLong(principal.getName());
            }

            if (userId == null) return ResponseEntity.ok(0);

            String base = safeBase(cartServiceBase, "http://localhost:8091/api/cart");
            String url = base + "/" + userId;

            ResponseEntity<?> resp = restTemplate.getForEntity(url, Object.class);
            if (resp != null && resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                Object body = resp.getBody();
                if (body instanceof Map) {
                    Object items = ((Map<?, ?>) body).get("items");
                    if (items instanceof Collection) {
                        // sum quantities if quantity field exists, otherwise count entries
                        int total = 0;
                        for (Object it : (Collection<?>) items) {
                            if (it instanceof Map) {
                                Object q = ((Map<?, ?>) it).get("quantity");
                                if (q instanceof Number) total += ((Number) q).intValue();
                                else if (q instanceof String) {
                                    try { total += Integer.parseInt((String) q); } catch (Exception e) { total += 1; }
                                } else total += 1;
                            } else total += 1;
                        }
                        return ResponseEntity.ok(total);
                    }
                }
                if (body instanceof Collection) {
                    return ResponseEntity.ok(((Collection<?>) body).size());
                }
                try {
                    Object items = body.getClass().getMethod("getItems").invoke(body);
                    if (items instanceof Collection) return ResponseEntity.ok(((Collection<?>) items).size());
                } catch (Exception ignored) {}
            }
        } catch (Exception ex) {
            log.warn("getCartCount failed: {}", ex.getMessage());
        }
        return ResponseEntity.ok(0);
    }

    // Helpers
    private String safeBase(String configured, String fallback) {
        if (configured == null || configured.isBlank()) return fallback;
        if (configured.endsWith("/")) return configured.substring(0, configured.length() - 1);
        return configured;
    }
//    private Long tryParseLong(String s) {
//        if (s == null) return null;
//        try { return Long.parseLong(s); } catch (Exception ignored) { return null; }
//    }

    private Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        if (o instanceof String) {
            try { return Long.parseLong((String) o); } catch (Exception ignored) {}
        }
        return null;
    }

    private Integer toInt(Object o, int defaultVal) {
        if (o == null) return defaultVal;
        if (o instanceof Number) return ((Number) o).intValue();
        if (o instanceof String) {
            try { return Integer.parseInt((String) o); } catch (Exception ignored) {}
        }
        return defaultVal;
    }

    private Double toDouble(Object o, double defaultVal) {
        if (o == null) return defaultVal;
        if (o instanceof Number) return ((Number) o).doubleValue();
        if (o instanceof String) {
            try { return Double.parseDouble((String) o); } catch (Exception ignored) {}
        }
        return defaultVal;
    }

    private String ensureAbsoluteImageUrl(String image) {
        if (image == null) return null;
        String t = image.trim();
        if (t.isEmpty()) return null;
        if (t.startsWith("http://") || t.startsWith("https://")) return t;

        String[] bases = new String[]{ uploadsBaseUrl, productServiceBase, masterServiceBase };

        for (String base : bases) {
            if (base == null) continue;
            base = base.trim();
            if (base.isEmpty()) continue;
            if (base.endsWith("/")) base = base.substring(0, base.length() - 1);

            if (t.startsWith("/")) {
                String trimmed = t.replaceAll("^/+", "");
                return base + "/" + trimmed;
            }
            if (t.toLowerCase().contains("uploads/")) {
                return base + "/" + t.replaceAll("^/+", "");
            }
            try {
                String encoded = java.net.URLEncoder.encode(t, java.nio.charset.StandardCharsets.UTF_8);
                return base + "/public/products/images/" + encoded;
            } catch (Exception e) {
                return base + "/public/products/images/" + t;
            }
        }

        return t;
    }

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> payload) {
        String target = paymentServiceUrl + "/api/payments/create-order";
        try {
            log.info("Proxying create-order to {}", target);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.exchange(target, HttpMethod.POST, request, Map.class);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (RestClientException ex) {
            log.error("Error proxying create-order to {} : {}", target, ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("success", false, "message", "Failed to create order on payment service", "error", ex.getMessage()));
        }
    }


    private BigDecimal fetchOrderAmount(Long orderId) {
        try {
            // Base should be just http://localhost:8092 (no /api/orders at the end)
            String base = safeBase(orderServiceBase, "http://localhost:8092");
            if (base.endsWith("/")) {
                base = base.substring(0, base.length() - 1);
            }

            // Final URL: http://localhost:8092/api/orders/{orderId}
            String url = base + "/api/orders/" + orderId;

            ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class);

            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> order = resp.getBody();

                // check if subtotal, tax, shipping are nested under "orderDetails"
                Map<String, Object> details =
                        order.containsKey("orderDetails") && order.get("orderDetails") instanceof Map
                                ? (Map<String, Object>) order.get("orderDetails")
                                : order;

                BigDecimal subtotal = toBigDecimal(details.get("subtotal"));
                BigDecimal tax      = toBigDecimal(details.get("tax"));
                BigDecimal shipping = toBigDecimal(details.get("shipping"));

                BigDecimal total = subtotal.add(tax).add(shipping);
                log.info("OrderId {}: subtotal={}, tax={}, shipping={}, total={}",
                        orderId, subtotal, tax, shipping, total);
                return total;
            }
        } catch (Exception e) {
            log.error("Failed to fetch order amount for orderId {}", orderId, e);
        }
        return BigDecimal.ZERO;
    }





    /**
     * Proxy endpoint that verifies a payment (forwards to payment service)
     * Expects payload returned by Razorpay handler: { razorpay_payment_id, razorpay_order_id, razorpay_signature }
     */


    /**
     * Optional: fetch configuration (razorpay key / default values) from payment service
     */
    @GetMapping("/config")
    public ResponseEntity<?> getPaymentConfig() {
        String target = paymentServiceUrl + "/api/payments/config";
        try {
            log.info("Proxying get-config to {}", target);
            ResponseEntity<Map> response = restTemplate.getForEntity(target, Map.class);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (RestClientException ex) {
            log.error("Error proxying get-config to {} : {}", target, ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("success", false, "message", "Failed to fetch payment config from payment service", "error", ex.getMessage()));
        }
    }

//    @RequestMapping(value = "/payment/create/{orderId}", method = RequestMethod.OPTIONS)
//    public ResponseEntity<Void> paymentCreateOptions() {
//        HttpHeaders headers = new HttpHeaders();
//        headers.add("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
//        headers.add("Access-Control-Allow-Headers", "Content-Type,Authorization");
//        headers.add("Access-Control-Allow-Origin", "*");
//        return new ResponseEntity<>(headers, HttpStatus.NO_CONTENT);
//    }

//
//    @PostMapping("/payment/create/{orderId}")
//    public ResponseEntity<?> createPayment(@PathVariable Long orderId, HttpServletRequest request) {
//
//        // 1️⃣ Fetch the order amount
//        BigDecimal amount = fetchOrderAmount(orderId);
//
//        // 2️⃣ Fallback if amount is null or zero (for testing / Razorpay requirement)
//        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
//            log.warn("fetchOrderAmount returned 0 or null for orderId={}. Using fallback amount=100 (1 INR)", orderId);
//            amount = BigDecimal.valueOf(100); // 1 INR minimum for Razorpay
//        } else {
//            log.info("OrderId {}: fetched amount={}", orderId, amount);
//        }
//
//        // 3️⃣ Build the payment service URL
//        String url = UriComponentsBuilder.fromHttpUrl(paymentServiceUrl)
//                .path("/api/payments/create-order")
//                .toUriString();
//
//        // 4️⃣ Prepare headers for forwarding
//        HttpHeaders forwardHeaders = buildForwardHeaders(request);
//        forwardHeaders.setContentType(MediaType.APPLICATION_JSON);
//
//        // 5️⃣ Build outgoing JSON payload
//        Map<String, Object> outgoing = new HashMap<>();
//        outgoing.put("appOrderId", orderId);
//        outgoing.put("amount", amount.setScale(2, RoundingMode.HALF_UP));
//
//        // Determine currency: prefer query param -> appCurrency -> default "INR"
//        String currency = request.getParameter("currency");
//        if (currency == null || currency.isBlank()) currency = appCurrency;
//        if (currency == null || currency.isBlank()) currency = "INR";
//        outgoing.put("currency", currency.toUpperCase(Locale.ROOT));
//
//        try {
//            // 6️⃣ Convert payload to JSON and send POST to payment service
//            String jsonOut = objectMapper.writeValueAsString(outgoing);
//            HttpEntity<String> entity = new HttpEntity<>(jsonOut, forwardHeaders);
//            ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.POST, entity, byte[].class);
//
//            // 7️⃣ Build and return response
//            return buildResponse(response);
//        } catch (Exception ex) {
//            log.error("Failed to create order on payment service for orderId={}", orderId, ex);
//            HttpHeaders headers = new HttpHeaders();
//            headers.add("Content-Type", "application/json");
//
//            Map<String, Object> err = Map.of(
//                    "success", false,
//                    "message", "Failed to create order on payment service",
//                    "error", ex.getMessage()
//            );
//
//            try {
//                String errJson = objectMapper.writeValueAsString(err);
//                return new ResponseEntity<>(errJson, headers, HttpStatus.INTERNAL_SERVER_ERROR);
//            } catch (Exception e2) {
//                return new ResponseEntity<>(Map.of("success", false), headers, HttpStatus.INTERNAL_SERVER_ERROR);
//            }
//        }
//    }

    // helper
    private BigDecimal toBigDecimal(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof Number) return new BigDecimal(((Number) o).toString());
        if (o instanceof String) {
            try { return new BigDecimal((String) o); } catch (Exception ignored) {}
        }
        return BigDecimal.ZERO;
    }

    // ---------- GET Payment ----------
    // ---------- POST Payment ----------
//    @PostMapping("/payment/create/{orderId}")
//    public ResponseEntity<?> createPaymentPost(@PathVariable Long orderId,
//                                               @RequestBody(required = false) byte[] body,
//                                               HttpServletRequest request) {
//
//        BigDecimal amount = fetchOrderAmount(orderId);
//        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                    .body(Map.of("success", false, "message", "Invalid amount. Must be greater than zero."));
//        }
//
//        String url = UriComponentsBuilder.fromHttpUrl(paymentServiceUrl)
//                .path("/api/payments/create-order")
//                .toUriString();
//
//        HttpHeaders forwardHeaders = buildForwardHeaders(request);
//        forwardHeaders.setContentType(MediaType.APPLICATION_JSON);
//
//        Map<String, Object> outgoing = new HashMap<>();
//        outgoing.put("appOrderId", orderId);
//        outgoing.put("amount", amount.setScale(2, RoundingMode.HALF_UP));
//
//        // default currency from app config
//        String currency = appCurrency != null && !appCurrency.isBlank() ? appCurrency : "INR";
//
//        try {
//            // merge any incoming currency override from request body
//            if (body != null && body.length > 0) {
//                Map<?, ?> incoming = objectMapper.readValue(body, Map.class);
//                if (incoming.containsKey("currency") && incoming.get("currency") != null) {
//                    String inc = incoming.get("currency").toString();
//                    if (!inc.isBlank()) currency = inc;
//                }
//            }
//
//            outgoing.put("currency", currency.toUpperCase(Locale.ROOT));
//
//            String jsonOut = objectMapper.writeValueAsString(outgoing);
//            HttpEntity<String> entity = new HttpEntity<>(jsonOut, forwardHeaders);
//            ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.POST, entity, byte[].class);
//            return buildResponse(response);
//        } catch (Exception ex) {
//            HttpHeaders headers = new HttpHeaders();
//            headers.add("Content-Type", "application/json");
//            Map<String, Object> err = Map.of("success", false, "message", "Failed to create order on payment service", "error", ex.getMessage());
//            try {
//                String errJson = objectMapper.writeValueAsString(err);
//                return new ResponseEntity<>(errJson, headers, HttpStatus.INTERNAL_SERVER_ERROR);
//            } catch (Exception e2) {
//                return new ResponseEntity<>(Map.of("success", false), headers, HttpStatus.INTERNAL_SERVER_ERROR);
//            }
//        }
//    }

    // helper: copy selected headers from incoming request
    private HttpHeaders buildForwardHeaders(HttpServletRequest request) {
        HttpHeaders forwardHeaders = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames == null) return forwardHeaders;

        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            if ("host".equalsIgnoreCase(name)) continue;
            Enumeration<String> values = request.getHeaders(name);
            while (values.hasMoreElements()) {
                forwardHeaders.add(name, values.nextElement());
            }
        }
        return forwardHeaders;
    }

    // helper: build response preserving status and headers
    private ResponseEntity<byte[]> buildResponse(ResponseEntity<byte[]> upstream) {
        HttpHeaders respHeaders = new HttpHeaders();
        upstream.getHeaders().forEach((k, v) -> respHeaders.put(k, v));
        return new ResponseEntity<>(upstream.getBody(), respHeaders, upstream.getStatusCode());
    }

    // --- Proxy verify endpoint used by frontend Razorpay handler ---
// --- Verify Razorpay signature (POST) – forwards to /api/payments/verify ---
//    @PostMapping("/payment/verify/{placedOrderId}")
//    public ResponseEntity<?> proxyVerifyPayment(@PathVariable("placedOrderId") Long placedOrderId,
//                                                @RequestBody(required = false) Map<String,Object> body,
//                                                HttpServletRequest request) {
//        String url = paymentServiceUrl + "/api/payments/verify/" + placedOrderId;
//
//        Map<String,Object> payload = new HashMap<>();
//        if (body != null) payload.putAll(body);
//        payload.putIfAbsent("appOrderId", placedOrderId);
//
//        try {
//            HttpHeaders headers = buildForwardHeaders(request);
//            headers.setContentType(MediaType.APPLICATION_JSON);
//            HttpEntity<Map<String,Object>> ent = new HttpEntity<>(payload, headers);
//            ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.POST, ent, Map.class);
//            return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
//        } catch (HttpStatusCodeException ex) {
//            String respBody = ex.getResponseBodyAsString();
//            HttpHeaders errH = new HttpHeaders();
//            if (ex.getResponseHeaders() != null) errH.putAll(ex.getResponseHeaders());
//            return ResponseEntity.status(ex.getStatusCode()).headers(errH)
//                    .body(respBody != null && !respBody.isBlank() ? respBody : Map.of("error","payment verify failed"));
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
//                    .body(Map.of("success", false, "message", "Failed to contact payment service", "error", e.getMessage()));
//        }
//    }
//

    // ---- Payment (POST) – forwards to /api/payments/create-order ----
// POST /user/api/payments-proxy/payment/create/{orderId}
//    @PostMapping("/payments-proxy/payment/create/{orderId}")
//    public ResponseEntity<?> createPaymentOrder_Compat(@PathVariable Long orderId,
//                                                       @RequestBody(required = false) Map<String,Object> body,
//                                                       HttpServletRequest request) {
//        BigDecimal amount = fetchOrderAmount(orderId);
//        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                    .body(Map.of("success", false, "message", "Invalid amount. Must be greater than zero."));
//        }
//
//        String url = UriComponentsBuilder.fromHttpUrl(paymentServiceUrl)
//                .path("/api/payments/create-order")
//                .toUriString();
//
//        Map<String, Object> outgoing = new HashMap<>();
//        outgoing.put("appOrderId", orderId);
//        outgoing.put("amount", amount.setScale(2, RoundingMode.HALF_UP)); // major units
//        outgoing.put("currency", "INR"); // force INR
//
//        try {
//            HttpHeaders headers = buildForwardHeaders(request);
//            headers.setContentType(MediaType.APPLICATION_JSON);
//            HttpEntity<Map<String,Object>> entity = new HttpEntity<>(outgoing, headers);
//
//            // **Return JSON**, not bytes
//            ResponseEntity<Map> upstream = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
//            return ResponseEntity.status(upstream.getStatusCode()).body(upstream.getBody());
//        } catch (Exception ex) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(Map.of("success", false, "message", "Failed to create order on payment service", "error", ex.getMessage()));
//        }
//    }

    // POST /user/api/payments-proxy/payment/verify/{placedOrderId}
//    @PostMapping("/payments-proxy/payment/verify/{placedOrderId}")
//    public ResponseEntity<?> verifyPayment_Compat(@PathVariable Long placedOrderId,
//                                                  @RequestBody(required = false) Map<String,Object> body,
//                                                  HttpServletRequest request) {
//        String url = paymentServiceUrl + "/api/payments/verify";
//        Map<String,Object> payload = new HashMap<>();
//        if (body != null) payload.putAll(body);
//        payload.putIfAbsent("appOrderId", placedOrderId);
//
//        try {
//            HttpHeaders headers = buildForwardHeaders(request);
//            headers.setContentType(MediaType.APPLICATION_JSON);
//            ResponseEntity<Map> upstream = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(payload, headers), Map.class);
//            return ResponseEntity.status(upstream.getStatusCode()).body(upstream.getBody());
//        } catch (HttpStatusCodeException ex) {
//            String respBody = ex.getResponseBodyAsString();
//            HttpHeaders errH = new HttpHeaders();
//            if (ex.getResponseHeaders() != null) errH.putAll(ex.getResponseHeaders());
//            return ResponseEntity.status(ex.getStatusCode()).headers(errH)
//                    .body(respBody != null && !respBody.isBlank() ? respBody : Map.of("error","payment verify failed"));
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
//                    .body(Map.of("success", false, "message", "Failed to contact payment service", "error", e.getMessage()));
//        }
//    }

    /**
     * Proxy to list orders for an arbitrary user ID (e.g. admin use).
     */

//
//    @GetMapping("/orders/user")
//    public ResponseEntity<?> proxyListOrdersForUser(@RequestParam(defaultValue = "0") int page,
//                                                    @RequestParam(defaultValue = "10") int size,
//                                                    Authentication authentication,
//                                                    Principal principal,
//                                                    HttpSession session) {
//        Long userId = resolveCurrentUserId(authentication, principal, session);
//        if (userId == null) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(Map.of("error", "User not authenticated"));
//        }
//        return fetchOrdersForUser(userId, page, size);
//    }
//
//    private ResponseEntity<?> fetchOrdersForUser(Long userId, int page, int size) {
//        try {
//            // Adjust base for user-level endpoints
//            String base = orderServiceBase.replace("/api/orders", "/api/user/orders");
//            if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
//
//            String url = UriComponentsBuilder.fromHttpUrl(base)
//                    .path("/by-user/" + userId)
//                    .queryParam("page", page)
//                    .queryParam("size", size)
//                    .toUriString();
//
//            ResponseEntity<Object> resp = restTemplate.getForEntity(url, Object.class);
//            return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
//        } catch (Exception ex) {
//            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
//                    .body(Map.of("error", "Failed to fetch orders", "detail", ex.getMessage()));
//        }
//    }
//



    /**
     * Resolve the current user ID from session, Spring Security, or Principal.
     */

    private ResponseEntity<?> fetchOrdersForUser(Long userId, int page, int size) {
        try {
            String base = safeBase(orderServiceBase, "http://localhost:8092");
            if (base.endsWith("/")) base = base.substring(0, base.length() - 1);

            // GET http://localhost:8092/api/orders/user/{userId}?page=..&size=..
            String url = UriComponentsBuilder.fromHttpUrl(base)
                    .path("/api/orders/user/" + userId)
                    .queryParam("page", page)
                    .queryParam("size", size)
                    .toUriString();

            ResponseEntity<Object> resp = restTemplate.getForEntity(url, Object.class);
            return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Failed to fetch orders", "detail", ex.getMessage()));
        }
    }

    // ---- Helpers ----
    private Long resolveCurrentUserId(Authentication authentication, Principal principal, HttpSession session) {
        if (session != null) {
            Long id = extractUserIdFromObject(session.getAttribute("currentUser"));
            if (id != null) return id;
        }
        if (authentication != null && authentication.isAuthenticated()) {
            Long id = extractUserIdFromObject(authentication.getPrincipal());
            if (id != null) return id;
            try {
                return Long.parseLong(authentication.getName());
            } catch (Exception ignored) {}
        }
        if (principal != null) {
            try {
                return Long.parseLong(principal.getName());
            } catch (Exception ignored) {}
        }
        return null;
    }
    /**
     * Helper to extract user ID from a session/auth principal.
     */

    private Long extractUserIdFromObject(Object obj) {
        if (obj == null) return null;
        // direct number
        if (obj instanceof Number) return ((Number)obj).longValue();
        // try getId() or getUserId()
        try {
            Object id = obj.getClass().getMethod("getId").invoke(obj);
            if (id instanceof Number) return ((Number) id).longValue();
            if (id instanceof String) return tryParseLong((String) id);
        } catch (Exception ignored) {}
        try {
            Object id = obj.getClass().getMethod("getUserId").invoke(obj);
            if (id instanceof Number) return ((Number) id).longValue();
            if (id instanceof String) return tryParseLong((String) id);
        } catch (Exception ignored) {}
        // map-based fallback
        if (obj instanceof Map) {
            Object id = ((Map<?,?>) obj).get("id");
            if (id instanceof Number) return ((Number) id).longValue();
            if (id instanceof String) return tryParseLong((String) id);
        }
        return null;
    }
    private Long tryParseLong(String value) {
        if (value == null) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }


//    @GetMapping("/orders/{id}")
//    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
//        String url = "http://localhost:8092/api/orders/" + id;
//        try {
//            OrderResponse order = restTemplate.getForObject(url, OrderResponse.class);
//            return ResponseEntity.ok(order);
//        } catch (Exception e) {
//            log.error("Failed to fetch order from order-service", e);
//            return ResponseEntity.status(500).build();
//        }
//    }


}
