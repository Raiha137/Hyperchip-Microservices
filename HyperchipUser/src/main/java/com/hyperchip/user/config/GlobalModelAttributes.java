package com.hyperchip.user.config;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;

import java.security.Principal;
import java.util.Collection;
import java.util.Map;

@ControllerAdvice
public class GlobalModelAttributes {

    private final RestTemplate restTemplate;

    @Value("${cart.service.url:http://localhost:8091/api/cart}")
    private String cartServiceBase;

    @Value("${master.service.url:http://localhost:8086}")
    private String masterBaseUrl;



    @Autowired
    public GlobalModelAttributes(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // keep master base url in model
    @ModelAttribute("masterBaseUrl")
    public String addMasterBaseUrl() {
        return masterBaseUrl;
    }

    // reliable cart count provider (returns total quantity if available; falls back to item count)
    @ModelAttribute("cartCount")
    public int cartCount(HttpSession session, Authentication authentication, Principal principal) {
        try {
            // 1) Try session 'currentUser' (legacy approach)
            Object su = session.getAttribute("currentUser");
            Long userId = extractUserIdFromObject(su);
            if (userId == null) {
                // 2) Try Spring Security Authentication principal (UserDetails)
                if (authentication != null && authentication.isAuthenticated()) {
                    Object p = authentication.getPrincipal();
                    userId = extractUserIdFromObject(p);
                }
            }
            // 3) Try Principal name (if it's the numeric id or email)
            if (userId == null && principal != null) {
                String name = principal.getName(); // may be username/email or id
                userId = tryParseLong(name);
            }

            if (userId == null) {
                return 0;
            }

            // build cart URL (add a short cache-busting timestamp query param to ensure freshness)
            String base = (cartServiceBase == null || cartServiceBase.isBlank()) ? "http://localhost:8091/api/cart" : cartServiceBase;
            if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
            String url = base + "/" + userId + "?ts=" + System.currentTimeMillis();

            ResponseEntity<?> resp = restTemplate.getForEntity(url, Object.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                Object body = resp.getBody();

                // If body is a map-like structure (most likely), prefer totalItems if provided
                if (body instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) body;
                    Object totalItems = map.get("totalItems");
                    if (totalItems instanceof Number) return ((Number) totalItems).intValue();
                    if (totalItems instanceof String) {
                        try { return Integer.parseInt((String) totalItems); } catch (Exception ignored) {}
                    }

                    Object items = map.get("items");
                    if (items instanceof Collection) return ((Collection<?>) items).stream()
                            .mapToInt(item -> {
                                // attempt to extract quantity from item if it's a map with 'quantity'
                                if (item instanceof Map) {
                                    Object q = ((Map<?, ?>) item).get("quantity");
                                    if (q instanceof Number) return ((Number) q).intValue();
                                    if (q instanceof String) {
                                        try { return Integer.parseInt((String) q); } catch (Exception ignored) {}
                                    }
                                }
                                return 1; // fallback: count as 1 per entry
                            }).sum();
                }

                // If body is a list directly (legacy behavior), sum quantities if possible
                if (body instanceof Collection) {
                    Collection<?> collection = (Collection<?>) body;
                    int sum = 0;
                    for (Object it : collection) {
                        if (it instanceof Map) {
                            Object q = ((Map<?, ?>) it).get("quantity");
                            if (q instanceof Number) sum += ((Number) q).intValue();
                            else if (q instanceof String) {
                                try { sum += Integer.parseInt((String) q); } catch (Exception ignored) { sum += 1; }
                            } else sum += 1;
                        } else sum += 1;
                    }
                    return sum;
                }

                // fallback: try getTotalItems() or getItems() via reflection
                try {
                    Object totalItems = body.getClass().getMethod("getTotalItems").invoke(body);
                    if (totalItems instanceof Number) return ((Number) totalItems).intValue();
                } catch (Exception ignored) {}
                try {
                    Object items = body.getClass().getMethod("getItems").invoke(body);
                    if (items instanceof Collection) {
                        // sum quantities if items are maps/objects with getQuantity
                        int sum = 0;
                        for (Object it : (Collection<?>) items) {
                            try {
                                Object q = it.getClass().getMethod("getQuantity").invoke(it);
                                if (q instanceof Number) sum += ((Number) q).intValue();
                                else if (q instanceof String) {
                                    try { sum += Integer.parseInt((String) q); } catch (Exception ex) { sum += 1; }
                                } else sum += 1;
                            } catch (Exception ex) {
                                sum += 1;
                            }
                        }
                        return sum;
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {
            // swallow errors to avoid breaking page rendering; return 0 on failure
        }
        return 0;
    }

    // helpers
    private Long extractUserIdFromObject(Object su) {
        if (su == null) return null;
        // if it's Long already
        if (su instanceof Number) return ((Number) su).longValue();
        // common SessionUser pattern: has getId()
        try {
            Object id = su.getClass().getMethod("getId").invoke(su);
            if (id instanceof Number) return ((Number) id).longValue();
            if (id instanceof String) return tryParseLong((String) id);
        } catch (Exception ignored) {}
        // If it's UserDetails and username is numeric id
        if (su instanceof UserDetails) {
            String username = ((UserDetails) su).getUsername();
            return tryParseLong(username);
        }
        // If the object is a Map-like DTO
        if (su instanceof Map) {
            Object id = ((Map<?, ?>) su).get("id");
            if (id instanceof Number) return ((Number) id).longValue();
            if (id instanceof String) return tryParseLong((String) id);
        }
        // try principal name if method exists
        try {
            Object name = su.getClass().getMethod("getUsername").invoke(su);
            if (name instanceof String) return tryParseLong((String) name);
        } catch (Exception ignored) {}
        return null;
    }

    private Long tryParseLong(String s) {
        if (s == null) return null;
        try {
            return Long.parseLong(s);
        } catch (Exception ignored) { return null; }
    }
}
