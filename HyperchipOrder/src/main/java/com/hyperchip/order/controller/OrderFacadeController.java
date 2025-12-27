package com.hyperchip.order.controller;

import com.hyperchip.order.model.Order;
import com.hyperchip.order.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * OrderFacadeController
 *
 * Simple description:
 * - This controller renders pages related to orders for the user-facing application.
 * - It does not return JSON — it returns view names (Thymeleaf templates).
 * - The controller asks OrderService for order data and places that data into the page model.
 *
 * Developer notes (simple words):
 * - If an order is not found, the user is redirected to the home page.
 * - The templates expected:
 *      - order/detail        -> shows order details
 *      - payment/success     -> show payment success message
 *      - payment/failure     -> show payment failure message
 * - Keep controller code light: complex business rules belong in OrderService.
 */
@Controller
@RequestMapping("/orders")
public class OrderFacadeController {

    private final OrderService orderService;

    public OrderFacadeController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Render the order detail page for a user.
     *
     * Simple flow:
     * 1. Read orderId from the URL path.
     * 2. Ask OrderService for the Order object.
     * 3. If the order is not found, redirect to the home page.
     * 4. If found, put the order into the model and return the "order/detail" view.
     *
     * Notes:
     * - The view "order/detail" should use the model attribute "order".
     * - Use simple server-side rendering (Thymeleaf) to show order info.
     *
     * @param orderId the numeric id of the order
     * @param model   Thymeleaf model to hold attributes for the view
     * @return view name or redirect
     */
    @GetMapping("/{orderId}")
    public String orderDetail(@PathVariable Long orderId, Model model) {
        // Fetch order from service
        Optional<Order> optOrder = orderService.getOrder(orderId);
        Order order = optOrder.orElse(null);

        // If no order found, send user to home page
        if (order == null) return "redirect:/";

        // Put order into model so template can use it
        model.addAttribute("order", order);
        return "order/detail"; // Thymeleaf template: src/main/resources/templates/order/detail.html
    }

    /**
     * Show a simple payment success page after a redirect from the payment gateway.
     *
     * Simple flow:
     * 1. Accept orderId as a request parameter.
     * 2. Place orderId in the model for the view to display.
     * 3. Return the "payment/success" view.
     *
     * @param orderId the order id related to the successful payment
     * @param model   Thymeleaf model to hold attributes for the view
     * @return view name "payment/success"
     */
    @GetMapping("/payment/success")
    public String paymentSuccess(@RequestParam Long orderId, Model model) {
        // Add order id so template can show order number or details if needed
        model.addAttribute("orderId", orderId);
        return "payment/success";
    }

    /**
     * Show a simple payment failure page after a redirect from the payment gateway.
     *
     * Simple flow:
     * 1. Accept orderId as a request parameter.
     * 2. Place orderId in the model so the view can show which order failed.
     * 3. Return the "payment/failure" view.
     *
     * @param orderId the order id related to the failed payment
     * @param model   Thymeleaf model to hold attributes for the view
     * @return view name "payment/failure"
     */
    @GetMapping("/payment/failure")
    public String paymentFailure(@RequestParam Long orderId, Model model) {
        // Add order id so template can show order number or guidance to user
        model.addAttribute("orderId", orderId);
        return "payment/failure";
    }
}
