package com.uf.geolocationservice.model;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class BusState {
	private final String busId;
	private final String lineId;
	private volatile double lat;
	private volatile double lon;
	private volatile Double speed;
	private volatile Integer heading;
	private volatile Instant lastUpdatedAt;

	private final Deque<BusUpdate> recentPositions;
	private final int maxHistory;

	public BusState(String busId, String lineId, int maxHistory) {
		this.busId = busId;
		this.lineId = lineId;
		this.maxHistory = Math.max(1, maxHistory);
		this.recentPositions = new ArrayDeque<>(this.maxHistory);
	}

	public synchronized void apply(BusUpdate update) {
		this.lat = update.getLat();
		this.lon = update.getLon();
		this.speed = update.getSpeed();
		this.heading = update.getHeading();
		this.lastUpdatedAt = Instant.ofEpochMilli(update.getTs());
		if (recentPositions.size() >= maxHistory) {
			recentPositions.removeFirst();
		}
		recentPositions.addLast(update);
	}

	public String getBusId() {
		return busId;
	}

	public String getLineId() {
		return lineId;
	}

	public double getLat() {
		return lat;
	}

	public double getLon() {
		return lon;
	}

	public Double getSpeed() {
		return speed;
	}

	public Integer getHeading() {
		return heading;
	}

	public Instant getLastUpdatedAt() {
		return lastUpdatedAt;
	}

	public synchronized List<BusUpdate> getRecentPositions(int limit) {
		int n = Math.min(limit, recentPositions.size());
		List<BusUpdate> list = new ArrayList<>(n);
		int start = recentPositions.size() - n;
		int i = 0;
		for (BusUpdate u : recentPositions) {
			if (i++ >= start) {
				list.add(u);
			}
		}
		return list;
	}
}




