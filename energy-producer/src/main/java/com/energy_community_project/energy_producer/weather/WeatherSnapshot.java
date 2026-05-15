package com.energy_community_project.energy_producer.weather;

public record WeatherSnapshot(double cloudCoverPercent, boolean daylight, double shortwaveRadiationWm2) {

    public WeatherSnapshot {
        cloudCoverPercent = clamp(cloudCoverPercent, 0.0, 100.0);
        shortwaveRadiationWm2 = Math.max(shortwaveRadiationWm2, 0.0);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
