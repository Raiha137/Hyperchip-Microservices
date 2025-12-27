package com.hyperchip.order.repository;

import com.hyperchip.order.model.DeliveryZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing delivery zones.
 * Each zone can define delivery charges based on pin code, city, or region.
 */
@Repository
public interface DeliveryZoneRepository extends JpaRepository<DeliveryZone, Long> {

    /**
     * Find a delivery zone by its exact pin code.
     *
     * @param pinCode the PIN code to search
     * @return Optional containing the DeliveryZone if found, empty otherwise
     */
    Optional<DeliveryZone> findByPinCode(String pinCode);
}
