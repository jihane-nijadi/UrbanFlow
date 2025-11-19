package com.uf.geolocationservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "external.api")
public class ExternalApiProperties {
	private String url;
	private long pollIntervalMs = 3000;
	private String apiKey;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public long getPollIntervalMs() {
		return pollIntervalMs;
	}

	public void setPollIntervalMs(long pollIntervalMs) {
		this.pollIntervalMs = pollIntervalMs;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}
}




