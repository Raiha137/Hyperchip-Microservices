package com.hyperchip.wallet.service;

import com.hyperchip.common.dto.*;

/**
 * WalletService interface
 *
 * Defines wallet-related operations that can be performed for a user.
 * Implementations handle the actual business logic, including
 * balance checking, crediting, and wallet payments.
 */
public interface WalletService {

    /**
     * Get the wallet balance for a specific user.
     * If the wallet does not exist, a virtual wallet with 0 balance can be returned.
     *
     * @param userId the ID of the user
     * @return WalletBalanceDto containing userId and current balance
     */
    WalletBalanceDto getWallet(Long userId);

    /**
     * Credit (add) a specific amount to the user's wallet.
     * Common use cases: order cancelled, refund approved.
     *
     * @param request WalletCreditRequest containing userId, amount, reason, and optional orderId
     * @return updated wallet balance
     */
    WalletBalanceDto credit(WalletCreditRequest request);

    /**
     * Pay using wallet balance during checkout.
     * Deducts the requested amount from the user's wallet.
     *
     * @param request WalletPaymentRequest containing userId, amount, and orderId
     * @return WalletPaymentResponse indicating success, paid amount, remaining balance, and order info
     */
    WalletPaymentResponse pay(WalletPaymentRequest request);
}
