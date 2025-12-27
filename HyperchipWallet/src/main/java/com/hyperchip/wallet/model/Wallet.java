package com.hyperchip.wallet.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Wallet entity represents the wallet of a user.
 * Each user can have only one wallet (unique userId).
 * Stores the current balance and timestamps for tracking.
 */
@Entity
@Table(
        name = "wallets",
        uniqueConstraints = @UniqueConstraint(columnNames = "user_id")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Reference to the user owning this wallet (unique) */
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    /** Current wallet balance */
    @Column(name = "balance", nullable = false)
    private Double balance;

    /** Timestamp when wallet is created (auto-generated) */
    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    /** Timestamp when wallet is updated (auto-updated) */
    @UpdateTimestamp
    private Instant updatedAt;

    /** Timestamp of the last transaction (credit or debit) */
    @Column(name = "last_transaction_at")
    private Instant lastTransactionAt;

    /**
     * Initialize wallet balance before persisting to DB.
     * If balance is null, set to 0.0
     */
    @PrePersist
    void prePersist() {
        if (balance == null) balance = 0.0;
    }
}
