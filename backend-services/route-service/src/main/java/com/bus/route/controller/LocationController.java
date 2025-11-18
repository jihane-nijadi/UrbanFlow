package com.bus.route.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/location")
@Slf4j
public class LocationController {

    /**
     * Get all active bus locations (mock data for development)
     * Frontend BusTracker calls this endpoint
     */
    @GetMapping("/all")
    public ResponseEntity<List<Map<String, Object>>> getAllBusLocations() {
        log.info("GET /api/location/all - Fetching all bus locations");
        
        List<Map<String, Object>> busLocations = new ArrayList<>();
        
        // Mock bus data - replace with real GPS data later
        busLocations.add(createBusLocation("BUS-001", "Route 1", 36.7783, -119.4179, 45.5));
        busLocations.add(createBusLocation("BUS-002", "Route 1", 36.7850, -119.4100, 52.0));
        busLocations.add(createBusLocation("BUS-003", "Route 2", 36.7900, -119.4250, 38.2));
        busLocations.add(createBusLocation("BUS-004", "Route 2", 36.7950, -119.4300, 41.8));
        busLocations.add(createBusLocation("BUS-005", "Route 3", 36.7700, -119.4050, 55.3));
        
        return ResponseEntity.ok(busLocations);
    }

    /**
     * Get bus locations for a specific route (called by RouteService)
     */
    @GetMapping("/route/{routeId}")
    public ResponseEntity<List<Map<String, Object>>> getBusesByRoute(@PathVariable String routeId) {
        log.info("GET /api/location/route/{} - Fetching buses for route", routeId);
        
        List<Map<String, Object>> busLocations = new ArrayList<>();
        
        // Mock data - filter by routeId
        busLocations.add(createBusLocation("BUS-001", routeId, 36.7783, -119.4179, 45.5));
        busLocations.add(createBusLocation("BUS-002", routeId, 36.7850, -119.4100, 52.0));
        
        return ResponseEntity.ok(busLocations);
    }

    private Map<String, Object> createBusLocation(String busId, String routeId, 
                                                   double latitude, double longitude, 
                                                   double speed) {
        Map<String, Object> location = new HashMap<>();
        location.put("busId", busId);
        location.put("routeId", routeId);
        location.put("latitude", latitude);
        location.put("longitude", longitude);
        location.put("speed", speed);
        location.put("heading", Math.random() * 360); // Random heading
        location.put("timestamp", System.currentTimeMillis());
        location.put("status", "ACTIVE");
        return location;
    }
}
