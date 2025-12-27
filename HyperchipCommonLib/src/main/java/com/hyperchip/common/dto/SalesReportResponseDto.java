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
public class SalesReportResponseDto {
    private List<SalesReportRowDto> rows;
    private SalesReportSummaryDto summary;
}
