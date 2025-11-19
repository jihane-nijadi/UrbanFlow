package com.uf.routeservice.service;

import com.uf.routeservice.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple publisher for route-related events (e.g., route cancelled)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RouteEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publish a route cancelled event with minimal payload.
     */
    public void publishRouteCancelled(Long routeId, String routeName, String reason) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("routeId", routeId);
            payload.put("routeName", routeName);
            payload.put("cancellationReason", reason);
            payload.put("affectedDate", Instant.now().toString());
            payload.put("eventType", "ROUTE_CANCELLED");
            payload.put("eventTimestamp", Instant.now().toString());

            rabbitTemplate.convertAndSend(RabbitMQConfig.ROUTE_EXCHANGE, RabbitMQConfig.ROUTE_CANCELLED_KEY, payload);
            log.info("Published route.cancelled event for routeId={}", routeId);
        } catch (Exception e) {
            log.error("Failed to publish route.cancelled event", e);
        }
    }
}
