package com.hyperchip.order.service.impl;

import com.hyperchip.order.model.Order;
import com.hyperchip.order.model.OrderItem;
import com.hyperchip.order.service.MailService;
import com.hyperchip.common.dto.AddressDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

/**
 * Advanced MailService for Hyperchip Orders.
 * <p>
 * Features:
 * - Fetches delivery address from User microservice using RestTemplate
 * - Builds detailed order email (items, subtotal, shipping, total)
 * - Sends order confirmation and order-paid notifications
 */
@Service
@RequiredArgsConstructor
@Primary
@Slf4j
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    private final RestTemplate restTemplate;

    @Value("${user.service.url:http://localhost:8081}") // base URL of user microservice
    private String userServiceUrl;

    @Value("${order.confirm.recipient:}") // fallback email if user email missing
    private String defaultRecipient;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Override
    public void sendOrderConfirmation(Order order) {
        sendEmail(order, "Order Confirmed");
    }

    @Override
    public void sendOrderPaidNotification(Order order) {
        sendEmail(order, "Payment Received");
    }

    /**
     * Core email sending logic
     */
    private void sendEmail(Order order, String subjectPrefix) {
        if (order == null) return;

        // 1️⃣ Recipient
        String to = order.getUserEmail();
        if (to == null || to.isBlank()) {
            to = (defaultRecipient != null && !defaultRecipient.isBlank()) ? defaultRecipient : null;
        }
        if (to == null) {
            log.warn("No recipient configured for order {} – skipping email", order.getOrderNumber());
            return;
        }

        // 2️⃣ Fetch delivery address from User microservice
        String deliveryAddress = "N/A";
        if (order.getAddressId() != null) {
            try {
                AddressDto addr = restTemplate.getForObject(
                        userServiceUrl + "/api/addresses/{id}", AddressDto.class, order.getAddressId());
                if (addr != null) {
                    deliveryAddress = addr.getAddressLine1() + ", "
                            + addr.getAddressLine2() + ", "
                            + addr.getCity() + ", "
                            + addr.getState() + " - "
                            + addr.getPincode();
                }
            } catch (Exception e) {
                log.warn("Failed to fetch address for order {}: {}", order.getOrderNumber(), e.getMessage());
            }
        }

        // 3️⃣ Build items list
        StringBuilder itemsList = new StringBuilder();
        for (OrderItem item : order.getOrderItems()) {
            itemsList.append("• ").append(item.getProductTitle())
                    .append(" x").append(item.getQuantity())
                    .append(" = AED ").append(item.getTotal()).append("\n");
        }

        // 4️⃣ Build email body
        double discount = order.getSubtotal() - order.getTotalAmount() + order.getShipping();
        String body = "✅ " + subjectPrefix + " – Hyperchip\n\n" +
                "Hi " + Objects.toString(order.getUserEmail(), "Customer") + ",\n\n" +
                "Your order has been successfully processed.\n\n" +
                "🧾 Order Details\n" +
                "• Order ID       : " + order.getOrderNumber() + "\n" +
                "• Payment Method : " + order.getPaymentMethod() + "\n" +
                "• Payment Status : " + order.getPaymentStatus() + "\n\n" +
                "📦 Items Ordered\n" +
                itemsList + "\n" +
                "💰 Price Summary\n" +
                "• Subtotal       : AED " + order.getSubtotal() + "\n" +
                "• Discount       : AED " + discount + "\n" +
                "• Shipping       : AED " + order.getShipping() + "\n" +
                "• Total Amount   : AED " + order.getTotalAmount() + "\n\n" +
                "🚚 Delivery Address\n" + deliveryAddress + "\n\n" +
                "Track your order here: https://hyperchip.com/my-orders\n\n" +
                "Thanks for choosing Hyperchip ❤️";

        // 5️⃣ Send email
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            if (fromAddress != null && !fromAddress.isBlank()) msg.setFrom(fromAddress);
            msg.setSubject(subjectPrefix + " – " + order.getOrderNumber());
            msg.setText(body);
            mailSender.send(msg);
            log.info("Sent '{}' email to {} for order {}", subjectPrefix, to, order.getOrderNumber());
        } catch (MailException me) {
            log.error("Failed to send '{}' email to {} for order {}: {}", subjectPrefix, to, order.getOrderNumber(), me.getMessage());
        }
    }
}
