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

    @Transactional
    public TicketResponse createTicket(TicketRequest request) {
        log.info("Creating ticket for user: {} on route: {}, schedule: {}", 
                request.getUserId(), request.getRouteId(), request.getScheduleId());

        // Fetch route and schedule information from Route Service
        RouteDTO route = routeServiceClient.getRouteById(request.getRouteId());
        ScheduleDTO schedule = routeServiceClient.getScheduleById(request.getScheduleId());

        // Validate that schedule belongs to the route
        if (!schedule.getRouteId().equals(route.getRouteId())) {
            throw new IllegalArgumentException(
                    "Schedule " + request.getScheduleId() + " does not belong to route " + request.getRouteId());
        }

        // Check seat availability (limit: 70 seats per schedule per date)
        long bookedSeats = ticketRepository.countByScheduleIdAndTravelDate(
                request.getScheduleId(), request.getTravelDate());
        if (bookedSeats >= 70) {
            throw new SeatAlreadyBookedException(
                    "All seats are booked for this schedule on " + request.getTravelDate() + " (70/70 seats taken)");
        }

        // Create ticket with route and schedule information
        Ticket ticket = Ticket.builder()
                .userId(request.getUserId())
                .routeId(request.getRouteId())
                .scheduleId(request.getScheduleId())
                .routeName(route.getRouteName())
                .departureStop(schedule.getDepartureStop())
                .arrivalStop(schedule.getArrivalStop())
                .departureTime(schedule.getDepartureTime())
                .arrivalTime(schedule.getArrivalTime())
                .travelDate(request.getTravelDate())
                .price(request.getPrice())
                .status(TicketStatus.PENDING)
                .qrCode(generateQRCode())
                .build();

        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Ticket created successfully with ID: {} for route: {} ({})", 
                savedTicket.getId(), route.getRouteName(), schedule.getDepartureStop() + " -> " + schedule.getArrivalStop());

        // Publish ticket created event to RabbitMQ
        publishTicketCreatedEvent(savedTicket);

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
