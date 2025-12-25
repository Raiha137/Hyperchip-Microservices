package com.hyperchip.user.controller;

import com.hyperchip.common.dto.AddressDto;
import com.hyperchip.user.model.Address;
import com.hyperchip.user.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AddressRestController
 * ---------------------
 * REST controller responsible for exposing address-related APIs
 * from the User Service.
 *
 * Purpose:
 * - Provide address data to other services or frontend clients
 * - Return only active (non-deleted) addresses
 * - Convert internal Address entities into safe DTOs
 */
@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressRestController {

    /**
     * addressRepository
     * -----------------
     * Repository used to interact with the Address table in the database.
     *
     * Why repository is used directly here:
     * - This controller exposes a simple read-only API
     * - No complex business logic is required for this endpoint
     */
    private final AddressRepository addressRepository;

    /**
     * getAddressById
     * --------------
     * Fetches a single address by its ID.
     *
     * Endpoint:
     * GET /api/addresses/{id}
     *
     * Purpose:
     * - Used by frontend or other microservices (e.g., Order Service)
     * - Ensures soft-deleted addresses are NOT exposed
     *
     * Response behavior:
     * - 200 OK      : Address exists and is not deleted
     * - 404 NotFound: Address does not exist or is marked as deleted
     */
    @GetMapping("/{id}")
    public ResponseEntity<AddressDto> getAddressById(@PathVariable Long id) {

        return addressRepository.findById(id)

                // Filter out soft-deleted addresses
                .filter(address -> Boolean.FALSE.equals(address.getDeleted()))

                // Convert entity to DTO
                .map(this::toDto)

                // Wrap DTO in HTTP 200 response
                .map(ResponseEntity::ok)

                // Return 404 if address is not found or deleted
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * toDto
     * -----
     * Converts Address entity into AddressDto.
     *
     * Why DTO is used:
     * - Prevents exposing internal entity structure
     * - Avoids lazy-loading issues (e.g., User entity)
     * - Ensures stable API contract across services
     *
     * Special handling:
     * - User ID is extracted safely to avoid NullPointerException
     */
    private AddressDto toDto(Address address) {

        return AddressDto.builder()
                .id(address.getId())
                .userId(address.getUser() != null ? address.getUser().getId() : null)
                .label(address.getLabel())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .state(address.getState())
                .pincode(address.getPincode())
                .country(address.getCountry())
                .contactName(address.getContactName())
                .contactPhone(address.getContactPhone())
                .isDefault(address.getIsDefault())
                .build();
    }
}
