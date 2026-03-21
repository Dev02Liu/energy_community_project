package com.energy_community_project.rest_api;

public class CurrentPercentageDTO {
    private String hour;
    private double communityDepleted;
    private double gridPortion;

    public CurrentPercentageDTO(String hour, double communityDepleted, double gridPortion) {
        this.hour = hour;
        this.communityDepleted = communityDepleted;
        this.gridPortion = gridPortion;
    }

    // Getter und Setter (wichtig für JSON-Serialisierung)
    public String getHour() { return hour; }
    public double getCommunityDepleted() { return communityDepleted; }
    public double getGridPortion() { return gridPortion; }
}