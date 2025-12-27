package com.hyperchip.wallet.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * WalletTransaction entity
 *
 * Represents a single wallet-related transaction (credit or debit).
 * Stored fields include user and wallet references, amount, type, source,
 * and creation timestamp.
 */
@Entity
@Table(name = "wallet_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** User who owns this transaction (stored directly for simplicity) */
    @Column(nullable = false)
    private Long userId;

    /** Reference to Wallet entity */
    @Column(nullable = false)
    private Long walletId;

    /** Optional related order id */
    private Long orderId;

    /** Transaction type: CREDIT or DEBIT */
    @Column(nullable = false)
    private String type; // CREDIT / DEBIT

    /** Transaction amount */
    @Column(nullable = false)
    private Double amount;

    /** Optional human-readable description */
    private String description;

    /** Source of transaction: e.g. ORDER_CANCELLED, RETURN_APPROVED, WALLET_PAYMENT */
    private String source;

    /** Creation timestamp (set at persist time) */
    private LocalDateTime createdAt;

    /**
     * Initialize createdAt before persisting.
     * Keeps the same behavior as original implementation.
     */
    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
