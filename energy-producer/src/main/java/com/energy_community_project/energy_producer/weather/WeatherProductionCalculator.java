package com.energy_community_project.energy_producer.weather;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WeatherProductionCalculator {

    private final double minKwh;
    private final double maxKwh;

    public WeatherProductionCalculator(@Value("${app.production.min-kwh:10.0}") double minKwh,
                                       @Value("${app.production.max-kwh:30.0}") double maxKwh) {
        if (minKwh < 0 || maxKwh <= minKwh) {
            throw new IllegalArgumentException("Production kWh range must be non-negative and increasing.");
        }
        this.minKwh = minKwh;
        this.maxKwh = maxKwh;
    }

    public double calculateKwh(WeatherSnapshot weather) {
        double daylightFactor = weather.daylight() ? 1.0 : 0.12;
        double cloudFactor = 1.0 - (weather.cloudCoverPercent() / 100.0 * 0.70);
        double radiationFactor = Math.min(weather.shortwaveRadiationWm2() / 800.0, 1.0);
        double weatherFactor = Math.max(radiationFactor, daylightFactor * cloudFactor);
        double clampedFactor = Math.max(0.05, Math.min(1.0, weatherFactor));

        return minKwh + ((maxKwh - minKwh) * clampedFactor);
    }
}
