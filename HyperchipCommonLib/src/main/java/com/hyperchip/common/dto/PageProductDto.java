//package com.hyperchip.common.dto;
//
//import java.io.Serializable;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Objects;
//
//public class PageProductDto implements Serializable {
//    private static final long serialVersionUID = 1L;
//
//    private List<ProductDto> content = new ArrayList<>();
//    private int totalPages;
//    private int number;
//    private int size;
//    private long totalElements;
//    private boolean first;
//    private boolean last;
//
//    public PageProductDto() {}
//
//    public List<ProductDto> getContent() { return content; }
//    public void setContent(List<ProductDto> content) { this.content = content; }
//
//    public int getTotalPages() { return totalPages; }
//    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
//
//    public int getNumber() { return number; }
//    public void setNumber(int number) { this.number = number; }
//
//    public int getSize() { return size; }
//    public void setSize(int size) { this.size = size; }
//
//    public long getTotalElements() { return totalElements; }
//    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
//
//    public boolean isFirst() { return first; }
//    public void setFirst(boolean first) { this.first = first; }
//
//    public boolean isLast() { return last; }
//    public void setLast(boolean last) { this.last = last; }
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof PageProductDto)) return false;
//        PageProductDto that = (PageProductDto) o;
//        return number == that.number && size == that.size;
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(number, size);
//    }
//}
//
//
//
//
//
//
package com.hyperchip.common.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class PageProductDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<ProductDto> content;
    private int totalPages;
    private int number;
    private int size;
    private long totalElements;
    private boolean first;
    private boolean last;



    public PageProductDto() {}

    public List<ProductDto> getContent() { return content; }
    public void setContent(List<ProductDto> content) { this.content = content; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public int getNumber() { return number; }
    public void setNumber(int number) { this.number = number; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

    public boolean isFirst() { return first; }
    public void setFirst(boolean first) { this.first = first; }

    public boolean isLast() { return last; }
    public void setLast(boolean last) { this.last = last; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PageProductDto)) return false;
        PageProductDto that = (PageProductDto) o;
        return number == that.number && size == that.size;
    }

    @Override
    public int hashCode() {
        return Objects.hash(number, size);
    }
}
