package com.hyperchip.order.service.impl;
import com.hyperchip.common.dto.SalesReportFilterDto;
import com.hyperchip.common.dto.SalesReportResponseDto;
import com.hyperchip.common.dto.SalesReportRowDto;
import com.hyperchip.common.dto.SalesReportSummaryDto;
import com.hyperchip.order.model.CouponUsage;
import com.hyperchip.order.model.Order;
import com.hyperchip.order.model.OrderStatus;
import com.hyperchip.order.repository.CouponUsageRepository;
import com.hyperchip.order.repository.OrderRepository;
import com.hyperchip.order.service.SalesReportService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.hyperchip.common.dto.BestSellerProductDto;
import com.hyperchip.common.dto.BestSellerCategoryDto;
import com.hyperchip.common.dto.BestSellerBrandDto;
import com.hyperchip.common.dto.DashboardChartPointDto;
import com.hyperchip.common.dto.DashboardResponseDto;
import com.hyperchip.order.model.OrderItem;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
// PDF
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import org.apache.poi.ss.usermodel.*;
// Excel
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Implementation of SalesReportService.
 *
 * <p>This service provides functionality to generate sales reports,
 * dashboard data, and export reports as PDF or Excel files.</p>
 *
 * <p>It fetches orders from the OrderRepository, coupon usage
 * from CouponUsageRepository, and optionally product/category/brand
 * details via RestTemplate calls.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SalesReportServiceImpl implements SalesReportService {
    private final OrderRepository orderRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final org.springframework.web.client.RestTemplate restTemplate;
    @org.springframework.beans.factory.annotation.Value("${product.service.url:http://localhost:8086}")
    private String productServiceBase;
    @org.springframework.beans.factory.annotation.Value("${master.service.url:http://localhost:8086}")
    private String masterServiceBase;
    @org.springframework.beans.factory.annotation.Value("${uploads.base.url:http://localhost:8086}")
    private String uploadsBaseUrl;

    /**
     * Generate sales report based on the provided filter.
     * Returns a DTO containing rows (individual orders) and summary.
     */
    @Override
    public SalesReportResponseDto getSalesReport(SalesReportFilterDto filter) {
        if (filter == null) {
            filter = new SalesReportFilterDto();
        }
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        LocalDate from = filter.getFromDate();
        LocalDate to = filter.getToDate();
        String period = filter.getPeriodType() != null ? filter.getPeriodType().toUpperCase() : "DAY";
        switch (period) {
            case "WEEK":
                if (from == null) from = today.minusDays(6);
                if (to == null) to = today;
                break;
            case "MONTH":
                if (from == null) from = today.withDayOfMonth(1);
                if (to == null) to = today;
                break;
            case "YEAR":
                if (from == null) from = today.withDayOfYear(1);
                if (to == null) to = today;
                break;
            case "CUSTOM":
                if (from == null) from = today;
                if (to == null) to = today;
                break;
            case "DAY":
            default:
                if (from == null) from = today;
                if (to == null) to = from;
                break;
        }

        // Convert dates to Instant for repository query
        Instant start = from.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        // Fetch orders within the date range
        List<Order> orders = orderRepository.findByCreatedAtBetween(start, end);

        // Apply filters if specified
        if (filter.getPaymentMethod() != null && !filter.getPaymentMethod().isBlank()) {
            String pm = filter.getPaymentMethod().trim();
            orders = orders.stream()
                    .filter(o -> o.getPaymentMethod() != null && o.getPaymentMethod().equalsIgnoreCase(pm))
                    .collect(Collectors.toList());
        }
        if (filter.getOrderStatus() != null && !filter.getOrderStatus().isBlank()) {
            OrderStatus st = OrderStatus.valueOf(filter.getOrderStatus().toUpperCase());
            orders = orders.stream()
                    .filter(o -> o.getStatus() == st)
                    .collect(Collectors.toList());
        }
        if (orders.isEmpty()) {
            return SalesReportResponseDto.builder()
                    .rows(Collections.emptyList())
                    .summary(SalesReportSummaryDto.builder()
                            .totalOrders(0L)
                            .totalOrderAmount(BigDecimal.ZERO)
                            .totalCouponDiscount(BigDecimal.ZERO)
                            .totalFinalAmount(BigDecimal.ZERO)
                            .overallDiscount(BigDecimal.ZERO)
                            .build())
                    .build();
        }
        List
                <Long>
                orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());
        Map<Long, BigDecimal> couponDiscountMap = couponUsageRepository.findByOrderIdIn(orderIds).stream()
                .collect(Collectors.toMap(
                        CouponUsage::getOrderId,
                        cu -> cu.getDiscountAmount() != null ? cu.getDiscountAmount() : BigDecimal.ZERO,
                        BigDecimal::add
                ));
        List
                <SalesReportRowDto>
                rows = new ArrayList<>();
        BigDecimal totalOrderAmount = BigDecimal.ZERO;
        BigDecimal totalCouponDiscount = BigDecimal.ZERO;
        BigDecimal totalFinalAmount = BigDecimal.ZERO;
        for (Order o : orders) {
            BigDecimal subtotal = BigDecimal.valueOf(
                    o.getSubtotal() != null ? o.getSubtotal() : 0.0
            );
            if (subtotal.compareTo(BigDecimal.ZERO) <= 0 && o.getOrderItems() != null) {
                double sum = o.getOrderItems().stream()
                        .mapToDouble(it -> it.getTotal() != null ? it.getTotal() : 0.0)
                        .sum();
                subtotal = BigDecimal.valueOf(sum);
            }
            BigDecimal finalAmount = BigDecimal.valueOf(
                    o.getTotalAmount() != null ? o.getTotalAmount() : subtotal.doubleValue()
            );
// ✅ 1) Try value from CouponUsage (if you store it there)
            BigDecimal couponDiscount = couponDiscountMap.getOrDefault(o.getId(), BigDecimal.ZERO);
// ✅ 2) If still zero, calculate from subtotal - finalAmount
            if (couponDiscount == null || couponDiscount.compareTo(BigDecimal.ZERO) <= 0) {
                BigDecimal diff = subtotal.subtract(finalAmount);
                if (diff.compareTo(BigDecimal.ZERO) > 0) {
                    couponDiscount = diff;
                } else {
                    couponDiscount = BigDecimal.ZERO;
                }
            }
            SalesReportRowDto row = SalesReportRowDto.builder()
                    .orderId(o.getId())
                    .orderNumber(o.getOrderNumber())
                    .orderDate(LocalDateTime.ofInstant(o.getCreatedAt(), ZoneId.systemDefault()))
                    .userId(o.getUserId())
                    .userEmail(o.getUserEmail())
                    .paymentMethod(o.getPaymentMethod())
                    .paymentStatus(o.getPaymentStatus())
                    .orderStatus(o.getStatus() != null ? o.getStatus().name() : null)
                    .subTotal(subtotal)
                    .couponDiscount(couponDiscount)   // 👈 now has real amount
                    .finalAmount(finalAmount)
                    .build();
            rows.add(row);
            totalOrderAmount = totalOrderAmount.add(subtotal);
            totalCouponDiscount = totalCouponDiscount.add(couponDiscount);
            totalFinalAmount = totalFinalAmount.add(finalAmount);
        }
        BigDecimal overallDiscount = totalOrderAmount.subtract(totalFinalAmount);
        SalesReportSummaryDto summary = SalesReportSummaryDto.builder()
                .totalOrders((long) rows.size())
                .totalOrderAmount(totalOrderAmount)
                .totalCouponDiscount(totalCouponDiscount)
                .totalFinalAmount(totalFinalAmount)
                .overallDiscount(overallDiscount)
                .build();
        return SalesReportResponseDto.builder()
                .rows(rows)
                .summary(summary)
                .build();
    }
    @Override
    public DashboardResponseDto getDashboardData(SalesReportFilterDto filter) {
        if (filter == null) {
            filter = new SalesReportFilterDto();
        }
        String period = filter.getPeriodType() != null
                ? filter.getPeriodType().toUpperCase()
                : "MONTH";
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        LocalDate from;
        LocalDate to;
        switch (period) {
            case "DAY" -> {
                from = today;
                to = today;
            }
            case "WEEK" -> {
                from = today.minusDays(6);
                to = today;
            }
            case "MONTH" -> {
                from = today.withDayOfMonth(1);
                to = today;
            }
            case "YEAR" -> {
                from = today.withDayOfYear(1);
                to = today;
            }
            default -> {
                from = today.withDayOfMonth(1);
                to = today;
            }
        }
        Instant start = from.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
// 1) load orders and ignore cancelled / failed
        List
                <Order>
                orders = orderRepository.findByCreatedAtBetween(start, end).stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED
                        && o.getStatus() != OrderStatus.PAYMENT_FAILED)
                .toList();
        if (orders.isEmpty()) {
            return DashboardResponseDto.builder()
                    .summary(SalesReportSummaryDto.builder()
                            .totalOrders(0L)
                            .totalOrderAmount(BigDecimal.ZERO)
                            .totalCouponDiscount(BigDecimal.ZERO)
                            .totalFinalAmount(BigDecimal.ZERO)
                            .overallDiscount(BigDecimal.ZERO)
                            .build())
                    .chartPoints(Collections.emptyList())
                    .topProducts(Collections.emptyList())
                    .topCategories(Collections.emptyList())
                    .topBrands(Collections.emptyList())
                    .build();
        }
