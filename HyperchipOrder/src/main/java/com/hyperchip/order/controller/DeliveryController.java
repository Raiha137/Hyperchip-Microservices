package com.hyperchip.order.controller;

import com.hyperchip.order.service.DeliveryChargeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * DeliveryController
 *
 * Purpose:
 * - Provides REST endpoints to fetch delivery charges based on pin code.
 * - Helps frontend calculate shipping costs dynamically.
 *
 * Developer notes:
 * - All business logic (calculating delivery charges) is delegated to DeliveryChargeService.
 * - The controller only maps HTTP requests and builds response objects.
 * - Returns a simple JSON map containing the pin code and the calculated delivery charge.
 */
@RestController
@RequestMapping("/api/delivery")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryChargeService deliveryChargeService;

    /**
     * getCharge
     *
     * Flow:
     * 1. Accepts pinCode as a query parameter.
     * 2. Calls DeliveryChargeService to compute delivery charge for the given pin code.
     * 3. Builds a response map with pinCode and deliveryCharge.
     * 4. Returns HTTP 200 OK with JSON payload.
     *
     * Developer notes:
     * - Consider adding validation to ensure pinCode is in the correct format.
     * - This endpoint is simple but can be extended to return estimated delivery time, service type, or free delivery eligibility.
     *
     * @param pinCode the postal code for which to calculate delivery charges
     * @return ResponseEntity containing a JSON object with pinCode and deliveryCharge
     */
    @GetMapping("/charge")
    public ResponseEntity<?> getCharge(@RequestParam String pinCode) {
        // Fetch delivery charge for the given pin code
        double charge = deliveryChargeService.getDeliveryCharge(pinCode);

        // Build response as a map
        Map<String, Object> response = new HashMap<>();
        response.put("pinCode", pinCode);
        response.put("deliveryCharge", charge);

        // Return response to frontend
        return ResponseEntity.ok(response);
    }
}
