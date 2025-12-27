package com.hyperchip.order.service;

import com.hyperchip.common.dto.OrderResponse;
import com.hyperchip.common.dto.PlaceOrderRequest;
import com.hyperchip.common.dto.PlaceOrderResponse;
import com.hyperchip.order.model.Order;
import com.hyperchip.order.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service interface for handling orders in Hyperchip e-commerce.
 *
 * <p>This interface defines the contract for managing orders, including placing orders,
 * retrieving orders, updating order/payment status, handling cancellations, returns,
 * replacements, and generating invoices.</p>
 */
public interface OrderService {

    /**
     * Place a new order based on the request details.
     *
     * @param req the place order request containing cart, address, payment info
     * @return a response with order ID, status, totals, and other info
     */
    PlaceOrderResponse placeOrder(PlaceOrderRequest req);

    /**
     * Retrieve an order by its internal database ID.
     *
     * @param orderId the order ID
     * @return an Optional containing the Order if found
     */
    Optional<Order> getOrder(Long orderId);

    /**
     * Retrieve an order by its identifier or order number.
     *
     * @param idOrOrderNumber the order ID or order number string
     * @return the order response DTO
     */
    OrderResponse getOrderByIdentifier(String idOrOrderNumber);

    /**
     * Retrieve all orders placed by a specific user.
     *
     * @param userId the user ID
     * @return list of order response DTOs
     */
    List<OrderResponse> getOrdersByUserId(Long userId);

    /**
     * Retrieve an order by its ID and return a DTO.
     *
     * @param orderId the order ID
     * @return the order response DTO
     */
    OrderResponse getOrderById(Long orderId);

    /**
     * Retrieve a paginated list of orders.
     *
     * @param pageable pagination info
     * @return paginated orders
     */
    Page<Order> listOrders(Pageable pageable);

    /**
     * Retrieve a paginated list of orders for a user as DTOs.
     *
     * @param userId the user ID
     * @param pageable pagination info
     * @return paginated order response DTOs
     */
    Page<OrderResponse> listOrdersForUserResponses(Long userId, Pageable pageable);

    /**
     * Retrieve a paginated list of orders for a user as entities.
     *
     * @param userId the user ID
     * @param pageable pagination info
     * @return paginated Order entities
     */
    Page<Order> listOrdersForUser(Long userId, Pageable pageable);

    /**
     * Cancel an entire order with a reason.
     *
     * @param orderId the order ID
     * @param reason the cancellation reason
     * @return response with updated status
     */
    PlaceOrderResponse cancelOrder(Long orderId, String reason);

    /**
     * Cancel a specific item in an order.
     *
     * @param orderId the order ID
     * @param itemId the order item ID
     * @param reason the cancellation reason
     * @return response with updated status
     */
    PlaceOrderResponse cancelOrderItem(Long orderId, Long itemId, String reason);

    /**
     * Change the status of an order (e.g., shipped, delivered, cancelled).
     *
     * @param orderId the order ID
     * @param status the new order status
     * @param note optional note for status change
     * @return response with updated status
     */
    PlaceOrderResponse changeOrderStatus(Long orderId, OrderStatus status, String note);

    /**
     * Generate PDF invoice for a given order.
     *
     * @param orderId the order ID
     * @return byte array representing the PDF file
     * @throws Exception if PDF generation fails
     */
    byte[] generateInvoicePdf(Long orderId) throws Exception;

    /**
     * Search orders based on a keyword (user email, order number, etc.).
     *
     * @param keyword search keyword
     * @return list of matching order DTOs
     */
    List<OrderResponse> searchOrders(String keyword);

    /**
     * Retrieve all orders for a given user ID.
     *
     * @param userId the user ID
     * @return list of order DTOs
     */
    List<OrderResponse> getOrdersForUser(Long userId);

    // ----------------- Admin-specific helpers -----------------

    /**
     * List orders for admin with optional pagination, status filter, and search query.
     *
     * @param page page number
     * @param size page size
     * @param status optional order status filter
     * @param q optional search keyword
     * @return list of order DTOs
     */
    List<OrderResponse> listForAdmin(int page, int size, String status, String q);

    /**
     * Find an order DTO by ID.
     *
     * @param id the order ID
     * @return Optional containing the order DTO if found
     */
    Optional<OrderResponse> findOrderResponseById(Long id);

    /**
     * Update the status of an order from the admin panel.
     *
     * @param id order ID
     * @param status new status
     * @param note optional note
     */
    void updateStatusAdmin(Long id, String status, String note);

    /**
     * Get PDF invoice for admin purposes.
     *
     * @param id the order ID
     * @return PDF file as byte array
     * @throws Exception if PDF generation fails
     */
    byte[] getInvoicePdf(Long id) throws Exception;

    /**
     * Update the status of an order.
     *
     * @param orderId order ID
     * @param status new status
     * @param note optional note
     */
    void updateOrderStatus(Long orderId, String status, String note);

    /**
     * Mark an order as paid when payment service confirms payment success.
     *
     * @param orderId the order ID
     * @param paymentReference payment transaction reference
     * @param paymentMethod payment method used
     * @param amount amount paid
     * @return true if order update succeeded
     */
    boolean markOrderPaid(Long orderId, String paymentReference, String paymentMethod, Double amount);

    /**
     * Update the payment status of an order.
     *
     * @param orderId order ID
     * @param status payment status (e.g., PAID, FAILED)
     */
    void updatePaymentStatus(String orderId, String status);

    /**
     * Handle Razorpay payment gateway callback.
     *
     * @param payload payload from Razorpay
     * @param params additional parameters
     */
    void handleRazorpayCallback(Map<String, Object> payload, Map<String, String> params);

    /**
     * Handle Paypal payment gateway callback.
     *
     * @param payload payload from Paypal
     * @param params additional parameters
     */
    void handlePaypalCallback(Map<String, Object> payload, Map<String, String> params);

    /**
     * Get the total payable amount for an order.
     *
     * @param orderId order ID
     * @return payable amount
     */
    Double getPayableAmount(Long orderId);

    /**
     * Mark an order's payment as failed.
     *
     * @param orderId order ID
     * @param reason reason for failure
     * @return true if update succeeded
     */
    boolean markOrderPaymentFailed(Long orderId, String reason);

    /**
     * Cancel an order by admin.
     *
     * @param orderId order ID
     * @param reason reason for cancellation
     * @return updated order response
     */
    PlaceOrderResponse cancelOrderByAdmin(Long orderId, String reason);

    /**
     * Mark an order as returned by admin.
     *
     * @param orderId order ID
     * @param reason reason for return
     * @return updated order response
     */
    PlaceOrderResponse returnOrderByAdmin(Long orderId, String reason);

    /**
     * Increment product stock when an order is cancelled or returned.
     *
     * @param productId product ID
     * @param quantity quantity to increment
     */
    void incrementStock(Long productId, Integer quantity);

    /**
     * Request a return for an order.
     *
     * @param orderId order ID
     * @param reason reason for return
     * @return updated order response
     */
    PlaceOrderResponse requestReturn(Long orderId, String reason);

    /**
     * Cancel a previously requested return.
     *
     * @param orderId order ID
     */
    void cancelReturnRequest(Long orderId);

    /**
     * Request a replacement for an order.
     *
     * @param orderId order ID
     * @param reason reason for replacement
     * @return updated order response
     */
    PlaceOrderResponse requestReplacement(Long orderId, String reason);

    /**
     * Cancel a previously requested replacement.
     *
     * @param orderId order ID
     */
    void cancelReplacementRequest(Long orderId);
}