// --------------------------------------------------------------------
// 2) Summary + chart points
// --------------------------------------------------------------------
// --------------------------------------------------------------------
// 2) Summary + chart points
// --------------------------------------------------------------------
        Map<LocalDate, BigDecimal> amountByDate = new HashMap<>();
        Map<LocalDate, Long> orderCountByDate = new HashMap<>();
        BigDecimal totalOrderAmount = BigDecimal.ZERO;
        BigDecimal totalFinalAmount = BigDecimal.ZERO;
        BigDecimal totalCouponDiscount = BigDecimal.ZERO;
        for (Order o : orders) {
            LocalDate d = LocalDateTime.ofInstant(o.getCreatedAt(), ZoneId.systemDefault()).toLocalDate();
            BigDecimal subtotal = BigDecimal.valueOf(
                    o.getSubtotal() != null ? o.getSubtotal() : 0.0
            );
            BigDecimal finalAmount = BigDecimal.valueOf(
                    o.getTotalAmount() != null ? o.getTotalAmount() : 0.0
            );
            totalOrderAmount = totalOrderAmount.add(subtotal);
            totalFinalAmount = totalFinalAmount.add(finalAmount);
            BigDecimal diff = subtotal.subtract(finalAmount);
            if (diff.compareTo(BigDecimal.ZERO) > 0) {
                totalCouponDiscount = totalCouponDiscount.add(diff);
            }
            amountByDate.merge(d, finalAmount, BigDecimal::add);
            orderCountByDate.merge(d, 1L, Long::sum);
        }
        BigDecimal overallDiscount = totalOrderAmount.subtract(totalFinalAmount);
        long totalOrders = orders.size();
        List
                <DashboardChartPointDto>
                chartPoints = amountByDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> DashboardChartPointDto.builder()
                        .label(e.getKey().toString())
                        .totalAmount(e.getValue()) // 👈 BigDecimal, NOT double
                        .orderCount(orderCountByDate.getOrDefault(e.getKey(), 0L))
                        .build()
                )
                .collect(Collectors.toList());
        SalesReportSummaryDto summary = SalesReportSummaryDto.builder()
                .totalOrders(totalOrders)
                .totalOrderAmount(totalOrderAmount)
                .totalCouponDiscount(totalCouponDiscount)
                .totalFinalAmount(totalFinalAmount)
                .overallDiscount(overallDiscount)
                .build();
