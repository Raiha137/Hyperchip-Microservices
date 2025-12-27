package com.hyperchip.wallet.controller;

import com.hyperchip.common.dto.LedgerEntryDto;
import com.hyperchip.wallet.model.WalletTransaction;
import com.hyperchip.wallet.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Wallet Ledger Controller (Admin)
 *
 * This controller is used by admin users to view wallet transaction records.
 * It acts like a wallet ledger (statement), showing credit and debit entries.
 *
 * Features:
 * - Fetch all wallet transactions
 * - Filter wallet transactions by date range
 * - Convert wallet transactions into a common ledger format
 *
 * Endpoint base path:
 * /api/admin/wallet
 */
@RestController
@RequestMapping("/api/admin/wallet")
@RequiredArgsConstructor
public class WalletAdminLedgerController {

    // Repository to access wallet transaction data from database
    private final WalletTransactionRepository txRepo;

    /**
     * Get wallet ledger entries.
     *
     * URL:
     * GET /api/admin/wallet/ledger
     *
     * Optional parameters:
     * - fromDate : start date (YYYY-MM-DD)
     * - toDate   : end date (YYYY-MM-DD)
     *
     * Behavior:
     * - If both dates are provided, transactions within that date range are returned
     * - If dates are not provided, all wallet transactions are returned
     *
     * Response:
     * - List of LedgerEntryDto objects
     */
    @GetMapping("/ledger")
    public List<LedgerEntryDto> getWalletLedger(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate
    ) {
        List<WalletTransaction> txs;

        // If date range is provided, fetch transactions between those dates
        if (fromDate != null && toDate != null) {
            LocalDateTime from = fromDate.atStartOfDay();
            LocalDateTime to = toDate.plusDays(1).atStartOfDay();
            txs = txRepo.findByCreatedAtBetween(from, to);
        } else {
            // Otherwise, fetch all wallet transactions
            txs = txRepo.findAll();
        }

        // Convert WalletTransaction entities to LedgerEntryDto
        return txs.stream()
                .map(this::toLedgerEntry)
                .collect(Collectors.toList());
    }

    /**
     * Convert WalletTransaction entity to LedgerEntryDto.
     *
     * This method prepares wallet data in a standard ledger format
     * so it can be used in reports, admin screens, or exports.
     *
     * Credit / Debit logic:
     * - If transaction type is "DEBIT" → direction is DEBIT
     * - Otherwise → direction is CREDIT
     */
    private LedgerEntryDto toLedgerEntry(WalletTransaction tx) {
        if (tx == null) {
            return null;
        }

        // CREDIT in wallet = CREDIT, DEBIT in wallet = DEBIT
        String direction = tx.getType() != null
                && tx.getType().equalsIgnoreCase("DEBIT")
                ? "DEBIT"
                : "CREDIT";

        return LedgerEntryDto.builder()
                .type("WALLET") // Ledger type is wallet
                .direction(direction)
                .orderId(tx.getOrderId())
                .orderNumber(tx.getOrderId() != null ? String.valueOf(tx.getOrderId()) : null)
                .userId(tx.getUserId())
                .userEmail(null)        // Email not available in wallet transaction
                .paymentMethod(null)    // Wallet does not use payment method
                .source(tx.getSource())
                .amount(tx.getAmount())
                .createdAt(
                        tx.getCreatedAt() != null
                                ? tx.getCreatedAt()
                                .atZone(java.time.ZoneId.systemDefault())
                                .toInstant()
                                : null
                )
                .build();
    }
}
