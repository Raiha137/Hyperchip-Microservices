package com.hyperchip.wallet.service.impl;

import com.hyperchip.common.dto.*;
import com.hyperchip.wallet.model.Wallet;
import com.hyperchip.wallet.repository.WalletRepository;
import com.hyperchip.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;

    /**
     * Get wallet balance for a user.
     * If user wallet does not exist, return virtual empty wallet (0 balance).
     */
    @Override
    @Transactional(readOnly = true)
    public WalletBalanceDto getWallet(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseGet(() -> Wallet.builder()
                        .userId(userId)
                        .balance(0.0)
                        .build());

        // return balance without saving new wallet
        return WalletBalanceDto.builder()
                .userId(wallet.getUserId())
                .balance(wallet.getBalance())
                .build();
    }

    /**
     * Credit user's wallet (e.g., for refund or cancellation).
     * Creates wallet if not exists and adds amount.
     */
    @Override
    public WalletBalanceDto credit(WalletCreditRequest request) {
        // validate request
        if (request == null || request.getUserId() == null || request.getAmount() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId and amount are required");
        }
        if (request.getAmount() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount must be > 0");
        }

        // get existing wallet or create new
        Wallet wallet = walletRepository.findByUserId(request.getUserId())
                .orElseGet(() -> Wallet.builder()
                        .userId(request.getUserId())
                        .balance(0.0)
                        .build());

        // add credit amount
        double newBalance = wallet.getBalance() + request.getAmount();
        wallet.setBalance(newBalance);
        wallet.setLastTransactionAt(Instant.now());
        walletRepository.save(wallet);

        // log the transaction
        log.info("Wallet CREDIT userId={} amount={} reason={} orderId={}",
                request.getUserId(), request.getAmount(),
                request.getReason(), request.getOrderId());

        // return updated balance
        return WalletBalanceDto.builder()
                .userId(wallet.getUserId())
                .balance(wallet.getBalance())
                .build();
    }

    /**
     * Pay using wallet balance.
     * Deducts amount from user's wallet if sufficient balance exists.
     */
    @Override
    public WalletPaymentResponse pay(WalletPaymentRequest request) {
        // validate request
        if (request == null || request.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }

        double amount = request.getAmount() != null ? request.getAmount() : 0.0;
        if (amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid amount");
        }

        // get existing wallet, fail if not found
        Wallet wallet = walletRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Wallet not found for user " + request.getUserId()));

        double currentBalance = wallet.getBalance();

        // check sufficient balance
        if (currentBalance < amount) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Insufficient amount to buy this product");
        }

        // deduct amount and save
        double newBalance = currentBalance - amount;
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        // return payment response
        return WalletPaymentResponse.builder()
                .success(true)
                .message("Wallet payment successful")
                .paidAmount(amount)
                .remainingBalance(newBalance)
                .userId(request.getUserId())
                .orderId(request.getOrderId())
                .build();
    }

}
