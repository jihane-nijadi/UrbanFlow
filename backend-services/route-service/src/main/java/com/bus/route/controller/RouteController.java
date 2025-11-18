package com.bus.route.controller;

import com.bus.route.dto.RouteDTO;
import com.bus.route.dto.RoutePlanRequest;
import com.bus.route.dto.RoutePlanResponse;
import com.bus.route.dto.ScheduleDTO;
import com.bus.route.service.RouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Route and Schedule management.
 * Provides endpoints for the frontend and inter-service communication with Ticket Service.
 */
@RestController
@RequestMapping("/api/routes")
@CrossOrigin(origins = "*")
@Tag(name = "Route Management", description = "APIs for managing routes and schedules")
@Slf4j
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    // ============= ROUTE ENDPOINTS =============

    @GetMapping
    @Operation(summary = "Get all routes", description = "Retrieve all available routes with their schedules")
    public ResponseEntity<List<RouteDTO>> getAllRoutes() {
        log.info("GET /api/routes - Fetching all routes");
        return ResponseEntity.ok(routeService.getAllRoutes());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get route by ID", description = "Used by Ticket Service to fetch route details")
    public ResponseEntity<RouteDTO> getRouteById(@PathVariable Long id) {
        log.info("GET /api/routes/{} - Fetching route by ID", id);
        return ResponseEntity.ok(routeService.getRouteById(id));
    }

    @GetMapping("/route-id/{routeId}")
    @Operation(summary = "Get route by routeId", description = "Fetch route by routeId (e.g., R10)")
    public ResponseEntity<RouteDTO> getRouteByRouteId(@PathVariable String routeId) {
        log.info("GET /api/routes/route-id/{} - Fetching route by routeId", routeId);
        return ResponseEntity.ok(routeService.getRouteByRouteId(routeId));
    }

    @GetMapping("/{id}/available")
    @Operation(summary = "Check route availability", description = "Used by Ticket Service to validate route exists")
    public ResponseEntity<Boolean> isRouteAvailable(@PathVariable Long id) {
        log.info("GET /api/routes/{}/available - Checking route availability", id);
        return ResponseEntity.ok(routeService.isRouteAvailable(id));
    }

    // ============= SCHEDULE ENDPOINTS =============

    @GetMapping("/schedules/{id}")
    @Operation(summary = "Get schedule by ID", description = "Used by Ticket Service to fetch schedule details")
    public ResponseEntity<ScheduleDTO> getScheduleById(@PathVariable Long id) {
        log.info("GET /api/routes/schedules/{} - Fetching schedule by ID", id);
        return ResponseEntity.ok(routeService.getScheduleById(id));
    }

    @GetMapping("/route-id/{routeId}/schedules")
    @Operation(summary = "Get schedules for a route", description = "Get all schedules for a specific routeId")
    public ResponseEntity<List<ScheduleDTO>> getSchedulesByRouteId(@PathVariable String routeId) {
        log.info("GET /api/routes/route-id/{}/schedules - Fetching schedules for route", routeId);
        return ResponseEntity.ok(routeService.getSchedulesByRouteId(routeId));
    }

    @GetMapping("/schedules/{id}/available")
    @Operation(summary = "Check schedule availability", description = "Used by Ticket Service to validate schedule exists")
    public ResponseEntity<Boolean> isScheduleAvailable(@PathVariable Long id) {
        log.info("GET /api/routes/schedules/{}/available - Checking schedule availability", id);
        return ResponseEntity.ok(routeService.isScheduleAvailable(id));
    }

    // ============= STOP ENDPOINTS =============

    @GetMapping("/stops")
    @Operation(summary = "Get all stops", description = "Get all unique bus stops from all schedules")
    public ResponseEntity<List<String>> getAllStops() {
        log.info("GET /api/routes/stops - Fetching all stops");
        return ResponseEntity.ok(routeService.getAllStops());
    }

    @GetMapping("/stops/search")
    @Operation(summary = "Search stops", description = "Search stops by query string for autocomplete")
    public ResponseEntity<List<String>> searchStops(@RequestParam String query) {
        log.info("GET /api/routes/stops/search?query={} - Searching stops", query);
        return ResponseEntity.ok(routeService.searchStops(query));
    }

    // ============= ROUTE PLANNING =============

    @GetMapping("/plan")
    @Operation(summary = "Plan route", description = "Find direct route from one stop to another")
    public ResponseEntity<RoutePlanResponse> planRoute(
            @RequestParam String from, 
            @RequestParam String to) {
        log.info("GET /api/routes/plan?from={}&to={} - Planning route", from, to);
        return ResponseEntity.ok(routeService.planRoute(from, to));
    }

    @PostMapping("/plan")
    @Operation(summary = "Plan route (POST)", description = "Find direct route using request body")
    public ResponseEntity<RoutePlanResponse> planRoutePost(@Valid @RequestBody RoutePlanRequest request) {
        log.info("POST /api/routes/plan - Planning route from {} to {}", request.getFrom(), request.getTo());
        return ResponseEntity.ok(routeService.planRoute(request.getFrom(), request.getTo()));
    }
}
