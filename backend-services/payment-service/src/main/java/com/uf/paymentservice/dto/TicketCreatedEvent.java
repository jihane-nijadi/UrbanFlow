package com.uf.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import com.fasterxml.jackson.databind.JsonNode;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketCreatedEvent {
    private Long ticketId;
    private String userId;
    private String routeId;
    private String scheduleId;
    private String routeName;
    private String departureStop;
    private String arrivalStop;
    // Some producers send times as simple strings ("09:30") or arrays; accept them as raw strings and parse if needed
    private String departureTime;
    private String arrivalTime;
    private JsonNode travelDate;
    private BigDecimal price;
    private String bus;
    private JsonNode purchaseDate;
    private JsonNode eventTimestamp;
}
