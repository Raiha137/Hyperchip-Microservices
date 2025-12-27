package com.hyperchip.order.controller;

import com.hyperchip.common.dto.DashboardResponseDto;
import com.hyperchip.common.dto.SalesReportFilterDto;
import com.hyperchip.order.service.SalesReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * DashboardRestController
 *
 * Purpose:
 * - Provides administrative dashboard data for sales and order metrics.
 * - Supports aggregation by day, month, year, or a custom date range.
 *
 * Developer notes:
 * - Delegates all business logic to SalesReportService.
 * - Uses @ModelAttribute to automatically bind query parameters to the filter DTO.
 * - Returns a DashboardResponseDto which contains aggregated metrics, charts, and key figures.
 * - Example calls:
 *      1) /api/admin/dashboard?periodType=DAY
 *      2) /api/admin/dashboard?periodType=MONTH
 *      3) /api/admin/dashboard?periodType=YEAR
 *      4) /api/admin/dashboard?fromDate=2025-11-01&toDate=2025-11-26&periodType=CUSTOM
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardRestController {

    private final SalesReportService salesReportService;

    /**
     * getDashboard
     *
     * Flow:
     * 1. Accepts filtering parameters via SalesReportFilterDto:
     *      - periodType (DAY, MONTH, YEAR, CUSTOM)
     *      - fromDate, toDate (for CUSTOM range)
     * 2. Delegates to SalesReportService to compute dashboard metrics.
     * 3. Returns a DashboardResponseDto containing key metrics and summaries.
     *
     * Developer notes:
     * - All filtering and aggregation logic resides in the service layer.
     * - The controller is lightweight: its only responsibility is mapping HTTP requests
     *   to the service and returning the response.
     *
     * @param filter SalesReportFilterDto containing filtering parameters
     * @return DashboardResponseDto containing aggregated dashboard metrics
     */
    @GetMapping
    public DashboardResponseDto getDashboard(@ModelAttribute SalesReportFilterDto filter) {
        return salesReportService.getDashboardData(filter);
    }
}
