package com.energy_community_project.energy_user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class EnergyUsageCalculator {

    private final double baseUsageKwh;
    private final double peakMultiplier;
    private final double nightMultiplier;
    private final double normalMultiplier;

    public EnergyUsageCalculator(@Value("${app.usage.base-kwh}") double baseUsageKwh,
                                 @Value("${app.usage.peak-multiplier}") double peakMultiplier,
                                 @Value("${app.usage.night-multiplier}") double nightMultiplier,
                                 @Value("${app.usage.normal-multiplier}") double normalMultiplier) {
        this.baseUsageKwh = baseUsageKwh;
        this.peakMultiplier = peakMultiplier;
        this.nightMultiplier = nightMultiplier;
        this.normalMultiplier = normalMultiplier;
    }

    public double calculateKwh(LocalDateTime timestamp, double variationKwh) {
        double multiplier = multiplierForHour(timestamp.getHour());
        return (baseUsageKwh + variationKwh) * multiplier;
    }

    private double multiplierForHour(int hour) {
        if ((hour >= 7 && hour <= 9) || (hour >= 18 && hour <= 21)) {
            return peakMultiplier;
        }
        if (hour >= 23 || hour <= 5) {
            return nightMultiplier;
        }
        return normalMultiplier;
    }
}
