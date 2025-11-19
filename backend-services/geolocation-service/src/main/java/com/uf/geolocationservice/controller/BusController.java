package com.uf.geolocationservice.controller;

import com.uf.geolocationservice.model.BusState;
import com.uf.geolocationservice.model.BusStop;
import com.uf.geolocationservice.model.BusUpdate;
import com.uf.geolocationservice.service.BusStopService;
import com.uf.geolocationservice.service.BusStore;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.uf.geolocationservice.service.RealtimeBroadcaster;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class BusController {
	private final BusStore store;
	private final RealtimeBroadcaster realtime;
	private final BusStopService busStopService;

	public BusController(BusStore store, RealtimeBroadcaster realtime, BusStopService busStopService) {
		this.store = store;
		this.realtime = realtime;
		this.busStopService = busStopService;
	}

	@GetMapping("/buses")
	public ResponseEntity<List<BusState>> listActive() {
		return ResponseEntity.ok(store.listActive());
	}

	@GetMapping("/buses/{busId}")
	public ResponseEntity<BusState> get(@PathVariable String busId) {
		Optional<BusState> s = store.get(busId);
		return s.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
	}

	@GetMapping("/buses/{busId}/positions")
	public ResponseEntity<List<BusUpdate>> history(@PathVariable String busId, @RequestParam(defaultValue = "20") int limit) {
		Optional<BusState> s = store.get(busId);
		return s.map(state -> ResponseEntity.ok(state.getRecentPositions(Math.max(1, Math.min(200, limit)))))
			.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@PostMapping("/telemetry")
	public ResponseEntity<Void> ingest(@Valid @RequestBody BusUpdate update) {
		store.upsert(update);
		realtime.broadcast(List.of(update));
		return ResponseEntity.accepted().build();
	}

	@GetMapping(path = "/realtime/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter stream() {
		return realtime.createEmitter();
	}

	// Endpoints pour les arrêts de bus
	@GetMapping("/stops")
	public ResponseEntity<List<BusStop>> getAllStops() {
		return ResponseEntity.ok(busStopService.getAllStops());
	}

	@GetMapping("/stops/{stopId}")
	public ResponseEntity<BusStop> getStop(@PathVariable String stopId) {
		Optional<BusStop> stop = busStopService.getStopById(stopId);
		return stop.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
	}

	@GetMapping("/stops/line/{lineId}")
	public ResponseEntity<List<BusStop>> getStopsByLine(@PathVariable String lineId) {
		return ResponseEntity.ok(busStopService.getStopsByLine(lineId));
	}
}




