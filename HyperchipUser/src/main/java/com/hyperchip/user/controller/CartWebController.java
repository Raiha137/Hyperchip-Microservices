package com.hyperchip.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperchip.common.dto.AddToCartRequest;
import com.hyperchip.common.dto.CartResponse;
import com.hyperchip.user.session.SessionUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * CartWebController
 * ----------------
 * Handles all user-facing cart operations (web pages & API endpoints).
 *
 * Purpose:
 * - Render cart page for logged-in users
 * - Allow adding/removing/updating cart items
 * - Communicate with Cart microservice via RestTemplate
 * - Keep session attributes like cart count in sync
 */
@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
public class CartWebController {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Base URL for Cart Service
     * - Can be overridden via property: cart.service.url
     */
    @Value("${cart.service.url:http://localhost:8080/api/cart}")
    private String cartServiceBase;

    /**
     * Login page URL for redirection
     */
    @Value("${auth.service.login.url:/login}")
    private String authLoginUrl;

    /**
     * Utility to remove trailing slash from base URLs
     */
    private String ensureNoTrailing(String base) {
        if (base == null) return "";
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    // ------------------- Web Page Endpoints -------------------

    /**
     * Renders the cart page for the logged-in user.
     * Updates session attribute cartCount.
     */
    @GetMapping("/cart")
    public String viewCart(Model model, HttpSession session) {
        Long userId = resolveUserId(session);
        if (userId == null) return "redirect:" + authLoginUrl;

        try {
            String url = ensureNoTrailing(cartServiceBase) + "/" + userId;
            ResponseEntity<CartResponse> resp = restTemplate.getForEntity(url, CartResponse.class);
            CartResponse cart = resp.getBody();
            model.addAttribute("cart", cart);
            model.addAttribute("userId", userId);
            session.setAttribute("cartCount", cart != null ? cart.getTotalItems() : 0);
        } catch (Exception ex) {
            log.warn("viewCart: failed to fetch cart for user {}, reason: {}", userId, ex.getMessage());
            model.addAttribute("cart", null);
            model.addAttribute("userId", userId);
            session.setAttribute("cartCount", 0);
        }
        return "user/cart";
    }

    /**
     * Adds a product to the cart and redirects back to previous page.
     * Ensures quantity limits and session cartCount update.
     */
    @GetMapping("/cart/add/{productId}")
    public String addToCart(@PathVariable Long productId,
                            @RequestParam(name = "q", required = false, defaultValue = "1") Integer qty,
                            HttpSession session,
                            HttpServletRequest request) {
        Long userId = resolveUserId(session);
        if (userId == null) {
            String redirectBack = request.getRequestURI() + (request.getQueryString() == null ? "" : "?" + request.getQueryString());
            return "redirect:" + authLoginUrl + "?redirect=" + redirectBack;
        }

        AddToCartRequest dto = new AddToCartRequest();
        dto.setUserId(userId);
        dto.setProductId(productId);
        dto.setQuantity(Math.max(1, Math.min(qty == null ? 1 : qty, 10)));

        try {
            String url = ensureNoTrailing(cartServiceBase) + "/add";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<AddToCartRequest> entity = new HttpEntity<>(dto, headers);
            ResponseEntity<CartResponse> resp = restTemplate.exchange(url, HttpMethod.POST, entity, CartResponse.class);

            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                session.setAttribute("cartCount", resp.getBody().getTotalItems());
            }
        } catch (HttpStatusCodeException hsx) {
            String msg = parseErrorMessage(hsx.getResponseBodyAsString());
            log.warn("addToCart upstream returned non-2xx: {} body={}", hsx.getStatusCode(), hsx.getResponseBodyAsString());
            session.setAttribute("cartError", msg != null ? msg : "Failed to add product to cart");
        } catch (Exception ex) {
            log.warn("addToCart error for user {} product {}: {}", userId, productId, ex.getMessage());
            session.setAttribute("cartError", "Cart service unavailable");
        }

        String referer = request.getHeader("Referer");
        return referer != null && !referer.isBlank() ? "redirect:" + referer : "redirect:/user/cart";
    }

