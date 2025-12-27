package com.hyperchip.order.repository;

import com.hyperchip.order.model.DeliveryPinPrefixRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for managing delivery charges based on PIN code prefixes.
 * Allows fetching rules like extra delivery fees for specific PIN code ranges.
 */
@Repository
public interface DeliveryPinPrefixRuleRepository extends JpaRepository<DeliveryPinPrefixRule, Long> {

    /**
     * Fetch all delivery PIN prefix rules, ordered descending by prefix.
     *
     * @return List of DeliveryPinPrefixRule sorted by prefix descending
     */
    List<DeliveryPinPrefixRule> findAllByOrderByPrefixDesc();
}
