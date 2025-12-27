//package com.hyperchip.order.service.impl;
//
//import com.hyperchip.order.model.Order;
//import com.hyperchip.order.service.MailService;
//import lombok.RequiredArgsConstructor;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Primary;
//import org.springframework.mail.MailException;
//import org.springframework.mail.SimpleMailMessage;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor
//@Primary
//public class SmtpMailService implements MailService {
//
//    private static final Logger log = LoggerFactory.getLogger(SmtpMailService.class);
//
//    private final JavaMailSender mailSender;
//
//    @Value("${order.confirm.recipient:}")
//    private String defaultRecipient;
//
//    @Value("${spring.mail.username:}")
//    private String fromAddress;
//
//    @Override
//    public void sendOrderConfirmation(Order order) {
//        if (order == null) return;
//
//        String to = null;
//        if (order.getUserEmail() != null && !order.getUserEmail().isBlank()) {
//            to = order.getUserEmail();
//        }
//
//        if ((to == null || to.isBlank()) && defaultRecipient != null && !defaultRecipient.isBlank()) {
//            to = defaultRecipient;
//        }
//
//        if (to == null || to.isBlank()) {
//            log.warn("No recipient configured for order confirmation; skipping email for order {}", order.getOrderNumber());
//            return;
//        }
//
//        SimpleMailMessage msg = new SimpleMailMessage();
//        msg.setTo(to);
//        if (fromAddress != null && !fromAddress.isBlank()) {
//            msg.setFrom(fromAddress);
//        }
//        msg.setSubject("Order Confirmed \u2014 " + (order.getOrderNumber() != null ? order.getOrderNumber() : order.getId()));
//        String body = "Thank you. Your order " + (order.getOrderNumber() != null ? order.getOrderNumber() : order.getId())
//                + " has been placed. Total: AED " + (order.getTotal() == null ? "0.00" : order.getTotal()) + ".";
//        msg.setText(body);
//
//        try {
//            mailSender.send(msg);
//            log.info("Order confirmation email sent to {} for order {}", to, order.getOrderNumber());
//        } catch (MailException me) {
//            log.error("Failed to send order confirmation email to {} for order {}: {}", to, order.getOrderNumber(), me.getMessage());
//            // do not propagate - best effort only
//        }
//    }
//}
