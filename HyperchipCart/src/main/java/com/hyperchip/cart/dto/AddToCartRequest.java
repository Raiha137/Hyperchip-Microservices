package com.hyperchip.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * AddToCartRequest
 *
 * This DTO is used when a user adds a product
 * to their shopping cart.
 *
 * It carries only the required data needed
 * for the add-to-cart operation.
 */
@Data
public class AddToCartRequest {

    /**
     * Unique identifier of the user
     * who is adding the product to the cart.
     */
    @NotNull(message = "User ID must not be null")
    private Long userId;

    /**
     * Unique identifier of the product
     * that needs to be added to the cart.
     */
    @NotNull(message = "Product ID must not be null")
    private Long productId;

    /**
     * Quantity of the product to add.
     *
     * Default value is set to 1 to make
     * add-to-cart easier for users.
     * Minimum allowed value is 1.
     */
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity = 1;
}
