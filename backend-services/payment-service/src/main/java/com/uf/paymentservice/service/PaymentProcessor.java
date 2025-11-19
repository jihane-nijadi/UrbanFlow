package com.uf.paymentservice.service;

import com.uf.paymentservice.dto.TicketCreatedEvent;
import com.uf.paymentservice.entity.PaymentSession;
import com.uf.paymentservice.repository.PaymentSessionRepository;
import com.uf.paymentservice.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentProcessor {

    private final RabbitTemplate rabbitTemplate;
    private final PaymentSessionRepository sessionRepository;

    @RabbitListener(queues = RabbitConfig.PAYMENT_QUEUE)
    public void onTicketCreated(TicketCreatedEvent event) {
        try {
            log.info("Received ticket created event, ticketId={} userId={}", event.getTicketId(), event.getUserId());

            // Persist a pending payment session for this ticket
            PaymentSession session = PaymentSession.builder()
                    .ticketId(event.getTicketId())
                    .userId(event.getUserId())
                    .amount(event.getPrice())
                    .status("PENDING")
                    .paymentMethod(null)
                    .returnUrl(null)
                    .metadata(null)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            // If a session already exists for the ticket, keep the existing one
            Optional<PaymentSession> existing = sessionRepository.findByTicketId(event.getTicketId());
            if (existing.isPresent()) {
                log.info("Payment session already exists for ticketId={}, skipping create", event.getTicketId());
            } else {
                sessionRepository.save(session);
                log.info("Stored pending payment session for ticketId={} amount={}", event.getTicketId(), event.getPrice());
            }

        } catch (Exception e) {
            log.error("Error processing ticket created event", e);
        }
    }

    // Expose pending session for simple inspection (used by controller/tests)
    public PaymentSession getPending(Long ticketId) {
        return sessionRepository.findByTicketId(ticketId).orElse(null);
    }
}
