package com.hyperchip.wallet.repository;

import com.hyperchip.wallet.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository interface for Wallet entity.
 * Provides CRUD operations and a method to find a wallet by user ID.
 */
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    /**
     * Find a wallet by the user ID.
     *
     * @param userId user identifier
     * @return Optional containing the Wallet if found, empty otherwise
     */
    Optional<Wallet> findByUserId(Long userId);
}
