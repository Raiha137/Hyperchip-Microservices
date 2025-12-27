package com.hyperchip.common.dto;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class PageCategoryDto {
    private List<CategoryDto> content;
    private int totalPages;
    private int number;   // current page
    private int size;     // page size
    private boolean first;
    private boolean last;
}
