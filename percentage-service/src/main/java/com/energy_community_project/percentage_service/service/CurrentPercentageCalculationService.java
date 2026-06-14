package com.energy_community_project.percentage_service.service;

import com.energy_community_project.percentage_service.entity.CurrentPercentageEntity;
import com.energy_community_project.percentage_service.messaging.HourlyUsageUpdatedMessage;
import com.energy_community_project.percentage_service.repository.CurrentPercentageRepository;
import org.springframework.stereotype.Service;

@Service
public class CurrentPercentageCalculationService {

    private final CurrentPercentageRepository currentPercentageRepository;

    public CurrentPercentageCalculationService(CurrentPercentageRepository currentPercentageRepository) {
        this.currentPercentageRepository = currentPercentageRepository;
    }

    public void updateCurrentPercentage(HourlyUsageUpdatedMessage message) {
        double produced = message.getCommunityProduced();
        double communityUsed = message.getCommunityUsed();
        double gridUsed = message.getGridUsed();

        // Guard the message contract: kWh values can never be negative. Reject bad data instead of
        // storing a nonsensical (negative) percentage - the listener logs and drops the message.
        if (produced < 0 || communityUsed < 0 || gridUsed < 0) {
            throw new IllegalArgumentException("Negative kWh in update for hour " + message.getHour()
                    + " (produced=" + produced + ", communityUsed=" + communityUsed + ", gridUsed=" + gridUsed + ")");
        }

        double totalUsed = communityUsed + gridUsed;

        double communityDepleted = produced > 0 ? round(communityUsed / produced * 100.0) : 0.0;
        double gridPortion = totalUsed > 0 ? round(gridUsed / totalUsed * 100.0) : 0.0;

        // Upsert by hour (the primary key): the current hour's row is updated in place while past hours
        // stay, so the table keeps one historical percentage row per hour.
        currentPercentageRepository.save(new CurrentPercentageEntity(message.getHour(), communityDepleted, gridPortion));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
