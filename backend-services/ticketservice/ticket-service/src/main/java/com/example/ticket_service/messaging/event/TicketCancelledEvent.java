package com.example.ticket_service.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketCancelledEvent {

    private Long ticketId;
    private String userId;
    private Long routeId;
    private Long scheduleId;
    private String routeName;
    private String departureStop;
    private String arrivalStop;
    private String departureTime; // Time as String (e.g., "09:00")
    private LocalDate travelDate; // Actual date of travel
    private BigDecimal refundAmount;
    private String cancellationReason;
    @Builder.Default
    private String eventType = "TICKET_CANCELLED";
    private LocalDateTime eventTimestamp;
}
