package com.hyperchip.user.controller;

import com.hyperchip.common.dto.CouponDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * UserCouponProxyController
 * ----------------------------
 * Acts as a proxy for user-related coupon operations.
 * Delegates requests to the master service for:
 *   - Applying coupons at checkout
 *   - Removing applied coupons
 *   - Fetching the logged-in user's coupons
 */
@RestController
@RequestMapping("/user/api/proxy/coupons")
@RequiredArgsConstructor
public class UserCouponProxyController {

    private final RestTemplate restTemplate;

    @Value("${master.service.url:http://localhost:8086}")
    private String masterBase;

    // ==================== APPLY COUPON ====================

    /**
     * Apply a coupon for the current user.
     * Expects JSON body with "couponCode" and "totalAmount".
     * Delegates request to master service and returns response.
     */
    @PostMapping("/apply")
    public Map<String, Object> applyCoupon(@RequestBody Map<String, Object> body,
                                           HttpSession session) {

        // Extract and normalize coupon code
        String code = body.get("couponCode").toString().trim().toUpperCase();
        double total = Double.parseDouble(body.get("totalAmount").toString());

        // Fetch userId from session if available
        Long userId = getSessionUserId(session);

        // Prepare payload for master service
        Map<String, Object> payload = new HashMap<>();
        payload.put("code", code);
        payload.put("orderTotal", total);
        payload.put("userId", userId);  // can be null; master service handles it

        String url = masterBase + "/api/coupons/apply";

        // Call master service
        Map<String, Object> res = restTemplate.postForObject(url, payload, Map.class);

        if (res == null) {
            res = new HashMap<>();
            res.put("success", false);
            res.put("message", "Coupon service error");
        }

        return res;
    }

    // ==================== REMOVE COUPON ====================

    /**
     * Remove applied coupon.
     * Simply resets discount info (no backend call needed here).
     */
    @PostMapping("/remove")
    public Map<String, Object> removeCoupon() {
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("discountAmount", 0.0);
        return res;
    }

    // ==================== MY COUPONS ====================

    /**
     * Retrieve all coupons for the logged-in user.
     * Delegates request to master service.
     * Returns empty list if user is not logged in.
     */
    @GetMapping("/my")
    public List<CouponDto> myCoupons(HttpSession session) {

        Long userId = getSessionUserId(session);
        if (userId == null) {
            return Collections.emptyList();
        }

        String url = masterBase + "/api/coupons/user/" + userId;

        CouponDto[] arr = restTemplate.getForObject(url, CouponDto[].class);
        if (arr == null) return Collections.emptyList();

        return Arrays.asList(arr);
    }

    // ==================== HELPER ====================

    /**
     * Utility to fetch user ID from session safely.
     */
    private Long getSessionUserId(HttpSession session) {
        Object current = session.getAttribute("currentUser");
        if (current != null) {
            try {
                var getIdMethod = current.getClass().getMethod("getId");
                Object value = getIdMethod.invoke(current);
                if (value instanceof Number) {
                    return ((Number) value).longValue();
                } else if (value != null) {
                    return Long.valueOf(value.toString());
                }
            } catch (Exception ignored) {
                // ignore reflection exceptions
            }
        }
        return null;
    }
}
