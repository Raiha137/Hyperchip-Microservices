package com.hyperchip.admin.controller;

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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    /**
     * Logger for tracking admin order operations.
     *
     * Purpose:
     * - Logs important admin actions like status updates
     * - Helps in debugging and auditing admin behavior
     */
    private static final Logger log =
            LoggerFactory.getLogger(AdminOrderController.class);

    /**
     * RestTemplate used to communicate with Order Service.
     *
     * Purpose:
     * - Fetch order data
     * - Update order status
     * - Cancel orders
     * - Download invoices
     */
    private final RestTemplate restTemplate;

    /**
     * Base URL for Admin Order APIs from Order Service.
     *
     * Purpose:
     * - Centralized endpoint for all admin order-related operations
     * - Configurable via application properties
     */
    @Value("${order.service.admin-base-url:http://localhost:8092/api/admin/orders}")
    private String orderAdminBase;

    // ============================================================
    // LIST ORDERS
    // ============================================================

    /**
     * Displays paginated list of orders in admin panel.
     *
     * Purpose:
     * - Shows all customer orders to admin
     * - Supports pagination, status filtering, and search
     * - Acts as the main order management screen
     *
     * URL:
     * - GET /admin/orders
     *
     * Query Parameters:
     * - page   : Page number (default 0)
     * - size   : Page size (default 9)
     * - status : Optional order status filter
     * - q      : Optional search keyword
     *
     * @return admin/orders.html view
     */
    @GetMapping
    public String listOrders(
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q
    ) {

        /**
         * Build dynamic Order Service URL with pagination and filters.
         */
        StringBuilder url = new StringBuilder(orderAdminBase)
                .append("?page=").append(page)
                .append("&size=").append(size);

        if (status != null && !status.isBlank()) {
            url.append("&status=").append(status);
        }
        if (q != null && !q.isBlank()) {
            url.append("&q=").append(q);
        }

        /**
         * Fetch paginated orders from Order Service.
         */
        ResponseEntity<OrderResponse[]> response =
                restTemplate.getForEntity(url.toString(), OrderResponse[].class);

        /**
         * Convert response array to list for view rendering.
         */
        List<OrderResponse> orders =
                response.getBody() != null
                        ? Arrays.asList(response.getBody())
                        : List.of();

        /**
         * Populate model attributes for Thymeleaf view.
         */
        model.addAttribute("orders", orders);
        model.addAttribute("currentPage", page);
        model.addAttribute("filterStatus", status);
        model.addAttribute("q", q);
        model.addAttribute("activePage", "orders");

        return "admin/orders";
    }

    // ============================================================
    // VIEW ORDER DETAILS
    // ============================================================

    /**
     * Displays detailed view of a single order.
     *
     * Purpose:
     * - Allows admin to inspect full order details
     * - Shows customer, items, payment, and status information
     *
     * URL:
     * - GET /admin/orders/{orderId}
     *
     * @param orderId unique order identifier
     * @return admin/order-detail.html view
     */
    @GetMapping("/{orderId}")
    public String viewOrder(Model model, @PathVariable Long orderId) {

        String url = orderAdminBase + "/" + orderId;

        /**
         * Fetch order details from Order Service.
         */
        OrderResponse order =
                restTemplate.getForObject(url, OrderResponse.class);

        if (order == null) {
            throw new IllegalArgumentException("Order not found");
        }

        model.addAttribute("order", order);
        model.addAttribute("activePage", "orders");

        return "admin/order-detail";
    }

    // ============================================================
    // SHOW STATUS CHANGE FORM
    // ============================================================

    /**
     * Displays order status update form.
     *
     * Purpose:
     * - Allows admin to change order lifecycle state
     * - Shows available status options
     *
     * URL:
     * - GET /admin/orders/{orderId}/status
     *
     * @param orderId unique order identifier
     * @return admin/order-status.html view
     */
    @GetMapping("/{orderId}/status")
    public String showStatusForm(
            @PathVariable Long orderId,
            Model model
    ) {

        String url = orderAdminBase + "/" + orderId;

        /**
         * Fetch current order information.
         */
        OrderResponse order =
                restTemplate.getForObject(url, OrderResponse.class);

        model.addAttribute("orderId", orderId);
        model.addAttribute("order", order);

        /**
         * Provide valid order statuses for admin selection.
         */
        model.addAttribute(
                "statuses",
                Arrays.asList(
                        "PENDING",
                        "CONFIRMED",
                        "PACKED",
                        "SHIPPED",
                        "OUT_FOR_DELIVERY",
                        "DELIVERED",
                        "CANCELLED",
                        "RETURNED"
                )
        );

        return "admin/order-status";
    }

    // ============================================================
    // SUBMIT STATUS CHANGE
    // ============================================================

    /**
     * Submits updated order status to Order Service.
     *
     * Purpose:
     * - Persists admin-driven order status changes
     * - Optionally records admin note
     *
     * URL:
     * - POST /admin/orders/{orderId}/status
     *
     * @param orderId order identifier
     * @param status  new order status
     * @param note    optional admin note
     */
    @PostMapping("/{orderId}/status")
    public String submitStatusChange(
            @PathVariable Long orderId,
            @RequestParam("status") String status,
            @RequestParam(value = "note", required = false) String note,
            RedirectAttributes redirectAttributes
    ) {

        log.info("Admin updating status order={} -> {}", orderId, status);

        try {
            String url = orderAdminBase + "/" + orderId + "/status";

            /**
             * Build request payload.
             */
            Map<String, String> body = new HashMap<>();
            body.put("status", status);
            if (note != null) {
                body.put("note", note);
            }

            restTemplate.postForEntity(url, body, Void.class);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Order status updated."
            );
        } catch (Exception ex) {
            log.error("Status update failed", ex);
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Failed to update."
            );
        }

        return "redirect:/admin/orders/" + orderId;
    }

    // ============================================================
    // CANCEL ORDER
    // ============================================================

    /**
     * Cancels an order from admin panel.
     *
     * Purpose:
     * - Allows admin to cancel problematic or invalid orders
     * - Optional cancellation reason is forwarded to Order Service
     *
     * URL:
     * - POST /admin/orders/{orderId}/cancel
     *
     * @param orderId order identifier
     * @param reason  optional cancellation reason
     */
    @PostMapping("/{orderId}/cancel")
    public String cancelOrder(
            @PathVariable Long orderId,
            @RequestParam(required = false) String reason,
            RedirectAttributes redirectAttributes
    ) {

        try {
            String url = orderAdminBase
                    + "/" + orderId
                    + "/cancel?reason="
                    + (reason != null ? reason : "");

            restTemplate.postForEntity(url, null, Void.class);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Order cancelled."
            );
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Failed to cancel order."
            );
        }

        return "redirect:/admin/orders/" + orderId;
    }

    // ============================================================
    // DOWNLOAD INVOICE
    // ============================================================

    /**
     * Downloads invoice for a specific order.
     *
     * Purpose:
     * - Allows admin to download or print order invoice
     * - Invoice is generated by Order Service
     *
     * URL:
     * - GET /admin/orders/{orderId}/invoice
     *
     * @param orderId order identifier
     * @return invoice file as byte array
     */
    @GetMapping("/{orderId}/invoice")
    public ResponseEntity<byte[]> downloadInvoice(
            @PathVariable Long orderId
    ) {

        String url = orderAdminBase + "/" + orderId + "/invoice";

        return restTemplate.getForEntity(url, byte[].class);
    }

}
