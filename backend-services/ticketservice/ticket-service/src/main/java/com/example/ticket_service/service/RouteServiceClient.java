package com.example.ticket_service.service;

import com.example.ticket_service.dto.RouteDTO;
import com.example.ticket_service.dto.ScheduleDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service to communicate with Route Service
 * Fetches route and schedule information
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RouteServiceClient {

    private final RestTemplate restTemplate;

    @Value("${route.service.url:http://localhost:8081}")
    private String routeServiceUrl;

    /**
     * Get route information by route ID
     * @param routeId Route ID
     * @return Route information
     */
    public RouteDTO getRouteById(Long routeId) {
        try {
            log.info("Fetching route information for routeId: {}", routeId);
            String url = routeServiceUrl + "/api/routes/" + routeId;
            RouteDTO route = restTemplate.getForObject(url, RouteDTO.class);
            log.info("Successfully fetched route: {}", route.getRouteName());
            return route;
        } catch (Exception e) {
            log.error("Failed to fetch route with ID: {}", routeId, e);
            throw new RuntimeException("Route service unavailable or route not found: " + routeId, e);
        }
    }

    /**
     * Get schedule information by schedule ID
     * @param scheduleId Schedule ID
     * @return Schedule information
     */
    public ScheduleDTO getScheduleById(Long scheduleId) {
        try {
            log.info("Fetching schedule information for scheduleId: {}", scheduleId);
            String url = routeServiceUrl + "/api/routes/schedules/" + scheduleId;
            ScheduleDTO schedule = restTemplate.getForObject(url, ScheduleDTO.class);
            log.info("Successfully fetched schedule: {} -> {}", 
                    schedule.getDepartureStop(), schedule.getArrivalStop());
            return schedule;
        } catch (Exception e) {
            log.error("Failed to fetch schedule with ID: {}", scheduleId, e);
            throw new RuntimeException("Route service unavailable or schedule not found: " + scheduleId, e);
        }
    }

    /**
     * Check if route exists and is active
     * @param routeId Route ID
     * @return true if route exists
     */
    public boolean isRouteAvailable(Long routeId) {
        try {
            getRouteById(routeId);
            return true;
        } catch (Exception e) {
            log.warn("Route {} is not available", routeId);
            return false;
        }
    }

    /**
     * Check if schedule exists and is active
     * @param scheduleId Schedule ID
     * @return true if schedule exists
     */
    public boolean isScheduleAvailable(Long scheduleId) {
        try {
            getScheduleById(scheduleId);
            return true;
        } catch (Exception e) {
            log.warn("Schedule {} is not available", scheduleId);
            return false;
        }
    }
}
