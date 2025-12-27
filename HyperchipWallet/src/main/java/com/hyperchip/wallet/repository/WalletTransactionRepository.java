package com.hyperchip.wallet.repository;

import com.hyperchip.wallet.model.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for WalletTransaction entity.
 * Provides CRUD operations and custom queries for wallet transactions.
 */
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    /**
     * Find all wallet transactions of a user, ordered by creation time descending.
     *
     * @param userId user identifier
     * @return list of wallet transactions
     */
    List<WalletTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find all wallet transactions created between two timestamps.
     *
     * @param from start timestamp (inclusive)
     * @param to   end timestamp (exclusive)
     * @return list of wallet transactions in the given time range
     */
    List<WalletTransaction> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
}
