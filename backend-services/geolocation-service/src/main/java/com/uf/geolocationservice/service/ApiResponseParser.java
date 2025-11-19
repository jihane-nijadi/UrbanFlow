package com.uf.geolocationservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uf.geolocationservice.model.BusUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ApiResponseParser {
	private static final Logger log = LoggerFactory.getLogger(ApiResponseParser.class);
	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Parse external API response and convert to BusUpdate list.
	 * Supports multiple formats:
	 * 1. Simple JSON array: [{"busId": "...", "lat": ..., "lon": ..., ...}]
	 * 2. Transitland format: {"vehicle_positions": [...]}
	 * 3. GTFS-RT-like format: {"entity": [{"vehicle": {...}}]}
	 */
	public List<BusUpdate> parse(String json) {
		if (json == null || json.isBlank()) {
			return List.of();
		}

		try {
			JsonNode root = objectMapper.readTree(json);

			// Format 1: Direct array
			if (root.isArray()) {
				return parseArray(root);
			}

			// Format 2: Transitland format - {"vehicle_positions": [...]}
			if (root.has("vehicle_positions") && root.get("vehicle_positions").isArray()) {
				return parseArray(root.get("vehicle_positions"));
			}

			// Format 3: GTFS-RT format - {"entity": [{"vehicle": {...}}]}
			if (root.has("entity") && root.get("entity").isArray()) {
				return parseGtfsRt(root.get("entity"));
			}

			// Format 4: Simple object with vehicles array
			if (root.has("vehicles") && root.get("vehicles").isArray()) {
				return parseArray(root.get("vehicles"));
			}

			// Format 5: Single object (wrap in list)
			if (root.isObject() && root.has("busId")) {
				BusUpdate update = parseSingle(root);
				return update != null ? List.of(update) : List.of();
			}

			log.warn("Unknown API response format. Root keys: {}", root.fieldNames());
			return List.of();

		} catch (Exception e) {
			log.error("Failed to parse API response", e);
			return List.of();
		}
	}

	private List<BusUpdate> parseArray(JsonNode arrayNode) {
		List<BusUpdate> updates = new ArrayList<>();
		for (JsonNode node : arrayNode) {
			BusUpdate update = parseSingle(node);
			if (update != null) {
				updates.add(update);
			}
		}
		return updates;
	}

	private List<BusUpdate> parseGtfsRt(JsonNode entityArray) {
		List<BusUpdate> updates = new ArrayList<>();
		for (JsonNode entity : entityArray) {
			if (entity.has("vehicle") && entity.get("vehicle").has("vehicle")) {
				JsonNode vehicle = entity.get("vehicle").get("vehicle");
				JsonNode position = entity.get("vehicle").has("position") 
					? entity.get("vehicle").get("position") 
					: null;

				BusUpdate update = new BusUpdate();
				
				// Vehicle ID
				if (vehicle.has("id")) {
					update.setBusId(vehicle.get("id").asText());
				} else if (entity.has("id")) {
					update.setBusId(entity.get("id").asText());
				}

				// Trip/Route ID as lineId
				if (entity.get("vehicle").has("trip") && entity.get("vehicle").get("trip").has("routeId")) {
					update.setLineId(entity.get("vehicle").get("trip").get("routeId").asText());
				}

				// Position
				if (position != null) {
					if (position.has("latitude")) {
						update.setLat(position.get("latitude").asDouble());
					}
					if (position.has("longitude")) {
						update.setLon(position.get("longitude").asDouble());
					}
					if (position.has("bearing")) {
						update.setHeading(position.get("bearing").asInt());
					}
					if (position.has("speed")) {
						update.setSpeed(position.get("speed").asDouble());
					}
				}

				// Timestamp
				if (entity.get("vehicle").has("timestamp")) {
					update.setTs(entity.get("vehicle").get("timestamp").asLong() * 1000);
				} else {
					update.setTs(System.currentTimeMillis());
				}

				if (update.getBusId() != null && update.getLat() != null && update.getLon() != null) {
					updates.add(update);
				}
			}
		}
		return updates;
	}

	private BusUpdate parseSingle(JsonNode node) {
		try {
			BusUpdate update = new BusUpdate();

			// busId - try multiple field names
			if (node.has("busId")) {
				update.setBusId(node.get("busId").asText());
			} else if (node.has("id")) {
				update.setBusId(node.get("id").asText());
			} else if (node.has("vehicle_id")) {
				update.setBusId(node.get("vehicle_id").asText());
			} else if (node.has("vehicleId")) {
				update.setBusId(node.get("vehicleId").asText());
			}

			// lineId - try multiple field names
			if (node.has("lineId")) {
				update.setLineId(node.get("lineId").asText());
			} else if (node.has("route_id")) {
				update.setLineId(node.get("route_id").asText());
			} else if (node.has("routeId")) {
				update.setLineId(node.get("routeId").asText());
			} else if (node.has("line")) {
				update.setLineId(node.get("line").asText());
			}

			// lat - try multiple field names
			if (node.has("lat")) {
				update.setLat(node.get("lat").asDouble());
			} else if (node.has("latitude")) {
				update.setLat(node.get("latitude").asDouble());
			} else if (node.has("position") && node.get("position").has("lat")) {
				update.setLat(node.get("position").get("lat").asDouble());
			}

			// lon - try multiple field names
			if (node.has("lon")) {
				update.setLon(node.get("lon").asDouble());
			} else if (node.has("lng")) {
				update.setLon(node.get("lng").asDouble());
			} else if (node.has("longitude")) {
				update.setLon(node.get("longitude").asDouble());
			} else if (node.has("position") && node.get("position").has("lon")) {
				update.setLon(node.get("position").get("lon").asDouble());
			} else if (node.has("position") && node.get("position").has("lng")) {
				update.setLon(node.get("position").get("lng").asDouble());
			}

			// speed - try multiple field names
			if (node.has("speed")) {
				update.setSpeed(node.get("speed").asDouble());
			} else if (node.has("velocity")) {
				update.setSpeed(node.get("velocity").asDouble());
			}

			// heading - try multiple field names
			if (node.has("heading")) {
				update.setHeading(node.get("heading").asInt());
			} else if (node.has("bearing")) {
				update.setHeading(node.get("bearing").asInt());
			} else if (node.has("direction")) {
				update.setHeading(node.get("direction").asInt());
			}

			// timestamp - try multiple field names
			if (node.has("ts")) {
				update.setTs(node.get("ts").asLong());
			} else if (node.has("timestamp")) {
				long ts = node.get("timestamp").asLong();
				// If timestamp is in seconds, convert to milliseconds
				if (ts < 10000000000L) {
					ts = ts * 1000;
				}
				update.setTs(ts);
			} else if (node.has("time")) {
				long ts = node.get("time").asLong();
				if (ts < 10000000000L) {
					ts = ts * 1000;
				}
				update.setTs(ts);
			} else {
				update.setTs(System.currentTimeMillis());
			}

			// Validate required fields
			if (update.getBusId() == null || update.getBusId().isBlank()) {
				log.warn("BusUpdate missing busId: {}", node);
				return null;
			}
			if (update.getLat() == null || update.getLon() == null) {
				log.warn("BusUpdate missing coordinates for busId: {}", update.getBusId());
				return null;
			}

			return update;

		} catch (Exception e) {
			log.error("Failed to parse single BusUpdate from node: {}", node, e);
			return null;
		}
	}
}


