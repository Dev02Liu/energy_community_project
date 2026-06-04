package com.energy_community_project.energy_user.service;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class EnergyUsageCalculator {

    private static final double BASE_USAGE_KWH = 0.001;

    public double calculateKwh(LocalDateTime timestamp, double variationKwh) {
        double multiplier = multiplierForHour(timestamp.getHour());
        return (BASE_USAGE_KWH + variationKwh) * multiplier;
    }

    private double multiplierForHour(int hour) {
        if ((hour >= 7 && hour <= 9) || (hour >= 18 && hour <= 21)) {
            return 3.0;
        }
        if (hour >= 23 || hour <= 5) {
            return 0.5;
        }
        return 1.0;
    }
}
