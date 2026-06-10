package com.energy_community_project.rest_api.dto;

/** Response payload for {@code GET /energy/historical}: one aggregated hourly-usage row. */
public class HistoricalUsageDTO {
    private String hour;
    private double communityProduced;
    private double communityUsed;
    private double gridUsed;

    public HistoricalUsageDTO(String hour, double communityProduced, double communityUsed, double gridUsed) {
        this.hour = hour;
        this.communityProduced = communityProduced;
        this.communityUsed = communityUsed;
        this.gridUsed = gridUsed;
    }

    // Getters (required so Spring Boot can serialize the fields to JSON).
    public String getHour() {
        return hour;
    }

    public double getCommunityProduced() {
        return communityProduced;
    }

    public double getCommunityUsed() {
        return communityUsed;
    }

    public double getGridUsed() {
        return gridUsed;
    }
}
