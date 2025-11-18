package com.example.ticket_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing Schedule information from Route Service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleDTO {
    private Long id;
    private String routeId;
    private String departureStop;
    private String arrivalStop;
    private String departureTime;    // "08:00"
    private String arrivalTime;      // "08:25"
}
