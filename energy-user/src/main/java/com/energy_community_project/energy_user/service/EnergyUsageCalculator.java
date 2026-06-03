package com.energy_community_project.energy_user.service;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Computes demanded kWh from a base value plus variation, scaled by a time-of-day multiplier. */
@Component
public class EnergyUsageCalculator {

    private static final double BASE_USAGE_KWH = 0.001;

    /** (base + random variation) * time-of-day multiplier. */
    public double calculateKwh(LocalDateTime timestamp, double variationKwh) {
        double multiplier = multiplierForHour(timestamp.getHour());
        return (BASE_USAGE_KWH + variationKwh) * multiplier;
    }

    /** Higher demand at morning/evening peaks, lower overnight, baseline otherwise. */
    private double multiplierForHour(int hour) {
        if ((hour >= 7 && hour <= 9) || (hour >= 18 && hour <= 21)) {
            return 3.0; // peak hours
        }
        if (hour >= 23 || hour <= 5) {
            return 0.5; // night
        }
        return 1.0; // normal
    }
}
