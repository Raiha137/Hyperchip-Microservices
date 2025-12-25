package com.hyperchip.user.controller;

import com.hyperchip.common.dto.AddressDto;
import com.hyperchip.user.model.Address;
import com.hyperchip.user.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MigrationController
 * ------------------
 * Provides internal endpoints for data migration and export.
 *
 * Purpose:
 * - Allows exporting existing addresses from the database
 * - Converts Address entities to AddressDto for external processing or migration
 * - Intended for internal or admin use only
 */
@RestController
@RequestMapping("/internal/migrate")
@RequiredArgsConstructor
public class MigrationController {

    /**
     * Repository to access Address data from the database
     */
    private final AddressRepository addressRepository;

    /**
     * exportAddresses
     * ---------------
     * GET /internal/migrate/addresses/export
     *
     * Purpose:
     * - Fetch all addresses from the database
     * - Map each Address entity to AddressDto
     * - Return the list for migration or backup purposes
     */
    @GetMapping("/addresses/export")
    public List<AddressDto> exportAddresses() {
        List<Address> all = addressRepository.findAll();
        return all.stream().map(a -> AddressDto.builder()
                .id(a.getId())
                .userId(a.getUser() != null ? a.getUser().getId() : null)
                .label(a.getLabel())
                .addressLine1(a.getAddressLine1())
                .addressLine2(a.getAddressLine2())
                .city(a.getCity())
                .state(a.getState())
                .pincode(a.getPincode())
                .country(a.getCountry())
                .contactName(a.getContactName())
                .contactPhone(a.getContactPhone())
                .isDefault(a.getIsDefault())
                .build()
        ).collect(Collectors.toList());
    }
}