package com.hyperchip.user.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;

/**
 * WishlistWebController
 * ---------------------
 * Handles user-facing wishlist pages and acts as a proxy to the wishlist microservice.
 * Provides endpoints for viewing, adding, removing, and updating wishlist items.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/user")
public class WishlistWebController {

    private final RestTemplate restTemplate;

    /**
     * Base URL of the wishlist service (configured via application.properties).
     * Default: http://localhost:8093/api/wishlist
     */
    @Value("${wishlist.service.url:http://localhost:8093/api/wishlist}")
    private String wishlistServiceBase;

    // ------------------- FRONTEND PAGE -------------------

    /**
     * Render the wishlist page for the current user.
     * Adds "userId" to the model for frontend JS usage.
     *
     * @param model   Spring Model for view
     * @param session HttpSession to resolve current user
     * @return Thymeleaf template name: "user/wishlist"
     */
    @GetMapping("/wishlist")
    public String viewWishlist(org.springframework.ui.Model model, HttpSession session) {
        Long userId = resolveUserId(session);
        model.addAttribute("userId", userId != null ? userId : "");
        return "user/wishlist";
    }

    // ------------------- PROXY ENDPOINTS -------------------

    /**
     * Proxy GET: /user/api/wishlist/{userId}
     * Retrieves the wishlist from the wishlist microservice for the given user.
     */
    @GetMapping("/api/wishlist/{userId}")
    public ResponseEntity<?> apiGetWishlist(@PathVariable Long userId) {
        try {
            String url = normalizeBase(wishlistServiceBase) + "/" + userId;
            ResponseEntity<Object> resp = restTemplate.getForEntity(url, Object.class);
            return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
        } catch (HttpStatusCodeException hsx) {
            log.warn("apiGetWishlist upstream: {} body={}", hsx.getStatusCode(), hsx.getResponseBodyAsString());
            return ResponseEntity.status(hsx.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(hsx.getResponseBodyAsString());
        } catch (Exception ex) {
            log.warn("apiGetWishlist error: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Collections.emptyList());
        }
    }

    /**
     * Proxy POST: /user/api/wishlist/{userId}/items/{productId}
     * Adds a product to the user's wishlist.
     */
    @PostMapping("/api/wishlist/{userId}/items/{productId}")
    public ResponseEntity<?> apiAddToWishlist(@PathVariable Long userId, @PathVariable Long productId) {
        try {
            String url = normalizeBase(wishlistServiceBase) + "/" + userId + "/items/" + productId;
            ResponseEntity<Object> resp = restTemplate.postForEntity(url, null, Object.class);
            Object body = resp.getBody() != null ? resp.getBody() : Map.of("status", "ok");
            return ResponseEntity.status(resp.getStatusCode()).body(body);
        } catch (HttpStatusCodeException hsx) {
            log.warn("apiAddToWishlist upstream: {} body={}", hsx.getStatusCode(), hsx.getResponseBodyAsString());
            return ResponseEntity.status(hsx.getStatusCode())
                    .body(Map.of("error", safeResponseBody(hsx.getResponseBodyAsString())));
        } catch (Exception ex) {
            log.warn("apiAddToWishlist error: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Wishlist service unavailable"));
        }
    }

    /**
     * Proxy DELETE: /user/api/wishlist/{userId}/items/{productId}
     * Removes a product from the user's wishlist.
     */
    @DeleteMapping("/api/wishlist/{userId}/items/{productId}")
    public ResponseEntity<?> apiRemoveFromWishlist(@PathVariable Long userId, @PathVariable Long productId) {
        try {
            String url = normalizeBase(wishlistServiceBase) + "/" + userId + "/items/" + productId;
            restTemplate.delete(url);
            return ResponseEntity.noContent().build();
        } catch (HttpStatusCodeException hsx) {
            log.warn("apiRemoveFromWishlist upstream: {} body={}", hsx.getStatusCode(), hsx.getResponseBodyAsString());
            return ResponseEntity.status(hsx.getStatusCode())
                    .body(Map.of("error", safeResponseBody(hsx.getResponseBodyAsString())));
        } catch (Exception ex) {
            log.warn("apiRemoveFromWishlist error: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Wishlist service unavailable"));
        }
    }

    /**
     * Proxy POST: /user/api/wishlist/{userId}/items/{productId}/updateQuantity
     * Updates the quantity of a wishlist item.
     */
    @PostMapping("/api/wishlist/{userId}/items/{productId}/updateQuantity")
    public ResponseEntity<?> apiUpdateWishlist(@PathVariable Long userId,
                                               @PathVariable Long productId,
                                               @RequestBody Map<String, Object> body) {
        try {
            String url = normalizeBase(wishlistServiceBase) + "/" + userId + "/items/" + productId + "/set-quantity";
            ResponseEntity<Object> resp = restTemplate.postForEntity(url, body, Object.class);
            Object responseBody = resp.getBody() != null ? resp.getBody() : Map.of("status", "ok");
            return ResponseEntity.status(resp.getStatusCode()).body(responseBody);
        } catch (HttpStatusCodeException hsx) {
            log.warn("apiUpdateWishlist upstream: {} body={}", hsx.getStatusCode(), hsx.getResponseBodyAsString());
            return ResponseEntity.status(hsx.getStatusCode())
                    .body(Map.of("error", safeResponseBody(hsx.getResponseBodyAsString())));
        } catch (Exception ex) {
            log.warn("apiUpdateWishlist error: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Wishlist service unavailable"));
        }
    }

    // ------------------- UTILITY METHODS -------------------

    /** Ensure base URL does not end with a trailing slash */
    private String normalizeBase(String base) {
        if (base == null) return "";
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    /**
     * Resolve current user's ID from session.
     * Tries "currentUser" object first (via reflection getId or getUserId),
     * fallback to "currentUserId" attribute.
     */
    private Long resolveUserId(HttpSession session) {
        Object cur = session.getAttribute("currentUser");
        if (cur == null) {
            Object id = session.getAttribute("currentUserId");
            if (id instanceof Number) return ((Number) id).longValue();
            try {
                return id == null ? null : Long.parseLong(String.valueOf(id));
            } catch (Exception ignored) {
                return null;
            }
        }
        try {
            var cls = cur.getClass();
            try {
                var m = cls.getMethod("getId");
                Object v = m.invoke(cur);
                return v instanceof Number ? ((Number) v).longValue() : Long.parseLong(String.valueOf(v));
            } catch (NoSuchMethodException ignored) {}
            try {
                var m2 = cls.getMethod("getUserId");
                Object v = m2.invoke(cur);
                return v instanceof Number ? ((Number) v).longValue() : Long.parseLong(String.valueOf(v));
            } catch (NoSuchMethodException ignored) {}
        } catch (Exception ignored) {}
        return null;
    }

    /** Shorten long upstream response body for logging / error messages */
    private static String safeResponseBody(String s) {
        if (s == null) return null;
        return s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }
}
