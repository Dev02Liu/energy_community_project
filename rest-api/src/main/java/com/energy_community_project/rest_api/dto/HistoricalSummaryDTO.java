package com.energy_community_project.rest_api.dto;

/** Response payload for {@code GET /energy/historical}: aggregated totals over a time range. */
public class HistoricalSummaryDTO {
    private double communityProduced;
    private double communityUsed;
    private double gridUsed;

    public HistoricalSummaryDTO(double communityProduced, double communityUsed, double gridUsed) {
        this.communityProduced = communityProduced;
        this.communityUsed = communityUsed;
        this.gridUsed = gridUsed;
    }

    // Getters (required so Spring Boot can serialize the fields to JSON).
    public double getCommunityProduced() { return communityProduced; }
    public double getCommunityUsed() { return communityUsed; }
    public double getGridUsed() { return gridUsed; }
}
