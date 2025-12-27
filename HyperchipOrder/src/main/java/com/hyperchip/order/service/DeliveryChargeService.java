package com.hyperchip.order.service;

import com.hyperchip.common.dto.AddressDto;

/**
 * Service interface for calculating delivery charges in Hyperchip e-commerce.
 *
 * <p>This interface provides methods to determine delivery charges based on:</p>
 * <ul>
 *     <li>Pin code only</li>
 *     <li>Full address information</li>
 * </ul>
 */
public interface DeliveryChargeService {

    /**
     * Calculate delivery charge based on the provided pin code.
     *
     * @param pinCode the destination pin code
     * @return the delivery charge as a double
     */
    double getDeliveryCharge(String pinCode);

    /**
     * Calculate delivery charge based on the full address.
     *
     * <p>This is useful when you have a complete AddressDto, for example
     * from the OrderService or checkout process.</p>
     *
     * @param address the AddressDto containing details like pin code, city, state
     * @return the delivery charge as a double
     */
    double getDeliveryChargeForAddress(AddressDto address);
}
