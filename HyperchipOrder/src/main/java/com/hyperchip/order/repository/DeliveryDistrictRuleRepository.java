package com.hyperchip.order.repository;

import com.hyperchip.order.model.DeliveryDistrictRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing delivery charges based on state and district.
 * Stores rules such as extra delivery fees per district.
 */
@Repository
public interface DeliveryDistrictRuleRepository extends JpaRepository<DeliveryDistrictRule, Long> {

    /**
     * Find a delivery rule by state and district, ignoring case.
     *
     * @param state the state name
     * @param district the district name
     * @return an Optional containing the matching DeliveryDistrictRule
     */
    Optional<DeliveryDistrictRule> findByStateIgnoreCaseAndDistrictIgnoreCase(String state, String district);
}
