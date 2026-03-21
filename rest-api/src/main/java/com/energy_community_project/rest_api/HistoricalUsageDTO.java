package com.energy_community_project.rest_api;

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

    // --- GETTER ---
    // Diese sind extrem wichtig, damit Spring Boot die Daten als JSON ausgeben kann!

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