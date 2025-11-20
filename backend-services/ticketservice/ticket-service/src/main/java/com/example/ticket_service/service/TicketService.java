package com.example.ticket_service.service;

import com.example.ticket_service.dto.RouteDTO;
import com.example.ticket_service.dto.ScheduleDTO;
import com.example.ticket_service.entities.Ticket;
import com.example.ticket_service.entities.TicketStatus;
import com.example.ticket_service.dto.TicketRequest;
import com.example.ticket_service.dto.TicketResponse;
import com.example.ticket_service.exception.ResourceNotFoundException;
import com.example.ticket_service.exception.SeatAlreadyBookedException;
import com.example.ticket_service.mapper.TicketMapper;
import com.example.ticket_service.messaging.event.TicketCancelledEvent;
import com.example.ticket_service.messaging.event.TicketCreatedEvent;
import com.example.ticket_service.messaging.event.TicketUsedEvent;
import com.example.ticket_service.messaging.publisher.TicketEventPublisher;
import com.example.ticket_service.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketMapper ticketMapper;
    private final TicketEventPublisher eventPublisher;
    private final RouteServiceClient routeServiceClient;

    @Value("${PAYMENT_SERVICE_URL:http://payment-service:8085}")
    private String paymentServiceUrl;
    @Value("${PAYMENT_SERVICE_PUBLIC_URL:}")
    private String paymentServicePublicUrl;

    @Transactional
    public TicketResponse createTicket(TicketRequest request) {
        log.info("Creating ticket for user: {} on route: {}, schedule: {}", 
                request.getUserId(), request.getRouteId(), request.getScheduleId());


        // Attempt to fetch route and schedule information from Route Service.
        // If route service is unavailable or IDs are missing, fall back to using provided payload values
        RouteDTO route = null;
        ScheduleDTO schedule = null;
        try {
            if (request.getRouteId() != null) route = routeServiceClient.getRouteById(request.getRouteId());
        } catch (Exception e) {
            log.warn("Route service unavailable or route not found for id {}: {}", request.getRouteId(), e.getMessage());
        }
        try {
            if (request.getScheduleId() != null) schedule = routeServiceClient.getScheduleById(request.getScheduleId());
        } catch (Exception e) {
            log.warn("Route service unavailable or schedule not found for id {}: {}", request.getScheduleId(), e.getMessage());
        }

        // If schedule is available, validate it belongs to the route
        if (schedule != null && route != null) {
            if (!schedule.getRouteId().equals(route.getRouteId())) {
                throw new IllegalArgumentException(
                        "Schedule " + request.getScheduleId() + " does not belong to route " + request.getRouteId());
            }
        }

        // Check seat availability (limit: 70 seats per schedule per date) only when scheduleId and travelDate are present
        if (request.getScheduleId() != null && request.getTravelDate() != null) {
            long bookedSeats = ticketRepository.countByScheduleIdAndTravelDate(
                    request.getScheduleId(), request.getTravelDate());
            if (bookedSeats >= 70) {
                throw new SeatAlreadyBookedException(
                        "All seats are booked for this schedule on " + request.getTravelDate() + " (70/70 seats taken)");
            }
        }

        // Ensure we have numeric identifiers for route and schedule.
        // If the frontend didn't provide numeric IDs, derive stable numeric IDs from descriptive codes.
        Long effectiveRouteId = request.getRouteId();
        Long effectiveScheduleId = request.getScheduleId();
        if (effectiveRouteId == null && request.getRouteCode() != null) {
            effectiveRouteId = deterministicIdFromString(request.getRouteCode());
            log.info("Derived numeric routeId={} from routeCode={}", effectiveRouteId, request.getRouteCode());
        }
        if (effectiveScheduleId == null) {
            // Attempt to derive schedule id from available attributes (routeCode or stops + travelDate)
            String scheduleKey = null;
            if (request.getRouteCode() != null) scheduleKey = request.getRouteCode();
            else scheduleKey = (request.getDepartureStop() != null ? request.getDepartureStop() : "") + "|" +
                    (request.getArrivalStop() != null ? request.getArrivalStop() : "") + "|" +
                    (request.getTravelDate() != null ? request.getTravelDate().toString() : "");
            effectiveScheduleId = deterministicIdFromString(scheduleKey);
            log.info("Derived numeric scheduleId={} from key={}", effectiveScheduleId, scheduleKey);
        }

        // Create ticket with route and schedule information (use effective numeric ids)
        Ticket.TicketBuilder tb = Ticket.builder()
            .userId(request.getUserId())
            .routeId(effectiveRouteId)
            .scheduleId(effectiveScheduleId)
            .travelDate(request.getTravelDate())
            .price(request.getPrice())
            .status(TicketStatus.PENDING)
            .qrCode(generateQRCode());

        if (request.getBus() != null) {
            tb.bus(request.getBus());
        }

        if (route != null) tb.routeName(route.getRouteName());
        else if (request.getRouteCode() != null) tb.routeName(request.getRouteCode());
        else tb.routeName("Ad-hoc");
        if (schedule != null) {
            tb.departureStop(schedule.getDepartureStop())
                .arrivalStop(schedule.getArrivalStop())
                .departureTime(schedule.getDepartureTime())
                .arrivalTime(schedule.getArrivalTime());
        } else {
            tb.departureStop(request.getDepartureStop() != null ? request.getDepartureStop() : "Unknown")
                .arrivalStop(request.getArrivalStop() != null ? request.getArrivalStop() : "Unknown")
                .departureTime(request.getTravelDate() != null ? request.getTravelDate().toLocalTime().toString() : "")
                .arrivalTime("");
        }

        Ticket ticket = tb.build();

        Ticket savedTicket = ticketRepository.save(ticket);
        String routeNameStr = (route != null) ? route.getRouteName() : savedTicket.getRouteName();
        String routeStops = "";
        if (schedule != null) routeStops = schedule.getDepartureStop() + " -> " + schedule.getArrivalStop();
        else routeStops = savedTicket.getDepartureStop() + " -> " + savedTicket.getArrivalStop();
        log.info("Ticket created successfully with ID: {} for route: {} ({})", 
            savedTicket.getId(), routeNameStr, routeStops);

        // Publish ticket created event to RabbitMQ
        publishTicketCreatedEvent(savedTicket);

        // Do not return a direct payment page URL here. Payment should be handled via the payment-service session
        // or via the frontend payment dashboard. Returning a paymentUrl that points at payment-service
        // causes clients to be redirected to the raw payment page; avoid that by returning the ticket response
        // without a paymentUrl and let the frontend create a payment session when needed.
        return ticketMapper.toResponse(savedTicket);
    }

    public TicketResponse getTicketById(Long id) {
        log.debug("Fetching ticket with ID: {}", id);
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with ID: " + id));
        return ticketMapper.toResponse(ticket);
    }

    public List<TicketResponse> getTicketsByUserId(String userId) {
        log.debug("Fetching tickets for user: {}", userId);
        return ticketRepository.findByUserId(userId).stream()
                .map(ticketMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<TicketResponse> getUpcomingTickets(String userId) {
        log.debug("Fetching upcoming tickets for user: {}", userId);
        return ticketRepository.findUpcomingTicketsByUserId(userId, LocalDateTime.now()).stream()
                .map(ticketMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<TicketResponse> getActiveTickets(String userId) {
        log.debug("Fetching active tickets for user: {}", userId);
        return ticketRepository.findByUserIdAndStatus(userId, TicketStatus.ACTIVE).stream()
                .map(ticketMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TicketResponse cancelTicket(Long id, String userId) {
        log.info("Cancelling ticket with ID: {} for user: {}", id, userId);

        Ticket ticket = ticketRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket not found with ID: " + id + " for user: " + userId));

        if (ticket.getStatus() != TicketStatus.ACTIVE) {
            throw new IllegalStateException("Only active tickets can be cancelled");
        }

        ticket.setStatus(TicketStatus.CANCELLED);
        Ticket cancelledTicket = ticketRepository.save(ticket);

        log.info("Ticket cancelled successfully: {}", id);

        // Publish ticket cancelled event to RabbitMQ
        publishTicketCancelledEvent(cancelledTicket, "User requested cancellation");

        return ticketMapper.toResponse(cancelledTicket);
    }

    @Transactional
    public TicketResponse useTicket(Long id, String userId) {
        log.info("Marking ticket as used: {} for user: {}", id, userId);

        Ticket ticket = ticketRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket not found with ID: " + id + " for user: " + userId));

        if (ticket.getStatus() != TicketStatus.ACTIVE) {
            throw new IllegalStateException("Only active tickets can be used");
        }

        if (LocalDateTime.now().isAfter(ticket.getValidUntil())) {
            ticket.setStatus(TicketStatus.EXPIRED);
            ticketRepository.save(ticket);
            throw new IllegalStateException("Ticket has expired");
        }

        ticket.setStatus(TicketStatus.USED);
        Ticket usedTicket = ticketRepository.save(ticket);

        log.info("Ticket marked as used: {}", id);

        // Publish ticket used event to RabbitMQ
        publishTicketUsedEvent(usedTicket);

        return ticketMapper.toResponse(usedTicket);
    }

    @Transactional
    public void expireOldTickets() {
        log.info("Running job to expire old tickets");
        List<Ticket> expiredTickets = ticketRepository.findExpiredTickets(
                TicketStatus.ACTIVE, LocalDateTime.now());

        expiredTickets.forEach(ticket -> ticket.setStatus(TicketStatus.EXPIRED));
        ticketRepository.saveAll(expiredTickets);

        log.info("Expired {} tickets", expiredTickets.size());
    }

    private String generateQRCode() {
        // Generate a unique QR code identifier
        return UUID.randomUUID().toString();
    }

    private Long deterministicIdFromString(String key) {
        if (key == null) return null;
        // Use a stable hash to convert a descriptive string into a positive Long id
        int h = key.hashCode();
        long id = Integer.toUnsignedLong(h);
        if (id == 0) id = 1L;
        return id;
    }

    // ============= EVENT PUBLISHING METHODS =============

    private void publishTicketCreatedEvent(Ticket ticket) {
        try {
            TicketCreatedEvent event = TicketCreatedEvent.builder()
                    .ticketId(ticket.getId())
                    .userId(ticket.getUserId())
                    .routeId(ticket.getRouteId())
                    .scheduleId(ticket.getScheduleId())
                    .routeName(ticket.getRouteName())
                    .departureStop(ticket.getDepartureStop())
                    .arrivalStop(ticket.getArrivalStop())
                    .departureTime(ticket.getDepartureTime())
                    .arrivalTime(ticket.getArrivalTime())
                    .bus(ticket.getBus())
                    .travelDate(ticket.getTravelDate())
                    .price(ticket.getPrice())
                    .purchaseDate(ticket.getPurchaseDate())
                    .eventTimestamp(LocalDateTime.now())
                    .build();

            eventPublisher.publishTicketCreated(event);
        } catch (Exception e) {
            log.error("Failed to publish ticket created event for ticket: {}", ticket.getId(), e);
            // Don't fail the transaction, just log the error
        }
    }

    private void publishTicketCancelledEvent(Ticket ticket, String reason) {
        try {
            TicketCancelledEvent event = TicketCancelledEvent.builder()
                    .ticketId(ticket.getId())
                    .userId(ticket.getUserId())
                    .routeId(ticket.getRouteId())
                    .scheduleId(ticket.getScheduleId())
                    .routeName(ticket.getRouteName())
                    .departureStop(ticket.getDepartureStop())
                    .arrivalStop(ticket.getArrivalStop())
                    .departureTime(ticket.getDepartureTime())
                    .travelDate(ticket.getTravelDate().toLocalDate())
                    .refundAmount(ticket.getPrice())
                    .cancellationReason(reason)
                    .eventTimestamp(LocalDateTime.now())
                    .build();

            eventPublisher.publishTicketCancelled(event);
        } catch (Exception e) {
            log.error("Failed to publish ticket cancelled event for ticket: {}", ticket.getId(), e);
            // Don't fail the transaction, just log the error
        }
    }

    private void publishTicketUsedEvent(Ticket ticket) {
        try {
            TicketUsedEvent event = TicketUsedEvent.builder()
                    .ticketId(ticket.getId())
                    .userId(ticket.getUserId())
                    .routeId(ticket.getRouteId())
                    .scheduleId(ticket.getScheduleId())
                    .routeName(ticket.getRouteName())
                    .departureStop(ticket.getDepartureStop())
                    .arrivalStop(ticket.getArrivalStop())
                    .travelDate(ticket.getTravelDate().toLocalDate())
                    .usedAt(LocalDateTime.now())
                    .scanLocation("Gate Scan") // Could be enhanced with actual scan location
                    .eventTimestamp(LocalDateTime.now())
                    .build();

            eventPublisher.publishTicketUsed(event);
        } catch (Exception e) {
            log.error("Failed to publish ticket used event for ticket: {}", ticket.getId(), e);
            // Don't fail the transaction, just log the error
        }
    }
}
