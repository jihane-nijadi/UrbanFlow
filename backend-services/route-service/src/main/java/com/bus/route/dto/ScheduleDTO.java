package com.bus.route.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleDTO {
    private Long id;
    private String routeId;
    private String departureStop;
    private String arrivalStop;
    private String departureTime;
    private String arrivalTime;
}
