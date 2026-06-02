package com.energy_community_project.energy_producer.weather;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

@Component
public class WeatherProductionCalculator {

    private final double minKwh;
    private final double maxKwh;
    private final DoubleSupplier variationSupplier;

    @Autowired
    public WeatherProductionCalculator(@Value("${app.production.min-kwh:0.001}") double minKwh,
                                       @Value("${app.production.max-kwh:0.006}") double maxKwh) {
        this(minKwh, maxKwh, () -> ThreadLocalRandom.current().nextDouble(-0.10, 0.10));
    }

    WeatherProductionCalculator(double minKwh, double maxKwh, DoubleSupplier variationSupplier) {
        if (minKwh < 0 || maxKwh <= minKwh) {
            throw new IllegalArgumentException("Production kWh range must be non-negative and increasing.");
        }
        this.minKwh = minKwh;
        this.maxKwh = maxKwh;
        this.variationSupplier = variationSupplier;
    }

    public double calculateKwh(WeatherSnapshot weather) {
        double daylightFactor = weather.daylight() ? 1.0 : 0.12;
        double cloudFactor = 1.0 - (weather.cloudCoverPercent() / 100.0 * 0.70);
        double radiationFactor = Math.min(weather.shortwaveRadiationWm2() / 800.0, 1.0);
        double weatherFactor = Math.max(radiationFactor, daylightFactor * cloudFactor);
        double clampedFactor = Math.max(0.05, Math.min(1.0, weatherFactor));

        double productionRange = maxKwh - minKwh;
        double weatherBasedKwh = minKwh + (productionRange * clampedFactor);
        double variedKwh = weatherBasedKwh + (productionRange * variationSupplier.getAsDouble());

        return Math.max(minKwh, Math.min(maxKwh, variedKwh));
    }
}
