package com.hyperchip.common.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.math.BigDecimal;
// OfferScope is in same package (com.hyperchip.common.dto), so no extra import needed


public class ProductDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String description;
    private Double price;
    private Integer stock;
    private Long categoryId;
    private Long brandId;
    private List<String> imageNames;
    private Boolean active;
    private Boolean deleted;

    private String categoryName;
    private String brandName;

    private BigDecimal originalPrice;
    private BigDecimal offerDiscount;
    private BigDecimal finalPrice;
    private OfferScope appliedScope;
    private Long appliedOfferId;


    private Double discount;
    private Double discountPrice;

    public ProductDto() {}

    // Getters / Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Double getDiscount() { return discount; }
    public void setDiscount(Double discount) { this.discount = discount; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public Long getBrandId() { return brandId; }
    public void setBrandId(Long brandId) { this.brandId = brandId; }

    public List<String> getImageNames() { return imageNames; }
    public void setImageNames(List<String> imageNames) { this.imageNames = imageNames; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Boolean getDeleted() { return deleted; }
    public void setDeleted(Boolean deleted) { this.deleted = deleted; }

    public Double getDiscountPrice() { return discountPrice; }
    public void setDiscountPrice(Double discountPrice) { this.discountPrice = discountPrice; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }


    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }

    public BigDecimal getOfferDiscount() { return offerDiscount; }
    public void setOfferDiscount(BigDecimal offerDiscount) { this.offerDiscount = offerDiscount; }

    public BigDecimal getFinalPrice() { return finalPrice; }
    public void setFinalPrice(BigDecimal finalPrice) { this.finalPrice = finalPrice; }

    public OfferScope getAppliedScope() { return appliedScope; }
    public void setAppliedScope(OfferScope appliedScope) { this.appliedScope = appliedScope; }

    public Long getAppliedOfferId() { return appliedOfferId; }
    public void setAppliedOfferId(Long appliedOfferId) { this.appliedOfferId = appliedOfferId; }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductDto)) return false;
        ProductDto that = (ProductDto) o;
        return Objects.equals(id, that.id);
    }
    @Override
    public int hashCode() { return Objects.hash(id); }
}
