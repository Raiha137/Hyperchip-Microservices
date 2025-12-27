package com.hyperchip.wallet.controller;

import com.hyperchip.common.dto.*;
import com.hyperchip.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Wallet Controller (User)
 *
 * This controller handles all wallet-related operations for users.
 * It allows users to:
 * - View wallet balance
 * - Receive money back into wallet (credit)
 * - Pay using wallet balance during checkout
 *
 * Base URL:
 * /api/wallet
 */
@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    // Service layer that contains wallet business logic
    private final WalletService walletService;

    /**
     * Get wallet balance of a user.
     *
     * URL:
     * GET /api/wallet/{userId}
     *
     * Used to display the current wallet balance for a user.
     *
     * @param userId user identifier
     * @return wallet balance details
     */
    @GetMapping("/{userId}")
    public ResponseEntity<WalletBalanceDto> getWallet(@PathVariable Long userId) {
        return ResponseEntity.ok(walletService.getWallet(userId));
    }

    /**
     * Credit amount to wallet.
     *
     * URL:
     * POST /api/wallet/credit
     *
     * This is mainly used when:
     * - Order is cancelled
     * - Order amount is refunded
     *
     * The credited amount will be added back to the user's wallet.
     *
     * @param request wallet credit request details
     * @return updated wallet balance
     */
    @PostMapping("/credit")
    public ResponseEntity<WalletBalanceDto> credit(@RequestBody WalletCreditRequest request) {
        return ResponseEntity.ok(walletService.credit(request));
    }

    /**
     * Pay using wallet balance.
     *
     * URL:
     * POST /api/wallet/pay
     *
     * This is used during checkout when the user chooses
     * wallet as the payment method.
     *
     * The amount will be deducted from the wallet balance.
     *
     * @param request wallet payment request details
     * @return wallet payment response
     */
    @PostMapping("/pay")
    public ResponseEntity<WalletPaymentResponse> pay(@RequestBody WalletPaymentRequest request) {
        return ResponseEntity.ok(walletService.pay(request));
    }
}