// --------------------------------------------------------------------
// 3) Aggregate by product (+ category, brand)
// --------------------------------------------------------------------
        Map<Long, ProductAgg> productAgg = new HashMap<>();
        for (Order o : orders) {
            if (o.getOrderItems() == null) continue;
            for (OrderItem it : o.getOrderItems()) {
                if (Boolean.TRUE.equals(it.isCancelled())) continue;
                if (it.getProductId() == null) continue;
                ProductAgg agg = productAgg.computeIfAbsent(it.getProductId(), k -> new ProductAgg());
                agg.totalQty += safeInt(it.getQuantity());
                agg.totalAmount = agg.totalAmount.add(
                        BigDecimal.valueOf(it.getTotal() != null ? it.getTotal() : 0.0)
                );
            }
        }
        if (productAgg.isEmpty()) {
            return DashboardResponseDto.builder()
                    .summary(summary)
                    .chartPoints(chartPoints)
                    .topProducts(Collections.emptyList())
                    .topCategories(Collections.emptyList())
                    .topBrands(Collections.emptyList())
                    .build();
        }
        Map<Long, CategoryAgg> categoryAgg = new HashMap<>();
        Map<Long, BrandAgg> brandAgg = new HashMap<>();
        List
                <BestSellerProductDto>
                productList = new ArrayList<>();
        for (Map.Entry<Long, ProductAgg> e : productAgg.entrySet()) {
            Long productId = e.getKey();
            ProductAgg pAgg = e.getValue();
            Map<String, Object> prod = fetchProductPublicOrAdmin(productId);
// try all possible keys: productName, name, title
            String productName = null;
            if (prod != null) {
                if (prod.get("productName") != null) {
                    productName = prod.get("productName").toString();
                } else if (prod.get("name") != null) {
                    productName = prod.get("name").toString();
                } else if (prod.get("title") != null) {   // fallback to title
                    productName = prod.get("title").toString();
                }
            }
            Long categoryId = extractLong(prod, "categoryId");
            String categoryName = prod != null && prod.get("categoryName") != null
                    ? prod.get("categoryName").toString()
                    : "Unknown";
            Long brandId = extractLong(prod, "brandId");
            String brandName = prod != null && prod.get("brandName") != null
                    ? prod.get("brandName").toString()
                    : "Unknown";
// 👉 this is what your JS reads: productName
            productList.add(
                    BestSellerProductDto.builder()
                            .productId(productId)
                            .productName(productName != null ? productName : "#" + productId)
                            .totalQuantity(pAgg.totalQty)
                            .totalAmount(pAgg.totalAmount)
                            .build()
            );
// category aggregation
            if (categoryId != null) {
                CategoryAgg cAgg = categoryAgg.computeIfAbsent(
                        categoryId,
                        id -> new CategoryAgg(id, categoryName)
                );
                cAgg.totalQty += pAgg.totalQty;
                cAgg.totalAmount = cAgg.totalAmount.add(pAgg.totalAmount);
                if (categoryName != null) cAgg.name = categoryName;
            }
// brand aggregation
            if (brandId != null) {
                BrandAgg bAgg = brandAgg.computeIfAbsent(
                        brandId,
                        id -> new BrandAgg(id, brandName)
                );
                bAgg.totalQty += pAgg.totalQty;
                bAgg.totalAmount = bAgg.totalAmount.add(pAgg.totalAmount);
                if (brandName != null) bAgg.name = brandName;
            }
        }
