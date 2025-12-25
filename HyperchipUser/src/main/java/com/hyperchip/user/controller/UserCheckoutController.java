package com.hyperchip.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpSession;

/**
 * UserCheckoutController
 * ----------------------
 * Handles the user-facing checkout page.
 * Resolves user info from session or optional X-User-Id header
 * and populates the checkout page with necessary data.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserCheckoutController {

    @Value("${order.service.url:http://localhost:8092}")
    private String orderServiceUrl;  // Base URL for order service

    @Value("${cart.service.url:http://localhost:8091/api/cart}")
    private String cartServiceUrl;   // Base URL for cart service

    @Value("${address.service.url:http://localhost:8090}")
    private String addressServiceUrl; // Base URL for address service

    /**
     * Render the checkout page.
     * Resolves user ID and email to pre-populate checkout form.
     *
     * @param xUserId Optional user ID from header (used for API clients)
     * @param session HttpSession (for logged-in users)
     * @param model   Spring MVC model to pass attributes to Thymeleaf template
     * @return Thymeleaf template name for checkout page
     */
    @GetMapping("/checkout")
    public String checkoutPage(@RequestHeader(value = "X-User-Id", required = false) String xUserId,
                               HttpSession session,
                               Model model) {

        // Resolve current user's ID
        Long userId = resolveUserId(xUserId, session);
        model.addAttribute("userId", userId != null ? userId : "");

        // Resolve current user's email
        String email = resolveUserEmail(session);
        model.addAttribute("userEmail", email != null ? email : "");

        // Return the Thymeleaf template for checkout
        return "user/checkout";
    }

    /**
     * Helper method to resolve current user ID.
     * Priority: X-User-Id header → session.currentUser.getId() → session.currentUser.getUserId()
     *
     * @param xUserId Optional header
     * @param session HttpSession
     * @return user ID or null if not found
     */
    private Long resolveUserId(String xUserId, HttpSession session) {
        // Try header first
        if (xUserId != null && !xUserId.isBlank()) {
            try {
                return Long.parseLong(xUserId);
            } catch (NumberFormatException ignored) {}
        }

        // Fallback to session object
        Object cur = session.getAttribute("currentUser");
        if (cur != null) {
            try {
                var cls = cur.getClass();

                // Try getId() method
                try {
                    var m = cls.getMethod("getId");
                    Object v = m.invoke(cur);
                    if (v instanceof Number) return ((Number) v).longValue();
                    if (v instanceof String) return Long.parseLong((String) v);
                } catch (NoSuchMethodException ignored) {}

                // Try getUserId() method
                try {
                    var m2 = cls.getMethod("getUserId");
                    Object v = m2.invoke(cur);
                    if (v instanceof Number) return ((Number) v).longValue();
                    if (v instanceof String) return Long.parseLong((String) v);
                } catch (NoSuchMethodException ignored) {}

            } catch (Exception ignored) {}
        }

        return null;
    }

    /**
     * Helper method to resolve current user email.
     * Priority: session.currentUser.getEmail() → getUsername() → getUserEmail()
     *
     * @param session HttpSession
     * @return email as String, or null if not found
     */
    private String resolveUserEmail(HttpSession session) {
        Object cur = session.getAttribute("currentUser");
        if (cur == null) return null;

        try {
            var cls = cur.getClass();

            try {
                var m = cls.getMethod("getEmail");
                Object v = m.invoke(cur);
                if (v instanceof String) return (String) v;
            } catch (NoSuchMethodException ignored) {}

            try {
                var m2 = cls.getMethod("getUsername");
                Object v2 = m2.invoke(cur);
                if (v2 instanceof String) return (String) v2;
            } catch (NoSuchMethodException ignored) {}

            try {
                var m3 = cls.getMethod("getUserEmail");
                Object v3 = m3.invoke(cur);
                if (v3 instanceof String) return (String) v3;
            } catch (NoSuchMethodException ignored) {}

        } catch (Exception ignored) {}

        return null;
    }
}
