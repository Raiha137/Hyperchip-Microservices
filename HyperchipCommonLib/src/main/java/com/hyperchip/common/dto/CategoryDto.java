package com.hyperchip.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data               // Generates getters, setters, toString, equals, and hashCode
@NoArgsConstructor  // Generates a no-args constructor
@AllArgsConstructor // Generates an all-args constructor
public class CategoryDto {
    private Long id;
    private String name;
    private String imageName;
    private Boolean active; // use Boolean, not boolean
    @JsonProperty("active_name")
    private String activeName;

}

