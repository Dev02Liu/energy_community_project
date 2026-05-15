package com.energy_community_project.energy_producer.weather;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalTime;

@Component
public class FallbackWeatherClient {

    private final Clock clock;

    public FallbackWeatherClient(Clock clock) {
        this.clock = clock;
    }

    public WeatherSnapshot currentWeather() {
        int hour = LocalTime.now(clock).getHour();
        boolean daylight = hour >= 6 && hour <= 20;
        double cloudCover = daylight ? 35.0 : 85.0;
        double radiation = daylight ? 520.0 : 0.0;

        return new WeatherSnapshot(cloudCover, daylight, radiation);
    }
}
