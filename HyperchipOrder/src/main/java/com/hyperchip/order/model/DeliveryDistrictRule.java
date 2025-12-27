package com.hyperchip.order.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing delivery charges for specific districts.
 * Used to calculate delivery fee based on the user's location.
 */
@Entity
@Table(name = "delivery_district_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryDistrictRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Unique ID for the delivery rule

    @Column(length = 50)
    private String state; // State name or code

    @Column(length = 50)
    private String district; // District name

    private Double charge; // Delivery charge for this district
}
