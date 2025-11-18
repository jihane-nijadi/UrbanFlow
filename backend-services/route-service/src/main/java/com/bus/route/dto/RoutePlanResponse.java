package com.bus.route.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutePlanResponse {
    private String routeId;
    private String routeName;
    private String departureStop;
    private String arrivalStop;
    private String departureTime;
    private String arrivalTime;
    private Integer estimatedDurationMinutes;
    private List<Map<String, Object>> busesAvailable;
}
