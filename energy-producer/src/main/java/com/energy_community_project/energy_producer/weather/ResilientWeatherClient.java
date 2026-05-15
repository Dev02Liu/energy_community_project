package com.energy_community_project.energy_producer.weather;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
        if ("fallback".equalsIgnoreCase(mode)) {
            return fallbackWeatherClient.currentWeather();
        }

        try {
            return openMeteoWeatherClient.currentWeather();
        } catch (WeatherClientException ex) {
            LOGGER.warn("Falling back to local weather simulation: {}", ex.getMessage());
            return fallbackWeatherClient.currentWeather();
        }
    }
}
