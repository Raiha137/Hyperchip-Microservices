package com.hyperchip.order.controller;

import com.hyperchip.common.dto.OrderResponse;
import com.hyperchip.common.dto.PlaceOrderRequest;
import com.hyperchip.common.dto.PlaceOrderResponse;
import com.hyperchip.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for all user order operations.
 *
 * Purpose: Handles order placement, viewing, cancellation, returns, replacements,
 * payments, and invoice downloads for users.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderUserRestController {

    private final OrderService orderService;

    // ====================================================
    // PLACE ORDER
    // ====================================================
    /**
     * Purpose: Accepts order details from the user and creates a new order.
     * If request body is missing, returns an error.
     */
    @PostMapping("/place")
    public ResponseEntity<PlaceOrderResponse> placeOrder(
            @RequestBody(required = false) PlaceOrderRequest req) {

        if (req == null) {
            return ResponseEntity.badRequest().body(
                    PlaceOrderResponse.builder()
                            .success(false)
                            .message("Missing order request")
                            .build()
            );
        }

        PlaceOrderResponse resp = orderService.placeOrder(req);
        return ResponseEntity.ok(resp);
    }

    // ====================================================
    // LIST ORDERS FOR USER
    // ====================================================
    /**
     * Purpose: Returns a list of all orders placed by a specific user.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponse>> listForUser(@PathVariable Long userId) {
        List<OrderResponse> orders = orderService.getOrdersByUserId(userId);
        return ResponseEntity.ok(orders);
    }

    // ====================================================
    // GET ORDER DETAILS
    // ====================================================
    /**
     * Purpose: Retrieves the details of a specific order using its ID or order number.
     */
    @GetMapping("/{idOrOrderNumber}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String idOrOrderNumber) {
        OrderResponse response = orderService.getOrderByIdentifier(idOrOrderNumber);
        return ResponseEntity.ok(response);
    }

    // ====================================================
    // CANCEL ENTIRE ORDER
    // ====================================================
    /**
     * Purpose: Cancels the entire order for the user. Optional reason can be provided.
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(
            @PathVariable Long orderId,
            @RequestParam(value = "reason", required = false) String reason) {

        orderService.cancelOrder(orderId, reason);
        return ResponseEntity.ok(Map.of("message", "Order cancelled"));
    }

    // ====================================================
    // CANCEL SINGLE ITEM FROM ORDER
    // ====================================================
    /**
     * Purpose: Cancels a single item from an order. Optional reason can be provided.
     */
    @PostMapping("/{orderId}/items/{itemId}/cancel")
    public ResponseEntity<Void> cancelOrderItem(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @RequestParam(required = false) String reason) {

        orderService.cancelOrderItem(orderId, itemId, reason);
        return ResponseEntity.ok().build();
    }

    // ====================================================
    // RETURN ORDER
    // ====================================================
    /**
     * Purpose: Requests a return for the order. Admin will handle the return process.
     */
    @PostMapping("/{orderId}/return")
    public ResponseEntity<?> returnOrder(
            @PathVariable Long orderId,
            @RequestParam String reason) {

        orderService.returnOrderByAdmin(orderId, reason);
        return ResponseEntity.ok(Map.of("message", "Return requested"));
    }

    // ====================================================
    // DOWNLOAD INVOICE
    // ====================================================
    /**
     * Purpose: Downloads the PDF invoice for the order.
     * Currently returns a dummy PDF; replace with real invoice logic if needed.
     */
    @GetMapping("/{orderId}/invoice")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long orderId) throws Exception {

        byte[] pdfBytes = orderService.getInvoicePdf(orderId);

        if (pdfBytes == null || pdfBytes.length == 0) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("invoice-" + orderId + ".pdf")
                        .build()
        );
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    // ====================================================
    // PAYMENT: MARK AS PAID
    // ====================================================
    /**
     * Purpose: Marks an order as paid after successful payment.
     */
    @PostMapping("/{orderId}/mark-paid")
    public ResponseEntity<?> markPaid(
            @PathVariable Long orderId,
            @RequestBody Map<String, Object> body) {

        String paymentReference = (String) body.getOrDefault("paymentReference", null);
        String paymentMethod = (String) body.getOrDefault("paymentMethod", null);
        Double amount = body.containsKey("amount")
                ? Double.valueOf(body.get("amount").toString())
                : null;

        orderService.markOrderPaid(orderId, paymentReference, paymentMethod, amount);
        return ResponseEntity.ok(Map.of("status", "OK"));
    }

    // ====================================================
    // PAYMENT: MARK PAYMENT FAILED
    // ====================================================
    /**
     * Purpose: Marks an order as failed if payment did not go through.
     */
    @PostMapping("/{orderId}/mark-payment-failed")
    public ResponseEntity<?> markFailed(
            @PathVariable Long orderId,
            @RequestBody Map<String, Object> body) {

        String reason = (String) body.getOrDefault("reason", null);
        orderService.markOrderPaymentFailed(orderId, reason);
        return ResponseEntity.ok(Map.of("status", "OK"));
    }

    // ====================================================
    // RETURN REQUEST
    // ====================================================
    /**
     * Purpose: Creates a return request for the order by the user.
     */
    @PostMapping("/{orderId}/return-request")
    public ResponseEntity<PlaceOrderResponse> requestReturn(
            @PathVariable Long orderId,
            @RequestParam("reason") String reason) {

        PlaceOrderResponse resp = orderService.requestReturn(orderId, reason);
        return ResponseEntity.ok(resp);
    }

    // ====================================================
    // CANCEL RETURN REQUEST
    // ====================================================
    /**
     * Purpose: Cancels an already made return request for the order.
     */
    @PostMapping("/{orderId}/cancel-return")
    public ResponseEntity<?> cancelReturn(@PathVariable Long orderId) {
        orderService.cancelReturnRequest(orderId);
        return ResponseEntity.ok(Map.of("message", "Return request cancelled"));
    }

    // ====================================================
    // REPLACEMENT REQUEST
    // ====================================================
    /**
     * Purpose: Creates a replacement request for the order by the user.
     */
    @PostMapping("/{orderId}/replacement-request")
    public ResponseEntity<PlaceOrderResponse> requestReplacement(
            @PathVariable Long orderId,
            @RequestParam("reason") String reason) {

        PlaceOrderResponse resp = orderService.requestReplacement(orderId, reason);
        return ResponseEntity.ok(resp);
    }

    // ====================================================
    // CANCEL REPLACEMENT REQUEST
    // ====================================================
    /**
     * Purpose: Cancels an already made replacement request for the order.
     */
    @PostMapping("/{orderId}/cancel-replacement")
    public ResponseEntity<?> cancelReplacement(@PathVariable Long orderId) {
        orderService.cancelReplacementRequest(orderId);
        return ResponseEntity.ok(Map.of("message", "Replacement request cancelled"));
    }

}
