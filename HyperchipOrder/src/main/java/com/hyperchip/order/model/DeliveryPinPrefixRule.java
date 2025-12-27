package com.hyperchip.order.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing delivery charges based on PIN code prefixes.
 * This allows calculating delivery fees by matching the starting digits of a postal code.
 */
@Entity
@Table(name = "delivery_pin_prefix_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryPinPrefixRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Unique ID for the PIN prefix rule

    @Column(nullable = false, length = 10)
    private String prefix; // Starting digits of the PIN code to which this charge applies

    @Column(nullable = false)
    private Double charge; // Delivery charge for this PIN prefix
}
