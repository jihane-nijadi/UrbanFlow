package com.example.ticket_service.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateTicketPayload {
    private String routeId;
    private String scheduleId;
    private String departureTime;
    private Integer quantity;
    private String passengerEmail;
    private Double price;
    private String departureStop;
    private String arrivalStop;

    // alternative keys used by frontend
    private String departureStopId;
    private String arrivalStopId;

    private Passenger passenger;

    // Bus identifier selected by the user at purchase time (e.g. "001", "BUS-12")
    private String bus;

    @Data
    public static class Passenger {
        private String name;
        private String email;
    }
}
