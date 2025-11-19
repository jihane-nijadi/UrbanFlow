package com.uf.geolocationservice.service;

import com.uf.geolocationservice.model.BusState;
import com.uf.geolocationservice.model.BusUpdate;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(prefix = "geolocation.persistence", name = "enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryBusStore implements BusStore {
	private final Map<String, BusState> busIdToState = new ConcurrentHashMap<>();
	private final int maxHistory = 50;
	private final Duration staleThreshold = Duration.ofSeconds(60);

	public void upsert(BusUpdate update) {
		BusState state = busIdToState.computeIfAbsent(update.getBusId(), id -> new BusState(id, update.getLineId(), maxHistory));
		state.apply(update);
	}

	public Optional<BusState> get(String busId) {
		return Optional.ofNullable(busIdToState.get(busId));
	}

	public List<BusState> listActive() {
		Collection<BusState> values = busIdToState.values();
		List<BusState> active = new ArrayList<>(values.size());
		Instant now = Instant.now();
		for (BusState s : values) {
			if (s.getLastUpdatedAt() == null) {
				continue;
			}
			if (Duration.between(s.getLastUpdatedAt(), now).compareTo(staleThreshold) <= 0) {
				active.add(s);
			}
		}
		return active;
	}
}




