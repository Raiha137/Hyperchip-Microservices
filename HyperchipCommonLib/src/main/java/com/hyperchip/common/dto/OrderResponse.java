package com.hyperchip.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private Long orderId;
    private String orderNumber;
    private Long userId;

    private LocalDateTime orderDate;
    private String status;

    private Double subtotal;
    private Double tax;         // ✅ new
    private Double shipping;    // ✅ new
    private Double total;
    private Double totalAmount;

    private Integer totalItems;
    private List<OrderItemResponse> items;
    private List<OrderItemResponse> orderItems;

    private Instant createdAt;
    private Instant updatedAt;

    private String userEmail;      // ✅ new

    // ✅ payment info
    private String paymentMethod;  // COD / RAZORPAY / WALLET
    private String paymentStatus;  // PENDING / PAID / FAILED / REFUNDED
    private Double paidAmount;     // how much actually paid
}
