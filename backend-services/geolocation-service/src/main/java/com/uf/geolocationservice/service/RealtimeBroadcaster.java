package com.uf.geolocationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uf.geolocationservice.model.BusUpdate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class RealtimeBroadcaster {
	private final Set<SseEmitter> emitters = new CopyOnWriteArraySet<>();
	private final ObjectMapper objectMapper = new ObjectMapper();

	public SseEmitter createEmitter() {
		SseEmitter emitter = new SseEmitter(0L); // no timeout
		emitter.onCompletion(() -> emitters.remove(emitter));
		emitter.onTimeout(() -> emitters.remove(emitter));
		emitter.onError(e -> emitters.remove(emitter));
		emitters.add(emitter);
		return emitter;
	}

	public void broadcast(List<BusUpdate> updates) {
		if (updates == null || updates.isEmpty()) return;
		for (SseEmitter emitter : emitters) {
			try {
				String payload = objectMapper.writeValueAsString(updates);
				emitter.send(SseEmitter.event()
					.name("bus_updates")
					.data(payload, MediaType.APPLICATION_JSON));
			} catch (IOException e) {
				emitter.complete();
				emitters.remove(emitter);
			}
		}
	}
}