    /**
     * Removes a product from cart (web) and updates session cartCount.
     */
    @GetMapping("/cart/items/{productId}/remove")
    public String removeFromCart(@PathVariable Long productId, HttpSession session, HttpServletRequest request) {
        Long userId = resolveUserId(session);
        if (userId == null) return "redirect:" + authLoginUrl;
        try {
            String base = ensureNoTrailing(cartServiceBase);
            restTemplate.delete(base + "/" + userId + "/items/" + productId);
            CartResponse cart = restTemplate.getForObject(base + "/" + userId, CartResponse.class);
            session.setAttribute("cartCount", cart != null ? cart.getTotalItems() : 0);
        } catch (Exception ex) {
            log.warn("removeFromCart error: {}", ex.getMessage());
            session.setAttribute("cartCount", 0);
        }
        String ref = request.getHeader("Referer");
        return ref != null ? "redirect:" + ref : "redirect:/user/cart";
    }

    // ------------------- API Endpoints -------------------

    @GetMapping("/api/cart/{userId}")
    public ResponseEntity<CartResponse> apiGetCart(@PathVariable Long userId) {
        try {
            String url = ensureNoTrailing(cartServiceBase) + "/" + userId;
            CartResponse cart = restTemplate.getForObject(url, CartResponse.class);
            return ResponseEntity.ok(cart);
        } catch (Exception ex) {
            log.warn("apiGetCart error: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    @PostMapping("/api/cart/add")
    public ResponseEntity<?> apiAddToCart(@RequestBody AddToCartRequest req, HttpSession session) {
        try {
            String url = ensureNoTrailing(cartServiceBase) + "/add";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<AddToCartRequest> entity = new HttpEntity<>(req, headers);
            ResponseEntity<CartResponse> resp = restTemplate.exchange(url, HttpMethod.POST, entity, CartResponse.class);

            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                session.setAttribute("cartCount", resp.getBody().getTotalItems());
                return ResponseEntity.ok(resp.getBody());
            } else {
                return ResponseEntity.status(resp.getStatusCode())
                        .body(Map.of("error", "Could not add to cart", "status", resp.getStatusCodeValue()));
            }
        } catch (HttpStatusCodeException hsx) {
            String msg = parseErrorMessage(hsx.getResponseBodyAsString());
            return ResponseEntity.status(hsx.getStatusCode())
                    .body(Map.of("error", msg != null ? msg : "Cart service error"));
        } catch (Exception ex) {
            log.warn("apiAddToCart error", ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Cart service unavailable"));
        }
    }

    @PutMapping("/api/cart/{userId}/items/{productId}")
    public ResponseEntity<CartResponse> apiUpdateQty(@PathVariable Long userId,
                                                     @PathVariable Long productId,
                                                     @RequestParam Integer qty,
                                                     HttpSession session) {
        try {
            String url = ensureNoTrailing(cartServiceBase) + "/" + userId + "/items/" + productId + "?qty=" + qty;
            ResponseEntity<CartResponse> resp = restTemplate.exchange(url, HttpMethod.PUT, null, CartResponse.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                session.setAttribute("cartCount", resp.getBody().getTotalItems());
            }
            return resp;
        } catch (HttpStatusCodeException hsx) {
            log.warn("apiUpdateQty upstream error: {} body={}", hsx.getStatusCode(), hsx.getResponseBodyAsString());
            return ResponseEntity.status(hsx.getStatusCode()).build();
        } catch (Exception ex) {
            log.warn("apiUpdateQty error", ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    @DeleteMapping("/api/cart/{userId}/items/{productId}")
    public ResponseEntity<Void> apiRemoveItem(@PathVariable Long userId, @PathVariable Long productId, HttpSession session) {
        try {
            String base = ensureNoTrailing(cartServiceBase);
            restTemplate.delete(base + "/" + userId + "/items/" + productId);
            CartResponse cart = restTemplate.getForObject(base + "/" + userId, CartResponse.class);
            session.setAttribute("cartCount", cart != null ? cart.getTotalItems() : 0);
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            log.warn("apiRemoveItem error", ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    // ------------------- Utility Methods -------------------

    /**
     * Resolves current user ID from session.
     * Handles multiple possible session attributes.
     */
    private Long resolveUserId(HttpSession session) {
        Object cur = session.getAttribute("currentUser");
        if (cur instanceof SessionUser su) return su.getId();
        Object id = session.getAttribute("currentUserId");
        if (id instanceof Long) return (Long) id;
        try { return id == null ? null : Long.parseLong(String.valueOf(id)); } catch (Exception ignored) { return null; }
    }

    /**
     * Extracts error message from JSON body returned by upstream services.
     */
    private String parseErrorMessage(String body) {
        if (body == null) return null;
        try {
            Map m = objectMapper.readValue(body, Map.class);
            Object err = m.getOrDefault("error", m.get("message"));
            return err == null ? body : String.valueOf(err);
        } catch (Exception e) {
            return body;
        }
    }
}
