package com.hyperchip.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponseDto {

    /** Summary numbers (reuse existing summary structure) */
    private SalesReportSummaryDto summary;

    /** Data points for chart (daily / monthly etc.) */
    private List<DashboardChartPointDto> chartPoints;

    /** Top best selling products (top 10) */
    private List<BestSellerProductDto> topProducts;

    /** Top best selling categories (will be filled in admin/master) */
    private List<BestSellerCategoryDto> topCategories;

    /** Top best selling brands (will be filled in admin/master) */
    private List<BestSellerBrandDto> topBrands;
}
