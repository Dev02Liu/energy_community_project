package com.energy_community_project.energy_producer.weather;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Turns the current solar radiation into a produced kWh value.
 * More sun (higher radiation) means more energy, plus a small random variation.
 */
@Component
public class WeatherProductionCalculator {

    private final double minKwh;
    private final double maxKwh;
    private final double fullSunRadiation;
    private final double variationFactor;
    private final Random random = new Random();

    public WeatherProductionCalculator(@Value("${app.production.min-kwh:0.001}") double minKwh,
                                       @Value("${app.production.max-kwh:0.006}") double maxKwh,
                                       @Value("${app.production.full-sun-radiation:800.0}") double fullSunRadiation,
                                       @Value("${app.production.variation-factor:0.1}") double variationFactor) {
        this.minKwh = minKwh;
        this.maxKwh = maxKwh;
        this.fullSunRadiation = fullSunRadiation;
        this.variationFactor = variationFactor;
    }

    /** 0 W/m2 (night) gives minKwh, fullSunRadiation W/m2 or more (bright sun) gives maxKwh. */
    public double calculateKwh(double solarRadiation) {
        double sunFactor = Math.min(solarRadiation / fullSunRadiation, 1.0);
        double kwh = minKwh + (maxKwh - minKwh) * sunFactor;
        double variation = (random.nextDouble() - 0.5) * (maxKwh - minKwh) * variationFactor;
        return Math.max(minKwh, Math.min(maxKwh, kwh + variation));
    }
}
