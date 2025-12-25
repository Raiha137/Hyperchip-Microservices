package com.hyperchip.user.controller;

import com.hyperchip.common.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * UserOrderController
 * -----------------------
 * Handles all user-facing order pages and actions:
 *   - Payment success/failure pages
 *   - Order details page
 *   - Cancel, return, and replacement requests
 * This controller acts as a bridge to the Order Service via RestTemplate.
 */
@Controller
@RequestMapping("/user/order")
@RequiredArgsConstructor
public class UserOrderController {

    private static final Logger log = LoggerFactory.getLogger(UserOrderController.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${order.service.url:http://localhost:8092}")
    private String orderServiceBase; // base URL for order service (host + port)

    // ==================== SUCCESS / FAILURE PAGES ====================

    /**
     * Display order success page after payment.
     * @param orderNumber The order number from payment confirmation
     */
    @GetMapping("/success-page")
    public String orderSuccess(@RequestParam("orderNumber") String orderNumber, Model model) {
        model.addAttribute("orderNumber", orderNumber);
        return "user/payment/order-success";
    }

    /**
     * Display order failure page after payment fails.
     * Shows either orderNumber or orderId if available.
     */
    @GetMapping("/payment-failed")
    public String orderFailure(@RequestParam(value = "orderNumber", required = false) String orderNumber,
                               @RequestParam(value = "orderId", required = false) String orderId,
                               Model model) {
        String id = (orderNumber != null && !orderNumber.isBlank()) ? orderNumber : orderId;
        model.addAttribute("orderId", id);
        return "user/payment/order-failure";
    }

    // ==================== ORDER DETAILS PAGE ====================

    /**
     * Show detailed order information.
     * Fetches order from Order Service via REST call.
     */
    @GetMapping("/details/{orderId}")
    public String orderDetailsPage(@PathVariable("orderId") Long orderId, Model model) {
        try {
            String url = orderServiceBase + "/" + orderId;
            ResponseEntity<OrderResponse> resp = restTemplate.getForEntity(url, OrderResponse.class);

            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                model.addAttribute("order", resp.getBody());
                model.addAttribute("orderId", orderId);
            } else {
                model.addAttribute("error", "Failed to load order details");
                model.addAttribute("orderId", orderId);
            }
        } catch (Exception ex) {
            log.error("Error fetching order details for {}", orderId, ex);
            model.addAttribute("error", "Unable to fetch order details");
            model.addAttribute("orderId", orderId);
        }
        return "user/order-detail";
    }

    // ==================== CANCEL ORDER ====================

    /**
     * Show cancel request form for user to provide reason.
     */
    @GetMapping("/cancel-request")
    public String cancelRequestPage(@RequestParam("orderId") Long orderId,
                                    @RequestParam(value = "itemId", required = false) Long itemId,
                                    Model model) {
        model.addAttribute("orderId", orderId);
        model.addAttribute("itemId", itemId);
        return "user/order/cancel-request";
    }

    /**
     * Submit cancel request to Order Service.
     * Can cancel entire order or a specific item.
     */
    @PostMapping("/cancel")
    public String submitCancelOrder(@RequestParam("orderId") Long orderId,
                                    @RequestParam(value = "itemId", required = false) Long itemId,
                                    @RequestParam(value = "reason", required = false) String reason) {
        try {
            String base = stripTrailingSlash(orderServiceBase);

            String url;
            if (itemId != null) {
                url = base + "/api/orders/" + orderId + "/items/" + itemId + "/cancel";
            } else {
                url = base + "/api/orders/" + orderId + "/cancel";
            }

            if (reason != null && !reason.isBlank()) {
                url += "?reason=" + URLEncoder.encode(reason, StandardCharsets.UTF_8);
            }

            restTemplate.postForEntity(url, null, Void.class);
        } catch (Exception ex) {
            log.error("Cancel order failed for order {}: {}", orderId, ex.getMessage());
        }

        return "redirect:/user/order/details/" + orderId;
    }

    // ==================== RETURN REQUEST ====================

    /**
     * Show return request form for an order.
     */
    @GetMapping("/return-request")
    public String returnRequestPage(@RequestParam(value = "orderId", required = false) Long orderId,
                                    Model model,
                                    RedirectAttributes ra) {
        if (orderId == null) {
            ra.addFlashAttribute("errorMessage", "Order not found for return.");
            return "redirect:/user/orders";
        }
        model.addAttribute("orderId", orderId);
        return "user/order-return-request";
    }

    /**
     * Submit return request to Order Service.
     */
    @PostMapping("/return-request")
    public String submitReturnOrder(@RequestParam("orderId") Long orderId,
                                    @RequestParam("reason") String reason,
                                    RedirectAttributes ra) {
        try {
            String url = stripTrailingSlash(orderServiceBase) +
                    "/api/orders/" + orderId + "/return-request?reason=" +
                    URLEncoder.encode(reason, StandardCharsets.UTF_8);

            restTemplate.postForEntity(url, null, Void.class);
            ra.addFlashAttribute("successMessage", "Return request submitted.");
        } catch (Exception ex) {
            log.error("Return order failed for order {}: {}", orderId, ex.getMessage());
            ra.addFlashAttribute("errorMessage", "Return request failed.");
        }

        return "redirect:/user/order/details/" + orderId;
    }

    /**
     * Cancel previously submitted return request.
     */
    @GetMapping("/cancel-return")
    public String cancelReturn(@RequestParam("orderId") Long orderId,
                               RedirectAttributes ra) {
        try {
            String url = stripTrailingSlash(orderServiceBase) +
                    "/api/orders/" + orderId + "/cancel-return";

            restTemplate.postForEntity(url, null, Void.class);
            ra.addFlashAttribute("successMessage", "Return request cancelled.");
        } catch (Exception ex) {
            log.error("Cancel return failed for order {}: {}", orderId, ex.getMessage());
            ra.addFlashAttribute("errorMessage", "Failed to cancel return request.");
        }

        return "redirect:/user/order/details/" + orderId;
    }

    // ==================== REPLACEMENT REQUEST ====================

    /**
     * Show replacement request form for an order.
     */
    @GetMapping("/replacement-request")
    public String replacementRequestPage(@RequestParam(value = "orderId", required = false) Long orderId,
                                         Model model,
                                         RedirectAttributes ra) {
        if (orderId == null) {
            ra.addFlashAttribute("errorMessage", "Order not found for replacement.");
            return "redirect:/user/orders";
        }
        model.addAttribute("orderId", orderId);
        return "user/order-replacement-request";
    }

    /**
     * Submit replacement request to Order Service.
     */
    @PostMapping("/replacement-request")
    public String submitReplacementOrder(@RequestParam("orderId") Long orderId,
                                         @RequestParam("reason") String reason,
                                         RedirectAttributes ra) {
        try {
            String url = stripTrailingSlash(orderServiceBase) +
                    "/api/orders/" + orderId + "/replacement-request?reason=" +
                    URLEncoder.encode(reason, StandardCharsets.UTF_8);

            restTemplate.postForEntity(url, null, Void.class);
            ra.addFlashAttribute("successMessage", "Replacement request submitted.");
        } catch (Exception ex) {
            log.error("Replacement order failed for order {}: {}", orderId, ex.getMessage());
            ra.addFlashAttribute("errorMessage", "Replacement request failed.");
        }

        return "redirect:/user/order/details/" + orderId;
    }

    /**
     * Cancel previously submitted replacement request.
     */
    @GetMapping("/cancel-replacement")
    public String cancelReplacement(@RequestParam("orderId") Long orderId,
                                    RedirectAttributes ra) {
        try {
            String url = stripTrailingSlash(orderServiceBase) +
                    "/api/orders/" + orderId + "/cancel-replacement";

            restTemplate.postForEntity(url, null, Void.class);
            ra.addFlashAttribute("successMessage", "Replacement request cancelled.");
        } catch (Exception ex) {
            log.error("Cancel replacement failed for order {}: {}", orderId, ex.getMessage());
            ra.addFlashAttribute("errorMessage", "Failed to cancel replacement request.");
        }

        return "redirect:/user/order/details/" + orderId;
    }

    // ==================== HELPER METHODS ====================

    /**
     * Remove trailing slash from a URL string to avoid double-slash issues.
     */
    private String stripTrailingSlash(String url) {
        if (url.endsWith("/")) return url.substring(0, url.length() - 1);
        return url;
    }
}