// --------------------------------------------------------------------
// 4) Sort and keep TOP 10
// --------------------------------------------------------------------
        List
                <BestSellerProductDto>
                topProducts = productList.stream()
                .sorted(Comparator.comparing(BestSellerProductDto::getTotalQuantity).reversed())
                .limit(10)
                .collect(Collectors.toList());
        List
                <BestSellerCategoryDto>
                topCategories = categoryAgg.values().stream()
                .sorted(Comparator.comparing((CategoryAgg c) -> c.totalQty).reversed())
                .limit(10)
                .map(c -> BestSellerCategoryDto.builder()
                        .categoryId(c.id)
                        .categoryName(c.name)
                        .totalQuantity(c.totalQty)
                        .totalAmount(c.totalAmount)
                        .build())
                .collect(Collectors.toList());
        List
                <BestSellerBrandDto>
                topBrands = brandAgg.values().stream()
                .sorted(Comparator.comparing((BrandAgg b) -> b.totalQty).reversed())
                .limit(10)
                .map(b -> BestSellerBrandDto.builder()
                        .brandId(b.id)
                        .brandName(b.name)
                        .totalQuantity(b.totalQty)
                        .totalAmount(b.totalAmount)
                        .build())
                .collect(Collectors.toList());
        return DashboardResponseDto.builder()
                .summary(summary)
                .chartPoints(chartPoints)
                .topProducts(topProducts)
                .topCategories(topCategories)
                .topBrands(topBrands)
                .build();
    }
    @Override
    public byte[] exportPdf(SalesReportResponseDto report) {
        try {
            Document doc = new Document(PageSize.A4.rotate());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(doc, baos);
            doc.open();
            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD, Color.BLACK);
            Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.BLACK);
            Font normalFont = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.BLACK);
            doc.add(new Paragraph("Sales Report", titleFont));
            doc.add(new Paragraph("Generated at: " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), normalFont));
            doc.add(new Paragraph(" "));
            PdfPTable table = new PdfPTable(9);
            table.setWidthPercentage(100f);
            table.setWidths(new float[]{2.5f, 3f, 4f, 3f, 2.5f, 2.5f, 2.5f, 2.5f, 2.5f});
            addHeaderCell(table, "Order #", headerFont);
            addHeaderCell(table, "Date/Time", headerFont);
            addHeaderCell(table, "User Email", headerFont);
            addHeaderCell(table, "Payment Method", headerFont);
            addHeaderCell(table, "Status", headerFont);
            addHeaderCell(table, "Subtotal", headerFont);
            addHeaderCell(table, "Coupon Disc.", headerFont);
            addHeaderCell(table, "Final Amount", headerFont);
            addHeaderCell(table, "Order Id", headerFont);
            if (report.getRows() != null) {
                for (var r : report.getRows()) {
                    addBodyCell(table, safe(r.getOrderNumber()), normalFont);
                    addBodyCell(table, r.getOrderDate() != null ?
                            r.getOrderDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "", normalFont);
                    addBodyCell(table, safe(r.getUserEmail()), normalFont);
                    addBodyCell(table, safe(r.getPaymentMethod()), normalFont);
                    addBodyCell(table, safe(r.getOrderStatus()), normalFont);
                    addBodyCell(table, r.getSubTotal() != null ? r.getSubTotal().toString() : "0", normalFont);
                    addBodyCell(table, r.getCouponDiscount() != null ? r.getCouponDiscount().toString() : "0", normalFont);
                    addBodyCell(table, r.getFinalAmount() != null ? r.getFinalAmount().toString() : "0", normalFont);
                    addBodyCell(table, r.getOrderId() != null ? String.valueOf(r.getOrderId()) : "", normalFont);
                }
            }
            doc.add(table);
            doc.add(new Paragraph(" "));
            SalesReportSummaryDto s = report.getSummary();
            if (s != null) {
                doc.add(new Paragraph("Summary", headerFont));
                doc.add(new Paragraph("Total Orders: " + s.getTotalOrders(), normalFont));
                doc.add(new Paragraph("Total Order Amount: " + s.getTotalOrderAmount(), normalFont));
                doc.add(new Paragraph("Total Coupon Discount: " + s.getTotalCouponDiscount(), normalFont));
                doc.add(new Paragraph("Total Final Amount: " + s.getTotalFinalAmount(), normalFont));
                doc.add(new Paragraph("Overall Discount: " + s.getOverallDiscount(), normalFont));
            }
            doc.close();
            return baos.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to export PDF", ex);
        }
    }
    @Override
    public byte[] exportExcel(SalesReportResponseDto report) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Sales Report");
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();  // ✅ POI Font
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            int rowIdx = 0;
            Row header = sheet.createRow(rowIdx++);
            String[] cols = {
                    "Order #", "Date/Time", "User Email", "Payment Method",
                    "Status", "Subtotal", "Coupon Disc.", "Final Amount", "Order Id"
            };
            for (int i = 0; i < cols.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(headerStyle);
            }
            if (report.getRows() != null) {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                for (var r : report.getRows()) {
                    Row row = sheet.createRow(rowIdx++);
                    int col = 0;
                    row.createCell(col++).setCellValue(safe(r.getOrderNumber()));
                    row.createCell(col++).setCellValue(
                            r.getOrderDate() != null ? r.getOrderDate().format(dtf) : "");
                    row.createCell(col++).setCellValue(safe(r.getUserEmail()));
                    row.createCell(col++).setCellValue(safe(r.getPaymentMethod()));
                    row.createCell(col++).setCellValue(safe(r.getOrderStatus()));
                    row.createCell(col++).setCellValue(
                            r.getSubTotal() != null ? r.getSubTotal().doubleValue() : 0.0);
                    row.createCell(col++).setCellValue(
                            r.getCouponDiscount() != null ? r.getCouponDiscount().doubleValue() : 0.0);
                    row.createCell(col++).setCellValue(
                            r.getFinalAmount() != null ? r.getFinalAmount().doubleValue() : 0.0);
                    row.createCell(col++).setCellValue(
                            r.getOrderId() != null ? r.getOrderId() : 0L);
                }
            }
            for (int i = 0; i < cols.length; i++) {
                sheet.autoSizeColumn(i);
            }
