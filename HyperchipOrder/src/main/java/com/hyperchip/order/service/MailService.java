package com.hyperchip.order.service;

import com.hyperchip.order.model.Order;

/**
 * Service interface for sending email notifications related to orders.
 *
 * <p>Provides methods to send various types of order-related emails, such as:</p>
 * <ul>
 *     <li>Order confirmation</li>
 *     <li>Order payment success notification</li>
 *     <li>Payment failure notification</li>
 * </ul>
 */
public interface MailService {

    /**
     * Send an order confirmation email to the customer.
     *
     * @param order the order for which confirmation email should be sent
     */
    void sendOrderConfirmation(Order order);

    /**
     * Send an email notification when an order is successfully paid.
     * <p>By default, it delegates to {@link #sendOrderConfirmation(Order)}.</p>
     *
     * @param order the order for which payment success notification should be sent
     */
    default void sendOrderPaidNotification(Order order) {
        sendOrderConfirmation(order);
    }

    /**
     * Send an email notification when payment for an order has failed.
     * <p>This is a default no-op method; concrete implementations (e.g., SMTP)
     * can override this to provide actual email sending functionality.</p>
     *
     * @param order the order for which payment failure notification should be sent
     */
    default void sendPaymentFailedNotification(Order order) {
        // no-op; override in concrete SMTP implementation if needed
    }
}
