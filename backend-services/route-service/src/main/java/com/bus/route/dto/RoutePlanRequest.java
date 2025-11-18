package com.bus.route.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutePlanRequest {
    @NotBlank(message = "Departure stop is required")
    private String from;
    
    @NotBlank(message = "Arrival stop is required")
    private String to;
}
