package com.hyperchip.order.service.impl;

import com.hyperchip.order.model.Order;
import com.hyperchip.order.service.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Dev-only placeholder mail service.
 * <p>
 * Purpose:
 * - Avoids sending real emails during local development/testing.
 * - Logs email attempts instead of actually sending them.
 * <p>
 * Activation:
 * - Use spring profile `local` to enable this implementation:
 *      spring.profiles.active=local
 * - In production, the real MailServiceImpl will be used.
 */
@Service
@Profile("local")
public class NoOpMailService implements MailService {

    private static final Logger log = LoggerFactory.getLogger(NoOpMailService.class);

    /**
     * Pretend to send an order confirmation.
     * Logs the action instead of sending an email.
     *
     * @param order the order object
     */
    @Override
    public void sendOrderConfirmation(Order order) {
        if (order == null) {
            log.warn("sendOrderConfirmation called with null order");
            return;
        }
        log.info("NoOpMailService: pretend-sent order confirmation for orderId={} orderNumber={}",
                order.getId(), order.getOrderNumber());
    }

    /**
     * Optional: You can implement sendOrderPaidNotification similarly
     * if you want to log payment notifications during dev without sending real emails.
     *
     * Uncomment and implement if needed:
     *
     * @Override
     * public void sendOrderPaidNotification(Order order) {
     *     if (order == null) {
     *         log.warn("sendOrderPaidNotification called with null order");
     *         return;
     *     }
     *     log.info("NoOpMailService: pretend-sent order-paid notification for orderId={} orderNumber={} paymentRef={}",
     *             order.getId(), order.getOrderNumber(), order.getPaymentReference());
     * }
     */
}
