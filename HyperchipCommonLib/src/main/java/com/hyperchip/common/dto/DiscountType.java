package com.hyperchip.common.dto;

/**
 * Enumeration representing the type of discount a coupon applies.
 * A percentage discount represents a percentage off the order total,
 * while a flat discount represents a fixed monetary amount off the order.
 */
public enum DiscountType {
    /** Percentage-based discount (e.g. 10% off). */
    PERCENT,
    /** Fixed amount discount (e.g. $50 off). */
    FLAT
}