// summary sheet
            Sheet summarySheet = workbook.createSheet("Summary");
            SalesReportSummaryDto s = report.getSummary();
            if (s != null) {
                int r = 0;
                Row row = summarySheet.createRow(r++);
                row.createCell(0).setCellValue("Total Orders");
                row.createCell(1).setCellValue(s.getTotalOrders());
                row = summarySheet.createRow(r++);
                row.createCell(0).setCellValue("Total Order Amount");
                row.createCell(1).setCellValue(
                        s.getTotalOrderAmount() != null ? s.getTotalOrderAmount().doubleValue() : 0.0);
                row = summarySheet.createRow(r++);
                row.createCell(0).setCellValue("Total Coupon Discount");
                row.createCell(1).setCellValue(
                        s.getTotalCouponDiscount() != null ? s.getTotalCouponDiscount().doubleValue() : 0.0);
                row = summarySheet.createRow(r++);
                row.createCell(0).setCellValue("Total Final Amount");
                row.createCell(1).setCellValue(
                        s.getTotalFinalAmount() != null ? s.getTotalFinalAmount().doubleValue() : 0.0);
                row = summarySheet.createRow(r++);
                row.createCell(0).setCellValue("Overall Discount");
                row.createCell(1).setCellValue(
                        s.getOverallDiscount() != null ? s.getOverallDiscount().doubleValue() : 0.0);
                summarySheet.autoSizeColumn(0);
                summarySheet.autoSizeColumn(1);
            }
            workbook.write(baos);
            return baos.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to export Excel", ex);
        }
    }
    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBackgroundColor(Color.LIGHT_GRAY);
        table.addCell(c);
    }
    private void addBodyCell(PdfPTable table, String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        table.addCell(c);
    }
    private String safe(String v) {
        return v != null ? v : "";
    }
    private double safeDouble(Double v) {
        return v != null ? v : 0.0;
    }
    private int safeInt(Integer v) {
        return v != null ? v : 0;
    }
    private Long extractLong(Map<String, Object> map, String key) {
        if (map == null || key == null) return null;
        Object v = map.get(key);
        if (v instanceof Number n) {
            return n.longValue();
        }
        if (v instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }
    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchProductPublicOrAdmin(Long productId) {
        if (productId == null) return null;
        String base = (productServiceBase != null && !productServiceBase.isBlank())
                ? productServiceBase
                : masterServiceBase;
        if (base == null || base.isBlank()) base = uploadsBaseUrl;
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        String publicUrl = base + "/public/products/" + productId;
        try {
            Map<String, Object> resp = restTemplate.getForObject(publicUrl, Map.class);
            if (resp != null) return resp;
        } catch (Exception ex) {
            log.debug("fetchProductPublicOrAdmin: public fetch failed for {} : {}", productId, ex.getMessage());
        }
        String adminUrl = base + "/api/admin/products/" + productId;
        try {
            Map<String, Object> resp2 = restTemplate.getForObject(adminUrl, Map.class);
            if (resp2 != null) return resp2;
        } catch (Exception ex) {
            log.debug("fetchProductPublicOrAdmin: admin fetch failed for {} : {}", productId, ex.getMessage());
        }
        return null;
    }
    // --- small aggregation helpers ---
    private static class ProductAgg {
        long totalQty = 0L;
        BigDecimal totalAmount = BigDecimal.ZERO;
    }
    private static class CategoryAgg {
        Long id;
        String name;
        long totalQty = 0L;
        BigDecimal totalAmount = BigDecimal.ZERO;
        CategoryAgg(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }
    private static class BrandAgg {
        Long id;
        String name;
        long totalQty = 0L;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BrandAgg(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}


/**
 * SalesReportServiceImpl
 *
 * This service implementation class provides all the functionality related to sales reporting
 * and dashboard analytics for the Hyperchip e-commerce application. It implements the
 * SalesReportService interface and handles:
 *
 * 1. Sales Report Generation
 *    - Fetches orders from the database based on date filters and optional criteria like
 *      payment method and order status.
 *    - Calculates subtotal, final amount, coupon discounts, and overall discount.
 *    - Aggregates data into a list of SalesReportRowDto objects for individual orders.
 *    - Computes summary data in SalesReportSummaryDto for total orders, total amounts,
 *      total discounts, and overall discount.
 *
 * 2. Dashboard Analytics
 *    - Provides aggregated dashboard data using getDashboardData() method.
 *    - Aggregates order data for a specified period (day, week, month, year).
 *    - Ignores cancelled or payment-failed orders.
 *    - Prepares chart data for visualization (DashboardChartPointDto) with total amount
 *      and order count per day.
 *    - Aggregates top-selling products, categories, and brands with quantity and total
 *      amount.
 *    - Communicates with product/master microservices using RestTemplate to fetch product,
 *      category, and brand information.
 *
 * 3. Export Functionality
 *    - Supports export of sales reports to PDF format using iText library.
 *      - Adds header and body cells.
 *      - Includes summary details at the bottom of the PDF.
 *    - Supports export of sales reports to Excel format using Apache POI.
 *      - Creates a sheet with order details.
 *      - Adds a separate summary sheet for totals.
 *
 * 4. Utility Methods
 *    - Safe handling of null values for strings, integers, and doubles.
 *    - Helper methods for adding PDF table cells.
 *    - fetchProductPublicOrAdmin(): fetches product info from public or admin endpoints.
 *    - extractLong(): safely extracts a Long value from a Map<String, Object>.
 *    - Aggregation helper classes (ProductAgg, CategoryAgg, BrandAgg) for internal
 *      computation of top-selling items.
 *
 * Dependencies:
 *    - OrderRepository: For querying orders.
 *    - CouponUsageRepository: For fetching coupon usage and discount amounts.
 *    - RestTemplate: For HTTP calls to fetch product/category/brand details.
 *    - iText (com.lowagie.text): For PDF generation.
 *    - Apache POI: For Excel export.
 *
 * Notes:
 *    - All methods are transactional.
 *    - Lombok annotations (@RequiredArgsConstructor, @Slf4j) are used for constructor
 *      injection and logging.
 *    - Handles various period types: DAY, WEEK, MONTH, YEAR, CUSTOM.
 *    - Gracefully handles missing or null data in orders, products, categories, and brands.
 *
 * Author: Developer (Hyperchip Project)
 * Version: 1.0
 */
