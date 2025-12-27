package com.hyperchip.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesReportFilterDto {
    private LocalDate fromDate;
    private LocalDate toDate;

    // "DAY", "WEEK", "MONTH", "CUSTOM"
    private String periodType;

    // optional filters
    private String paymentMethod; // COD, RAZORPAY, WALLET...
    private String orderStatus;   // PENDING, DELIVERED, CANCELLED...
}
