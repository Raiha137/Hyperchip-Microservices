package com.hyperchip.admin.controller;

import com.hyperchip.common.dto.LedgerEntryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminLedgerRestController {

    /**
     * RestTemplate used to communicate with other microservices.
     *
     * Purpose:
     * - Calls Order Service and Wallet Service
     * - Fetches ledger-related data from those services
     */
    private final RestTemplate restTemplate;

    /**
     * Base URL of the Order Service.
     *
     * Purpose:
     * - Used to fetch order-related financial ledger entries
     * - Default value allows local development without config
     */
    @Value("${order.service.url:http://localhost:8092}")
    private String orderServiceBase;

    /**
     * Base URL of the Wallet Service.
     *
     * Purpose:
     * - Used to fetch wallet-related ledger entries
     * - Includes wallet credits, debits, refunds, reserve fees, etc.
     */
    @Value("${wallet.service.url:http://localhost:8095}")
    private String walletServiceBase;

    /**
     * Returns a unified admin ledger by combining multiple sources.
     *
     * Purpose:
     * - Aggregates financial transactions from:
     *   1. Order Service (payments, COD, Razorpay, etc.)
     *   2. Wallet Service (wallet usage, refunds, reserve fees)
     * - Applies optional filters (date range, payment method)
     * - Sorts results by latest transaction
     * - Limits output to recent entries when no filters are applied
     *
     * URL:
     * - GET /api/admin/ledger
     *
     * Query Parameters (all optional):
     * - fromDate : Start date for filtering ledger entries
     * - toDate   : End date for filtering ledger entries
     * - type     : Payment method filter (COD, RAZORPAY, WALLET, etc.)
     *
     * @param fromDate optional start date
     * @param toDate   optional end date
     * @param type     optional payment method filter
     * @return list of unified ledger entries for admin view
     */
    @GetMapping("/ledger")
    public List<LedgerEntryDto> getLedger(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(required = false)
            String type   // Used as PAYMENT METHOD filter
    ) {

        /**
         * Convert LocalDate parameters to String format
         * so they can be safely passed to downstream services.
         */
        String fromParam = fromDate != null ? fromDate.toString() : null;
        String toParam   = toDate   != null ? toDate.toString()   : null;

        /**
         * Fetch ledger entries from Order Service.
         *
         * Includes:
         * - Order payments
         * - COD settlements
         * - Razorpay transactions
         */
        List<LedgerEntryDto> orderEntries =
                fetchOrderLedger(fromParam, toParam);

        /**
         * Fetch ledger entries from Wallet Service.
         *
         * Includes:
         * - Wallet credits
         * - Wallet debits
         * - Refunds
         * - Reserve fees
         */
        List<LedgerEntryDto> walletEntries =
                fetchWalletLedger(fromParam, toParam);

        /**
         * Combine order and wallet ledger entries into a single stream.
         *
         * Purpose:
         * - Create a unified admin ledger
         * - Allows common filtering and sorting logic
         */
        Stream<LedgerEntryDto> stream = Stream.concat(
                orderEntries.stream(),
                walletEntries.stream()
        );

        /**
         * Apply payment method filter if provided.
         *
         * Supported examples:
         * - RAZORPAY
         * - COD
         * - WALLET
         * - RESERVE_FEE
         */
        if (type != null && !type.isBlank()) {
            String method = type.toUpperCase();

            stream = stream.filter(entry ->
                    entry.getPaymentMethod() != null &&
                            method.equalsIgnoreCase(entry.getPaymentMethod())
            );
        }

        /**
         * Check whether any filter is applied.
         *
         * Purpose:
         * - Used to decide whether to limit the result size
         */
        boolean hasAnyFilter =
                fromDate != null ||
                        toDate   != null ||
                        (type != null && !type.isBlank());

        /**
         * Sort ledger entries by creation time.
         *
         * Order:
         * - Latest transactions first
         * - Null-safe sorting
         */
        Stream<LedgerEntryDto> sorted = stream.sorted(
                Comparator.comparing(
                        LedgerEntryDto::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                )
        );

        /**
         * If no filters are applied:
         * - Limit results to latest 50 entries
         *
         * Purpose:
         * - Prevent loading too much historical data by default
         * - Improves admin dashboard performance
         */
        if (!hasAnyFilter) {
            sorted = sorted.limit(50);
        }

        /**
         * Return final ledger list after filtering and sorting.
         */
        return sorted.collect(Collectors.toList());
    }

    /**
     * Fetches ledger entries from the Order Service.
     *
     * Purpose:
     * - Retrieves order-related financial transactions
     * - Supports optional date range filtering
     *
     * @param from optional start date (ISO format)
     * @param to   optional end date (ISO format)
     * @return list of order ledger entries
     */
    private List<LedgerEntryDto> fetchOrderLedger(String from, String to) {
        try {
            String url = orderServiceBase + "/api/admin/orders/ledger";

            List<String> params = new ArrayList<>();
            if (from != null) params.add("fromDate=" + from);
            if (to   != null) params.add("toDate=" + to);

            if (!params.isEmpty()) {
                url += "?" + String.join("&", params);
            }

            LedgerEntryDto[] response =
                    restTemplate.getForObject(url, LedgerEntryDto[].class);

            return response != null ? Arrays.asList(response) : List.of();

        } catch (Exception ex) {
            /**
             * Fail-safe behavior:
             * - If Order Service is unavailable or errors occur,
             *   return an empty list instead of breaking admin UI.
             */
            return List.of();
        }
    }

    /**
     * Fetches ledger entries from the Wallet Service.
     *
     * Purpose:
     * - Retrieves wallet-related financial transactions
     * - Includes refunds, wallet usage, and reserve fees
     * - Supports optional date range filtering
     *
     * @param from optional start date (ISO format)
     * @param to   optional end date (ISO format)
     * @return list of wallet ledger entries
     */
    private List<LedgerEntryDto> fetchWalletLedger(String from, String to) {
        try {
            String url = walletServiceBase + "/api/admin/wallet/ledger";

            List<String> params = new ArrayList<>();
            if (from != null) params.add("fromDate=" + from);
            if (to   != null) params.add("toDate=" + to);

            if (!params.isEmpty()) {
                url += "?" + String.join("&", params);
            }

            LedgerEntryDto[] response =
                    restTemplate.getForObject(url, LedgerEntryDto[].class);

            return response != null ? Arrays.asList(response) : List.of();

        } catch (Exception ex) {
            /**
             * Fail-safe behavior:
             * - Wallet service failure should not block admin ledger
             */
            return List.of();
        }
    }

}
