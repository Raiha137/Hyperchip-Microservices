package com.hyperchip.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDTO {
    private Long id;
    private Long orderId;
    private String orderNumber;
    private Long userId;
    private LocalDateTime orderDate;
    private String status;
    private Double subtotal;
    private Double tax;
    private Double shipping;
    private Double total;
    private Double totalAmount;
    private Integer totalItems;
    private List<OrderItemDto> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String userEmail;
    private String paymentMethod;    // RAZORPAY, WALLET, COD…
    private String paymentStatus;    // PENDING, PAID, FAILED, REFUNDED
    private Double paidAmount;
}
