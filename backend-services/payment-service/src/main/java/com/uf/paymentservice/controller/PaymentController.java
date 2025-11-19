package com.uf.paymentservice.controller;

import com.uf.paymentservice.dto.PaymentCompletedEvent;
import com.uf.paymentservice.config.RabbitConfig;
import com.uf.paymentservice.entity.PaymentSession;
import com.uf.paymentservice.repository.PaymentSessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@RestController
@RequestMapping("/api/payments")
@Slf4j
public class PaymentController {

    private final RabbitTemplate rabbitTemplate;
    private final RestTemplate restTemplate;
    private final PaymentSessionRepository sessionRepository;

    public PaymentController(RabbitTemplate rabbitTemplate, RestTemplate restTemplate, PaymentSessionRepository sessionRepository) {
        this.rabbitTemplate = rabbitTemplate;
        this.restTemplate = restTemplate;
        this.sessionRepository = sessionRepository;
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/sessions")
    public ResponseEntity<?> createSession(@RequestBody(required = false) Map<String, Object> jsonPayload,
                                           HttpServletRequest request) {
        try {
            Map<String, Object> payload = new HashMap<>();
            if (jsonPayload != null) payload.putAll(jsonPayload);
            // copy request parameters if present
            Map<String, String[]> params = request.getParameterMap();
            for (Map.Entry<String, String[]> e : params.entrySet()) {
                if (e.getValue() != null && e.getValue().length > 0) {
                    payload.put(e.getKey(), e.getValue()[0]);
                }
            }

            PaymentSession session = PaymentSession.builder()
                    .ticketId(null)
                    .userId((String) payload.getOrDefault("passengerEmail", payload.getOrDefault("userId", "unknown")))
                    .amount(payload.get("price") == null ? null : new BigDecimal(String.valueOf(payload.get("price"))))
                    .status("PENDING")
                    .paymentMethod(null)
                    .returnUrl((String) payload.get("returnUrl"))
                    .metadata(objectMapper.writeValueAsString(payload))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            sessionRepository.save(session);

            Map<String, Object> resp = new HashMap<>();
            // Return only the session id and amount — frontend should render payment UI inside app
            resp.put("sessionId", session.getId());
            resp.put("amount", session.getAmount());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.error("Failed to create payment session", e);
            return ResponseEntity.status(500).body(Map.of("error", "session_creation_failed"));
        }
    }

    @GetMapping("/sessions/{id}")
    public ResponseEntity<?> getSession(@PathVariable Long id) {
        try {
            return sessionRepository.findById(id)
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "session_not_found")));
        } catch (Exception e) {
            log.error("Failed to fetch payment session {}", id, e);
            return ResponseEntity.status(500).body(Map.of("error", "session_lookup_failed"));
        }
    }

    

    @PostMapping("/{ticketId}/pay")
    public ResponseEntity<?> payTicket(@PathVariable String ticketId,
                                       @RequestBody(required = false) Map<String, Object> jsonPayload,
                                       HttpServletRequest request) {
        try {
            // Merge form params and JSON payload (form submissions will come through request.getParameter)
            Map<String, Object> payload = new HashMap<>();
            if (jsonPayload != null) payload.putAll(jsonPayload);

            // copy request parameters (form submissions) if present
            Map<String, String[]> params = request.getParameterMap();
            for (Map.Entry<String, String[]> e : params.entrySet()) {
                if (e.getValue() != null && e.getValue().length > 0) {
                    payload.put(e.getKey(), e.getValue()[0]);
                }
            }

            log.info("Simulated payment for ticket {} payload={}", ticketId, payload);
            BigDecimal amount = payload.get("amount") == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(payload.get("amount")));
            String tx = UUID.randomUUID().toString();

            PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                    .paymentId(System.currentTimeMillis())
                    .ticketId(Long.valueOf(ticketId))
                    .userId((String) payload.getOrDefault("userId", payload.getOrDefault("passengerEmail", "unknown")))
                    .amount(amount)
                    .paymentMethod((String) payload.getOrDefault("paymentMethod", "card_simulated"))
                    .transactionId(tx)
                    .status("SUCCESS")
                    .eventTimestamp(LocalDateTime.now())
                    .build();

            // Persist the completed payment session (or update existing pending one)
            try {
                // Try to find session by ticketId first (old flow)
                Long tId = null;
                try { tId = Long.valueOf(ticketId); } catch (Exception ignore) {}

                // If there is a PaymentSession matching this ticketId (legacy flow), update it
                if (tId != null) {
                    PaymentSession session = sessionRepository.findByTicketId(tId).orElse(null);
                    if (session != null) {
                        session.setStatus("COMPLETED");
                        session.setPaymentMethod(event.getPaymentMethod());
                        session.setUpdatedAt(LocalDateTime.now());
                        sessionRepository.save(session);
                    }
                }
                // Also support the newer flow where the path variable is a session id (created earlier by frontend)
                try {
                    Long sid = Long.valueOf(ticketId);
                    sessionRepository.findById(sid).ifPresent(s -> {
                        if (s.getTicketId() == null) {
                            // session contains an intent (metadata) that we must use to create the actual ticket
                            try {
                                String meta = s.getMetadata();
                                if (meta != null && !meta.isBlank()) {
                                    Map<String, Object> ticketPayload = objectMapper.readValue(meta, new TypeReference<Map<String,Object>>(){});
                                    // POST to ticket-service to create the real ticket
                                    try {
                                        Map created = restTemplate.postForObject("http://ticket-service:8082/api/tickets", ticketPayload, Map.class);
                                        if (created != null && created.get("id") != null) {
                                            Long createdId = Long.valueOf(String.valueOf(created.get("id")));
                                            s.setTicketId(createdId);
                                            s.setStatus("COMPLETED");
                                            s.setUpdatedAt(LocalDateTime.now());
                                            s.setPaymentMethod(event.getPaymentMethod());
                                            sessionRepository.save(s);
                                        }
                                    } catch (Exception e) {
                                        log.warn("Failed to create ticket from payment session {}: {}", s.getId(), e.getMessage());
                                    }
                                }
                            } catch (Exception e) {
                                log.warn("Failed to parse session metadata for session {}: {}", s.getId(), e.getMessage());
                            }
                        }
                    });
                } catch (Exception exx) {
                    // ignore parsing session id
                }
            } catch (Exception ex) {
                log.warn("Could not persist payment session: {}", ex.getMessage());
            }

            rabbitTemplate.convertAndSend(RabbitConfig.PAYMENT_EXCHANGE, RabbitConfig.PAYMENT_COMPLETED_ROUTING, event);

            // Try to fetch ticket details from ticket-service so we can generate a PDF
            try {
                String ticketUrl = "http://ticket-service:8082/api/tickets/" + ticketId;
                Map ticket = restTemplate.getForObject(ticketUrl, Map.class);
                log.info("Fetched ticket details from ticket-service: {}", ticket);

                // Simulate PDF generation (in real app use iText / Apache PDFBox). We'll create a small dummy PDF payload.
                String pdfContent = "PDF for ticket " + ticketId + " - user:" + event.getUserId() + " - amount:" + event.getAmount();
                byte[] pdfBytes = pdfContent.getBytes();
                String pdfBase64 = Base64.getEncoder().encodeToString(pdfBytes);
                log.info("Generated simulated PDF for ticket {} ({} bytes)", ticketId, pdfBytes.length);

                // If a returnUrl was provided, redirect user back there (frontend flow)
                String returnUrl = (String) payload.get("returnUrl");
                if (returnUrl != null && !returnUrl.isBlank()) {
                    log.info("Payment processed, will redirect user to {} (payment event published)", returnUrl);
                    return ResponseEntity.status(HttpStatus.FOUND).header("Location", returnUrl).build();
                }

                Map<String, Object> resp = new HashMap<>();
                resp.put("transactionId", tx);
                resp.put("status", "SUCCESS");
                resp.put("ticketId", ticketId);
                resp.put("pdfBase64", pdfBase64);
                return ResponseEntity.ok(resp);
            } catch (Exception e) {
                log.warn("Unable to fetch ticket or generate PDF: {}", e.getMessage());
                Map<String, Object> resp = new HashMap<>();
                resp.put("transactionId", tx);
                resp.put("status", "SUCCESS");
                resp.put("ticketId", ticketId);
                return ResponseEntity.ok(resp);
            }
        } catch (Exception e) {
            log.error("Payment simulation failed", e);
            return ResponseEntity.status(500).body(Map.of("error", "payment_failed"));
        }
    }
}
