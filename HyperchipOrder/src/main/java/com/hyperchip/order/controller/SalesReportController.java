// src/main/java/com/hyperchip/order/controller/SalesReportController.java
package com.hyperchip.order.controller;

import com.hyperchip.common.dto.SalesReportFilterDto;
import com.hyperchip.common.dto.SalesReportResponseDto;
import com.hyperchip.order.service.SalesReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for admin sales reports.
 * Purpose: Allows admins to generate, view, and export sales reports in PDF/Excel format.
 */
@RestController
@RequestMapping("/api/admin/reports/sales")  // matches client usage
@RequiredArgsConstructor
public class SalesReportController {

    private final SalesReportService salesReportService;

    // ====================================================
    // GENERATE SALES REPORT
    // ====================================================
    /**
     * Purpose: Returns the sales report based on the given filter.
     * Example filters: date range, product category, or specific user.
     */
    @PostMapping
    public SalesReportResponseDto getReport(@RequestBody SalesReportFilterDto filter) {
        return salesReportService.getSalesReport(filter);
    }

    // ====================================================
    // EXPORT SALES REPORT AS PDF
    // ====================================================
    /**
     * Purpose: Generates the sales report as a PDF file and returns it as a download.
     */
    @PostMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(@RequestBody SalesReportFilterDto filter) {
        SalesReportResponseDto report = salesReportService.getSalesReport(filter);
        byte[] data = salesReportService.exportPdf(report);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sales-report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    // ====================================================
    // EXPORT SALES REPORT AS EXCEL
    // ====================================================
    /**
     * Purpose: Generates the sales report as an Excel file and returns it as a download.
     */
    @PostMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(@RequestBody SalesReportFilterDto filter) {
        SalesReportResponseDto report = salesReportService.getSalesReport(filter);
        byte[] data = salesReportService.exportExcel(report);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sales-report.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }
}
