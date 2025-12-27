package com.hyperchip.order.service.impl;

import com.hyperchip.common.dto.AddressDto;
import com.hyperchip.order.repository.DeliveryZoneRepository;
import com.hyperchip.order.repository.DeliveryDistrictRuleRepository;
import com.hyperchip.order.repository.DeliveryPinPrefixRuleRepository;
import com.hyperchip.order.service.DeliveryChargeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service implementation for calculating delivery charges based on:
 * - Exact PIN code match
 * - PIN code prefix rules
 * - State + District/City rules
 * - Default charge if no match found
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryChargeServiceImpl implements DeliveryChargeService {

    private final DeliveryZoneRepository deliveryZoneRepository;                 // ✅ exact pin codes
    private final DeliveryDistrictRuleRepository deliveryDistrictRuleRepository; // ✅ district/city rules
    private final DeliveryPinPrefixRuleRepository deliveryPinPrefixRuleRepository; // ✅ prefix rules like "670***"

    // ======================================================
    // GET DELIVERY CHARGE BY PINCODE ONLY
    // ======================================================
    /**
     * Calculate delivery charge using only the PIN code.
     *
     * @param pinCode The delivery PIN code
     * @return Delivery charge amount
     */
    @Override
    public double getDeliveryCharge(String pinCode) {
        String pin = normalize(pinCode);
        if (pin == null) return 100.0; // default charge if pin invalid

        // 1️⃣ Exact PIN match
        Double exact = deliveryZoneRepository.findByPinCode(pin)
                .map(z -> z.getCharge())
                .orElse(null);
        if (exact != null) return exact;

        // 2️⃣ Fallback to prefix rules (e.g., 670***)
        for (var rule : deliveryPinPrefixRuleRepository.findAllByOrderByPrefixDesc()) {
            String prefix = normalize(rule.getPrefix());
            if (prefix != null && pin.startsWith(prefix)) {
                return rule.getCharge() != null ? rule.getCharge() : 100.0;
            }
        }

        // 3️⃣ Default delivery charge if no match found
        return 100.0;
    }

    // ======================================================
    // GET DELIVERY CHARGE FOR FULL ADDRESS
    // ======================================================
    /**
     * Calculate delivery charge using full AddressDto.
     *
     * Order of checks:
     * 1. Exact PIN code match
     * 2. State + District/City match
     * 3. Default charge
     *
     * @param address Address details
     * @return Delivery charge
     */
    @Override
    public double getDeliveryChargeForAddress(AddressDto address) {
        if (address == null) return 100.0;

        // Normalize inputs
        String pin = normalize(address.getPincode());
        String state = normalize(address.getState());
        String districtOrCity = normalize(address.getCity()); // city/district

        // 1️⃣ Exact PIN code match
        if (pin != null) {
            Double byPin = deliveryZoneRepository.findByPinCode(pin)
                    .map(z -> z.getCharge())
                    .orElse(null);
            if (byPin != null) return byPin;
        }

        // 2️⃣ District/City rule fallback
        if (state != null && districtOrCity != null) {
            Double byDistrict = deliveryDistrictRuleRepository
                    .findByStateIgnoreCaseAndDistrictIgnoreCase(state, districtOrCity)
                    .map(r -> r.getCharge())
                    .orElse(null);
            if (byDistrict != null) return byDistrict;
        }

        // 3️⃣ Default charge
        return 100.0;
    }

    // ======================================================
    // HELPER METHOD
    // ======================================================
    /**
     * Normalize input string by trimming whitespace and returning null if empty.
     *
     * @param s Input string
     * @return Normalized string or null
     */
    private String normalize(String s) {
        if (s == null) return null;
        s = s.trim();
        return s.isEmpty() ? null : s;
    }
}
