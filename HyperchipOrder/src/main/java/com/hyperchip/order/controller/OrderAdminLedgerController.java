package com.hyperchip.order.controller;

import com.hyperchip.common.dto.LedgerEntryDto;
import com.hyperchip.order.model.Order;
import com.hyperchip.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * OrderAdminLedgerController
 *
 * Purpose:
 * - Provides a ledger view of orders for administrative purposes.
 * - Allows filtering by date range.
 * - Computes credit/debit direction based on payment status.
 *
 * Developer notes:
 * - Uses OrderRepository to fetch orders from the database.
 * - Converts Order entities into LedgerEntryDto for frontend consumption.
 * - By default, all orders are treated as CREDIT; refunded orders are DEBIT.
 * - Date filtering is inclusive: fromDate ≤ createdAt < toDate + 1 day.
 */
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class OrderAdminLedgerController {

    private final OrderRepository orderRepository;

    /**
     * getOrderLedger
     *
     * Flow:
     * 1. Accept optional fromDate and toDate query parameters.
     * 2. If both dates are provided, fetch orders within the inclusive date range.
     * 3. If dates are not provided, fetch all orders.
     * 4. Map each Order to LedgerEntryDto to include payment info and amount.
     *
     * Developer notes:
     * - fromDate and toDate are expected in ISO date format (yyyy-MM-dd).
     * - Amount is calculated as paidAmount if available; otherwise falls back to totalAmount.
     *
     * @param fromDate optional start date for ledger filter
     * @param toDate   optional end date for ledger filter
     * @return list of LedgerEntryDto representing order ledger entries
     */
    @GetMapping("/ledger")
    public List<LedgerEntryDto> getOrderLedger(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate
    ) {
        List<Order> orders;

        if (fromDate != null && toDate != null) {
            // Inclusive fromDate and toDate → add 1 day to end
            Instant start = fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
            Instant end = toDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
            orders = orderRepository.findByCreatedAtBetween(start, end);
        } else {
            // No filter → fetch all orders
            orders = orderRepository.findAll();
        }

        // Convert orders to LedgerEntryDto for frontend
        return orders.stream()
                .map(this::toLedgerEntry)
                .collect(Collectors.toList());
    }

    /**
     * Convert Order entity to LedgerEntryDto
     *
     * Flow:
     * 1. Default direction is CREDIT (money coming in).
     * 2. If paymentStatus is REFUNDED, direction is DEBIT.
     * 3. Determine amount using paidAmount first, then totalAmount as fallback.
     * 4. Populate all relevant ledger fields.
     *
     * @param o Order entity
     * @return LedgerEntryDto or null if input is null
     */
    private LedgerEntryDto toLedgerEntry(Order o) {
        if (o == null) return null;

        // Determine direction of money
        String direction = "CREDIT";
        if (o.getPaymentStatus() != null
                && "REFUNDED".equalsIgnoreCase(o.getPaymentStatus())) {
            direction = "DEBIT";
        }

        // Calculate amount: prioritize paidAmount, fallback to totalAmount
        Double amount = o.getPaidAmount() != null && o.getPaidAmount() > 0
                ? o.getPaidAmount()
                : (o.getTotalAmount() != null ? o.getTotalAmount() : 0.0);

        return LedgerEntryDto.builder()
                .type("ORDER")
                .direction(direction)
                .orderId(o.getId())
                .orderNumber(o.getOrderNumber())
                .userId(o.getUserId())
                .userEmail(o.getUserEmail())
                .paymentMethod(o.getPaymentMethod())
                .source(o.getPaymentStatus())   // store paymentStatus as source info
                .amount(amount)
                .createdAt(o.getCreatedAt())
                .build();
    }
}
