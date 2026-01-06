package com.hyperchip.admin.client;

import com.hyperchip.common.dto.DashboardResponseDto;
import com.hyperchip.common.dto.SalesReportFilterDto;
import com.hyperchip.common.dto.SalesReportResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class SalesReportClient {

    /**
     * RestTemplate used for communicating with Order Service APIs.
     * This client acts as a bridge between Admin Service and Order Service.
     */
    private final RestTemplate restTemplate;

    /**
     * Base URL of the Order Service.
     * Used mainly for Sales Report related APIs.
     */
    @Value("${order.service.url}")
    private String orderServiceBase;

    /**
     * Base URL of the Order Service.
     * Used specifically for Dashboard-related API calls.
     * (Kept separate intentionally to match existing configuration usage)
     */
    @Value("${order.service.url}")
    private String orderServiceBaseUrl;

    /**
     * Builds the base URL for Sales Report APIs.
     *
     * Purpose:
     * - Ensures there is no trailing slash in the configured base URL
     * - Appends the fixed sales report endpoint path
     *
     * Final URL format:
     * {orderServiceBase}/api/admin/reports/sales
     */
    private String baseUrl() {
        String base = orderServiceBase;

        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        return base + "/api/admin/reports/sales";
    }

    /**
     * Fetches the sales report data based on the provided filter criteria.
     *
     * Purpose:
     * - Sends filter parameters (date range, order status, payment type, etc.)
     *   to the Order Service
     * - Retrieves structured sales report data for admin UI display
     *
     * HTTP Method:
     * - POST
     *
     * Endpoint:
     * - /api/admin/reports/sales
     *
     * @param filter SalesReportFilterDto containing report filter options
     * @return SalesReportResponseDto containing rows and summary data
     */
    public SalesReportResponseDto getReport(SalesReportFilterDto filter) {
        String url = baseUrl();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<SalesReportFilterDto> entity =
                new HttpEntity<>(filter, headers);

        ResponseEntity<SalesReportResponseDto> response =
                restTemplate.postForEntity(url, entity, SalesReportResponseDto.class);

        return response.getBody();
    }

    /**
     * Exports the sales report as a PDF file.
     *
     * Purpose:
     * - Used when admin clicks "Export PDF" in the dashboard
     * - Returns raw PDF bytes to be streamed as a downloadable file
     *
     * HTTP Method:
     * - POST
     *
     * Endpoint:
     * - /api/admin/reports/sales/export/pdf
     *
     * @param filter SalesReportFilterDto defining report scope
     * @return byte[] representing the generated PDF file
     */
    public byte[] exportPdf(SalesReportFilterDto filter) {
        String url = baseUrl() + "/export/pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<SalesReportFilterDto> entity =
                new HttpEntity<>(filter, headers);

        ResponseEntity<byte[]> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        entity,
                        byte[].class
                );

        return response.getBody();
    }

    /**
     * Exports the sales report as an Excel file.
     *
     * Purpose:
     * - Allows admin to download sales data in Excel format
     * - Useful for accounting, auditing, or offline analysis
     *
     * HTTP Method:
     * - POST
     *
     * Endpoint:
     * - /api/admin/reports/sales/export/excel
     *
     * @param filter SalesReportFilterDto defining report scope
     * @return byte[] representing the generated Excel file
     */
    public byte[] exportExcel(SalesReportFilterDto filter) {
        String url = baseUrl() + "/export/excel";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<SalesReportFilterDto> entity =
                new HttpEntity<>(filter, headers);

        ResponseEntity<byte[]> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        entity,
                        byte[].class
                );

        return response.getBody();
    }

    /**
     * Fetches dashboard summary data for the admin panel.
     *
     * Purpose:
     * - Provides high-level metrics such as:
     *   - Total sales
     *   - Total orders
     *   - Revenue trends
     * - Data is calculated based on the selected period type
     *
     * HTTP Method:
     * - GET
     *
     * Endpoint:
     * - /api/admin/dashboard?periodType={type}
     *
     * @param periodType Time range filter (DAILY, WEEKLY, MONTHLY, YEARLY)
     * @return DashboardResponseDto containing aggregated dashboard metrics
     */
    public DashboardResponseDto getDashboard(String periodType) {
        String url = UriComponentsBuilder
                .fromHttpUrl(orderServiceBaseUrl + "/api/admin/dashboard")
                .queryParam("periodType", periodType)
                .toUriString();


        return restTemplate.getForObject(url, DashboardResponseDto.class);
    }

}
