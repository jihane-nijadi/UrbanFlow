package com.example.ticket_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing Route information from Route Service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteDTO {
    private Long id;
    private String routeId;      // ex: "R10"
    private String routeName;
    private List<ScheduleDTO> schedules;
}
