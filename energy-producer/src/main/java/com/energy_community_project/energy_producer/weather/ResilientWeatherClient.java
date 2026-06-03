package com.energy_community_project.energy_producer.weather;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Weather source with graceful degradation: uses the live Open-Meteo API by default, but falls back
 * to a local simulation when forced via {@code app.weather.mode=fallback} or when the API call fails,
 * so the producer keeps running offline.
 */
@Component
public class ResilientWeatherClient implements WeatherClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResilientWeatherClient.class);

    private final OpenMeteoWeatherClient openMeteoWeatherClient;
    private final FallbackWeatherClient fallbackWeatherClient;
    private final String mode;

    public ResilientWeatherClient(OpenMeteoWeatherClient openMeteoWeatherClient,
                                  FallbackWeatherClient fallbackWeatherClient,
                                  @Value("${app.weather.mode:open-meteo}") String mode) {
        this.openMeteoWeatherClient = openMeteoWeatherClient;
        this.fallbackWeatherClient = fallbackWeatherClient;
        this.mode = mode;
    }

    @Override
    public WeatherSnapshot currentWeather() {
        // Explicit offline mode: skip the network entirely.
        if ("fallback".equalsIgnoreCase(mode)) {
            return fallbackWeatherClient.currentWeather();
        }

        // Default: try the live API, but degrade to the local simulation on any failure.
        try {
            return openMeteoWeatherClient.currentWeather();
        } catch (WeatherClientException ex) {
            LOGGER.warn("Falling back to local weather simulation: {}", ex.getMessage());
            return fallbackWeatherClient.currentWeather();
        }
    }
}
