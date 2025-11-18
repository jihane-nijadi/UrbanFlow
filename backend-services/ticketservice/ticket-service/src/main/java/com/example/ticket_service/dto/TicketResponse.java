package com.example.ticket_service.dto;

import com.example.ticket_service.entities.TicketStatus;
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
public class TicketResponse {

    private Long id;
    private String userId;
    private Long routeId;
    private Long scheduleId;
    private String routeName;
    private String departureStop;
    private String arrivalStop;
    private String departureTime;
    private String arrivalTime;
    private LocalDateTime travelDate;
    private BigDecimal price;
    private TicketStatus status;
    private LocalDateTime purchaseDate;
    private LocalDateTime validUntil;
    private String qrCode;
}
