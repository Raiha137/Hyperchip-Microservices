package com.hyperchip.order.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing a delivery zone with associated charges.
 * Each zone can be defined by a PIN code, city, district, state, and country.
 * The delivery charge for this zone is specified in the charge field.
 */
@Entity
@Table(name = "delivery_zones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Unique ID for the delivery zone

    @Column(name = "pin_code", length = 10)
    private String pinCode; // Optional PIN code defining this zone

    @Column(length = 50)
    private String city; // City name of the delivery zone

    @Column(length = 50)
    private String district; // District name of the delivery zone

    @Column(length = 50)
    private String state; // State name of the delivery zone

    @Column(length = 50)
    private String country; // Country name of the delivery zone

    private Double charge; // Delivery charge for this zone (e.g., 40.00)
}
