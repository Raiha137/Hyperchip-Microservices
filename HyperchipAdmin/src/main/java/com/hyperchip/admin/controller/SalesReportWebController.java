package com.hyperchip.admin.controller;

import com.hyperchip.admin.client.SalesReportClient;
import com.hyperchip.common.dto.SalesReportFilterDto;
import com.hyperchip.common.dto.SalesReportResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * SalesReportWebController
 *
 * Purpose:
 * Handles admin-side views for Sales Reports and Ledger.
 * Provides:
 * - Sales report page
 * - Export to PDF/Excel
 * - Ledger page
 */
@Controller
@RequiredArgsConstructor
public class SalesReportWebController {

    private final SalesReportClient salesReportClient;

    // ===================================================
    // REDIRECT ROOT REPORTS
    // ===================================================
    /**
     * Purpose:
     * Redirect /admin/reports to default sales report page.
     * Called when admin clicks "Reports" in header.
     */
    @GetMapping("/admin/reports")
    public String reportsRoot() {
        return "redirect:/admin/reports/sales";
    }

    // ===================================================
    // SALES REPORT PAGE
    // ===================================================
    /**
     * Purpose:
     * Render the sales report page with applied filters.
     *
     * What it does:
     * - Accepts SalesReportFilterDto from request (dates, category, etc.)
     * - Calls SalesReportClient to fetch filtered report
     * - Passes report and filter back to Thymeleaf
     */
    @GetMapping("/admin/reports/sales")
    public String salesReportPage(@ModelAttribute("filter") SalesReportFilterDto filter,
                                  Model model) {

        if (filter == null) {
            filter = new SalesReportFilterDto();
        }

        SalesReportResponseDto report = salesReportClient.getReport(filter);

        model.addAttribute("report", report);
        model.addAttribute("filter", filter);
        model.addAttribute("activePage", "reports"); // for header highlighting

        return "admin/reports/sales-report"; // Thymeleaf view
    }

    // ===================================================
    // EXPORT SALES REPORT TO PDF
    // ===================================================
    /**
     * Purpose:
     * Export the sales report as a PDF file.
     *
     * What it does:
     * - Calls SalesReportClient to generate PDF bytes
     * - Returns ResponseEntity with appropriate headers for download
     */
    @GetMapping("/admin/reports/sales/export/pdf")
    public ResponseEntity<byte[]> exportPdf(@ModelAttribute("filter") SalesReportFilterDto filter) {
        byte[] pdf = salesReportClient.exportPdf(filter);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sales-report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ===================================================
    // EXPORT SALES REPORT TO EXCEL
    // ===================================================
    /**
     * Purpose:
     * Export the sales report as an Excel file (.xlsx).
     *
     * What it does:
     * - Calls SalesReportClient to generate Excel bytes
     * - Returns ResponseEntity with appropriate headers for download
     */
    @GetMapping("/admin/reports/sales/export/excel")
    public ResponseEntity<byte[]> exportExcel(@ModelAttribute("filter") SalesReportFilterDto filter) {
        byte[] excel = salesReportClient.exportExcel(filter);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sales-report.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }

    // ===================================================
    // LEDGER REPORT PAGE
    // ===================================================
    /**
     * Purpose:
     * Render the admin ledger report page.
     *
     * What it does:
     * - Sets activePage for header highlighting
     * - Returns the Thymeleaf template for ledger
     */
    @GetMapping("/admin/reports/ledger")
    public String ledgerReportPage(Model model) {
        model.addAttribute("activePage", "ledger");
        return "admin/admin-ledger"; // Thymeleaf view for ledger
    }
}
