package com.example.ticket_service.mapper;

import com.example.ticket_service.entities.Ticket;
import com.example.ticket_service.dto.TicketResponse;
import org.springframework.stereotype.Component;

@Component
public class TicketMapper {

    /**
     * Note: toEntity() method removed - tickets are now created directly in service
     * with data fetched from Route Service, not mapped directly from request
     */

    public TicketResponse toResponse(Ticket ticket) {
        return toResponse(ticket, null);
    }

    public TicketResponse toResponse(Ticket ticket, String paymentUrl) {
        return TicketResponse.builder()
                .id(ticket.getId())
                .userId(ticket.getUserId())
                .routeId(ticket.getRouteId())
                .scheduleId(ticket.getScheduleId())
                .routeName(ticket.getRouteName())
                .departureStop(ticket.getDepartureStop())
                .arrivalStop(ticket.getArrivalStop())
                .departureTime(ticket.getDepartureTime())
                .arrivalTime(ticket.getArrivalTime())
                .travelDate(ticket.getTravelDate())
                .price(ticket.getPrice())
                .status(ticket.getStatus())
                .purchaseDate(ticket.getPurchaseDate())
                .validUntil(ticket.getValidUntil())
                .qrCode(ticket.getQrCode())
                .paymentUrl(paymentUrl)
                .build();
    }
}
