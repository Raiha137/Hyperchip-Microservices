package com.hyperchip.order.service;

import com.hyperchip.common.dto.SalesReportFilterDto;
import com.hyperchip.common.dto.SalesReportResponseDto;
import com.hyperchip.common.dto.DashboardResponseDto;

/**
 * Service interface for generating sales reports and dashboard data.
 *
 * <p>This interface provides methods for:</p>
 * <ul>
 *     <li>Generating detailed sales reports (rows + summary)</li>
 *     <li>Building dashboard data for charts and top-selling products, categories, and brands</li>
 *     <li>Exporting sales reports as PDF or Excel files</li>
 * </ul>
 */
public interface SalesReportService {

    /**
     * Generate a detailed sales report including individual order rows and summary totals,
     * based on the given filter criteria (period, payment method, order status, etc.).
     *
     * @param filter filter criteria for the report; can be null for default period
     * @return sales report response containing rows and summary
     */
    SalesReportResponseDto getSalesReport(SalesReportFilterDto filter);

    /**
     * Generate dashboard data including summary totals, chart points, and top-selling products,
     * categories, and brands.
     *
     * @param filter filter criteria for the dashboard; can be null for default period
     * @return dashboard response containing aggregated data
     */
    DashboardResponseDto getDashboardData(SalesReportFilterDto filter);

    /**
     * Export the given sales report as a PDF.
     *
     * @param report the sales report to export
     * @return byte array representing the generated PDF file
     */
    byte[] exportPdf(SalesReportResponseDto report);

    /**
     * Export the given sales report as an Excel spreadsheet.
     *
     * @param report the sales report to export
     * @return byte array representing the generated Excel file
     */
    byte[] exportExcel(SalesReportResponseDto report);
}
