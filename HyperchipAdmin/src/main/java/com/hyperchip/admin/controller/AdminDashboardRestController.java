package com.hyperchip.admin.controller;

import com.hyperchip.admin.client.SalesReportClient;
import com.hyperchip.common.dto.DashboardResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardRestController {

    /**
     * Client used to fetch dashboard data from the Order Service.
     *
     * Purpose:
     * - Acts as a proxy between Admin UI and Order Service
     * - Delegates dashboard data aggregation to Order Service
     */
    private final SalesReportClient salesReportClient;

    /**
     * Provides dashboard data for the admin panel.
     *
     * Purpose:
     * - Fetches high-level business metrics for admin dashboard widgets
     * - Data varies based on selected time period
     */
    @GetMapping
    @ResponseBody
    public DashboardResponseDto getDashboardData(
            @RequestParam(defaultValue = "MONTH") String periodType
    ) {
        return salesReportClient.getDashboard(periodType);
    }

}
