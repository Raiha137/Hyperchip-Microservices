package com.hyperchip.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardChartPointDto {

    /**
     * Label shown in chart.
     * - For DAY/WEEK/MONTH: yyyy-MM-dd
     * - For YEAR: yyyy-MM (month)
     */
    private String label;

    /** Total final sales amount for this bucket */
    private BigDecimal totalAmount;

    /** Number of orders in this bucket */
    private Long orderCount;
}
