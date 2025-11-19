package com.example.ticket_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketRequest {
    // Keep fields flexible for development/testing: validation is performed in service when needed
    private String userId;
    private Long routeId;
    private String routeCode; // external code or descriptor from frontend (e.g. BUS-001|09:30|mock)
    private Long scheduleId;
    private LocalDateTime travelDate;
    private BigDecimal price;

    // Optional descriptive fields provided by the frontend when route service data is not available
    private String departureStop;
    private String arrivalStop;
}
