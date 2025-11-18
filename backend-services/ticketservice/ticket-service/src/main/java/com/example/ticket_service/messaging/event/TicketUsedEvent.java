package com.example.ticket_service.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketUsedEvent {

    private Long ticketId;
    private String userId;
    private Long routeId;
    private Long scheduleId;
    private String routeName;
    private String departureStop;
    private String arrivalStop;
    private LocalDate travelDate; // Actual date of travel
    private LocalDateTime usedAt;
    private String scanLocation;
    @Builder.Default
    private String eventType = "TICKET_USED";
    private LocalDateTime eventTimestamp;
}
