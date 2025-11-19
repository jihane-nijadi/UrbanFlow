package com.example.ticket_service.controller;

import com.example.ticket_service.dto.TicketRequest;
import com.example.ticket_service.dto.TicketResponse;
import com.example.ticket_service.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Slf4j
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    public ResponseEntity<?> createTicket(@RequestBody String rawPayload) {
        log.info("POST /api/tickets - raw body: {}", rawPayload);
        CreateTicketPayload payload;
        try {
            // strip BOM if present (some clients write a UTF-8 BOM which breaks Jackson)
            if (rawPayload != null && rawPayload.startsWith("\uFEFF")) {
                log.info("Detected UTF-8 BOM in request body, stripping it");
                rawPayload = rawPayload.substring(1);
            }
            String cleaned = rawPayload != null ? rawPayload.trim() : rawPayload;
            if (cleaned != null) {
                // remove UTF-8 BOM if present
                cleaned = cleaned.replace("\uFEFF", "");
                // remove any leading garbage before JSON object/array
                int idxObj = cleaned.indexOf('{');
                int idxArr = cleaned.indexOf('[');
                int start = -1;
                if (idxObj >= 0 && idxArr >= 0) start = Math.min(idxObj, idxArr);
                else if (idxObj >= 0) start = idxObj;
                else if (idxArr >= 0) start = idxArr;
                if (start > 0) {
                    log.info("Trimming leading non-JSON characters, starting at {}", start);
                    cleaned = cleaned.substring(start);
                }
            }
            log.info("Cleaned request body: {}", cleaned);

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            payload = mapper.readValue(cleaned, CreateTicketPayload.class);
        } catch (Exception e) {
            log.error("Failed to parse request body into CreateTicketPayload", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
        log.info("Parsed payload: {}", payload);

        // Map incoming flexible payload (from frontend) into internal TicketRequest
        // Frontend sends routeId/scheduleId as simple values and departureTime as ISO string.
        try {
            Long routeId = null;
            Long scheduleId = null;
            if (payload.getRouteId() != null) {
                try { routeId = Long.valueOf(payload.getRouteId()); } catch (NumberFormatException ignored) { }
            }
            if (payload.getScheduleId() != null) {
                try { scheduleId = Long.valueOf(payload.getScheduleId()); } catch (NumberFormatException ignored) { }
            }

            // If both numeric routeId and scheduleId are missing, allow creation only when
            // descriptive stops (departure/arrival) are provided by the frontend.
            if (routeId == null && scheduleId == null) {
                boolean hasStops = (payload.getDepartureStop() != null && payload.getArrivalStop() != null)
                        || (payload.getDepartureStopId() != null && payload.getArrivalStopId() != null);
                if (!hasStops) {
                    log.warn("Missing routeId and scheduleId and no descriptive stops provided in payload");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
                }
                log.info("No numeric route/schedule id provided; creating ad-hoc ticket using provided stops");
            }

            // default quantity
            int qty = payload.getQuantity() != null && payload.getQuantity() > 0 ? payload.getQuantity() : 1;

            java.time.LocalDateTime travelDate = null;
            if (payload.getDepartureTime() != null && !payload.getDepartureTime().isBlank()) {
                try { travelDate = java.time.LocalDateTime.parse(payload.getDepartureTime()); } catch (Exception e) {
                    // try parsing as date-only + time if needed
                    try { travelDate = java.time.LocalDateTime.parse(payload.getDepartureTime().replace(' ', 'T')); } catch (Exception ex) { travelDate = null; }
                }
            }
            if (travelDate == null) travelDate = java.time.LocalDateTime.now();

            java.math.BigDecimal price;
            if (payload.getPrice() != null) {
                try {
                    price = java.math.BigDecimal.valueOf(payload.getPrice());
                } catch (Exception e) {
                    price = java.math.BigDecimal.valueOf(7L * qty);
                }
            } else {
                price = java.math.BigDecimal.valueOf(7L * qty);
            }

            // build internal TicketRequest
                // build internal TicketRequest (include descriptive stops if available)
                    TicketRequest.TicketRequestBuilder reqBuilder = TicketRequest.builder()
                        .userId(payload.getPassengerEmail() != null ? payload.getPassengerEmail() : "anonymous")
                        .routeId(routeId)
                        .scheduleId(scheduleId)
                        .travelDate(travelDate)
                        .price(price)
                        .departureStop(payload.getDepartureStop())
                        .arrivalStop(payload.getArrivalStop());

                    // If the frontend sent a non-numeric routeId (e.g. a code like "BUS-001|09:30|mock"), keep it
                    // as routeCode so service can use it as a descriptive route name instead of failing.
                    if (routeId == null && payload.getRouteId() != null) {
                    reqBuilder.routeCode(payload.getRouteId());
                    }

                    TicketRequest req = reqBuilder.build();

            TicketResponse response = ticketService.createTicket(req);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Failed to create ticket from payload", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicketById(@PathVariable Long id) {
        log.info("GET /api/tickets/{} - Fetching ticket", id);
        TicketResponse response = ticketService.getTicketById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TicketResponse>> getUserTickets(@PathVariable String userId) {
        log.info("GET /api/tickets/user/{} - Fetching user tickets", userId);
        List<TicketResponse> tickets = ticketService.getTicketsByUserId(userId);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/user/{userId}/upcoming")
    public ResponseEntity<List<TicketResponse>> getUpcomingTickets(@PathVariable String userId) {
        log.info("GET /api/tickets/user/{}/upcoming - Fetching upcoming tickets", userId);
        List<TicketResponse> tickets = ticketService.getUpcomingTickets(userId);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<TicketResponse>> getActiveTickets(@PathVariable String userId) {
        log.info("GET /api/tickets/user/{}/active - Fetching active tickets", userId);
        List<TicketResponse> tickets = ticketService.getActiveTickets(userId);
        return ResponseEntity.ok(tickets);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<TicketResponse> cancelTicket(
            @PathVariable Long id,
            @RequestParam String userId) {
        log.info("PUT /api/tickets/{}/cancel - Cancelling ticket for user: {}", id, userId);
        TicketResponse response = ticketService.cancelTicket(id, userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/use")
    public ResponseEntity<TicketResponse> useTicket(
            @PathVariable Long id,
            @RequestParam String userId) {
        log.info("PUT /api/tickets/{}/use - Using ticket for user: {}", id, userId);
        TicketResponse response = ticketService.useTicket(id, userId);
        return ResponseEntity.ok(response);
    }
}
