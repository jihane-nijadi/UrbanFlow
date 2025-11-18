package com.uf.geolocationservice.service;

import com.uf.geolocationservice.config.ExternalApiProperties;
import com.uf.geolocationservice.model.BusUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class ExternalApiPoller {
	private static final Logger log = LoggerFactory.getLogger(ExternalApiPoller.class);

	private final ExternalApiProperties properties;
	private final RestTemplate restTemplate;
	private final InMemoryBusStore store;
	private final RealtimeBroadcaster broadcaster;
	private final ApiResponseParser parser;
	private final MockDataGenerator mockGenerator;

	public ExternalApiPoller(
		ExternalApiProperties properties,
		RestTemplate restTemplate,
		InMemoryBusStore store,
		RealtimeBroadcaster broadcaster,
		ApiResponseParser parser,
		MockDataGenerator mockGenerator
	) {
		this.properties = properties;
		this.restTemplate = restTemplate;
		this.store = store;
		this.broadcaster = broadcaster;
		this.parser = parser;
		this.mockGenerator = mockGenerator;
	}

	@Scheduled(fixedDelayString = "#{@externalApiProperties.getPollIntervalMs()}")
	public void poll() {
		List<BusUpdate> updates;

		// Try external API first
		if (properties.getUrl() != null && !properties.getUrl().isBlank()) {
			updates = fetchFromExternalApi();
		} else {
			updates = List.of();
		}

		// If no updates from API and mock is enabled, use mock data
		if (updates.isEmpty() && mockGenerator.isEnabled()) {
			log.debug("No external API data, using mock data");
			updates = mockGenerator.generateMockUpdates();
		}

		// Store and broadcast updates
		if (!updates.isEmpty()) {
			for (BusUpdate u : updates) {
				store.upsert(u);
			}
			broadcaster.broadcast(updates);
			log.debug("Processed {} bus updates", updates.size());
		}
	}

	private List<BusUpdate> fetchFromExternalApi() {
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.add("User-Agent", "UrbanFlow-GeolocationService/1.0");
			headers.add("Accept", "application/json");

			if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
				// Support different auth methods
				if (properties.getApiKey().startsWith("Bearer ")) {
					headers.add("Authorization", properties.getApiKey());
				} else {
					headers.add("Authorization", "Bearer " + properties.getApiKey());
				}
			}

			HttpEntity<Void> entity = new HttpEntity<>(headers);
			ResponseEntity<String> resp = restTemplate.exchange(
				properties.getUrl(),
				HttpMethod.GET,
				entity,
				String.class
			);

			if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
				log.warn("External API returned non-2xx or empty body: {}", resp.getStatusCode());
				return List.of();
			}

			List<BusUpdate> updates = parser.parse(resp.getBody());
			log.info("Fetched {} bus updates from external API", updates.size());
			return updates;

		} catch (org.springframework.web.client.ResourceAccessException e) {
			log.warn("Cannot reach external API (network error): {}", e.getMessage());
			return List.of();
		} catch (org.springframework.web.client.HttpClientErrorException e) {
			log.warn("External API returned error {}: {}", e.getStatusCode(), e.getMessage());
			return List.of();
		} catch (Exception e) {
			log.error("Polling external API failed", e);
			return List.of();
		}
	}
}




