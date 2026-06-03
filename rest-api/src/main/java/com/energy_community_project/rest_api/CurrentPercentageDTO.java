package com.energy_community_project.rest_api;

/** Response payload for {@code GET /energy/current}: current-hour percentages. */
public class CurrentPercentageDTO {
    private String hour;
    private double communityDepleted;
    private double gridPortion;

    public CurrentPercentageDTO(String hour, double communityDepleted, double gridPortion) {
        this.hour = hour;
        this.communityDepleted = communityDepleted;
        this.gridPortion = gridPortion;
    }

    // Getters (required for JSON serialization).
    public String getHour() { return hour; }
    public double getCommunityDepleted() { return communityDepleted; }
    public double getGridPortion() { return gridPortion; }
}