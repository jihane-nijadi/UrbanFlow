package com.bus.route.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteDTO {
    private Long id;
    private String routeId;
    private String routeName;
    private List<ScheduleDTO> schedules;
}
