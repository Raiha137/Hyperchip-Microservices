package com.hyperchip.order.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing an Order in the system.
 * Contains order details, payment info, status, and associated items.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Primary key

    private String orderNumber; // Unique order number

    private Long userId; // ID of the user who placed the order
    private Long addressId; // Delivery address ID

    private String userEmail; // Store email at order time

    private Double subtotal; // Order subtotal
    private Double tax;      // Tax amount
    private Double shipping; // Shipping charges

    @Column(name = "total_amount")
    private Double totalAmount; // Total amount including subtotal, tax, and shipping

    private Boolean cancelled; // True if order is cancelled

    // ---------------- Payment Info ----------------
    @Column(name = "payment_method")
    private String paymentMethod; // e.g., RAZORPAY, WALLET, CASH_ON_DELIVERY

    @Column(name = "payment_status")
    private String paymentStatus; // INITIATED, PAID, FAILED, REFUNDED

    @Column(name = "payment_reference")
    private String paymentReference; // Payment gateway reference ID

    @Column(name = "paid_amount")
    private Double paidAmount; // Amount paid by user

    @Column(name = "paid_at")
    private Instant paidAt; // Timestamp when payment is completed

    @Column(name = "payment_failure_reason", length = 1000)
    private String paymentFailureReason; // Optional reason if payment failed

    // ---------------- Order Status ----------------
    @Enumerated(EnumType.STRING)
    private OrderStatus status; // Current status of the order

    @Column(name = "admin_note", length = 1000)
    private String adminNote; // Notes added by admin (cancellation/refund/etc.)

    private String cancelReason; // Reason for order cancellation

    // ---------------- Timestamps ----------------
    @CreationTimestamp
    private Instant createdAt; // When the order was created

    @UpdateTimestamp
    private Instant updatedAt; // Last update time

    // ---------------- Order Items ----------------
    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItem> items = new ArrayList<>(); // List of items in this order

    // ---------------- Convenience Methods ----------------

    public List<OrderItem> getOrderItems() {
        return this.items;
    }

    public void addItem(OrderItem item) {
        if (item == null) return;
        if (this.items == null) this.items = new ArrayList<>();
        item.setOrder(this);
        this.items.add(item);
    }

    public void removeItem(OrderItem item) {
        if (item == null || this.items == null) return;
        item.setOrder(null);
        this.items.remove(item);
    }

    public Double getTotal() {
        return this.totalAmount;
    }

    public void updateStatus(OrderStatus status, String note) {
        this.status = status;
        this.adminNote = note;
    }

    public void setPaymentFailureReason(String reason) {
        this.paymentFailureReason = reason;
    }
}
