package com.example.ticket_service.messaging.event;

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
public class TicketCreatedEvent {

    private Long ticketId;
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
    // Persisted bus identifier (if available)
    private String bus;
    private LocalDateTime purchaseDate;
    @Builder.Default
    private String eventType = "TICKET_CREATED";
    private LocalDateTime eventTimestamp;
}
