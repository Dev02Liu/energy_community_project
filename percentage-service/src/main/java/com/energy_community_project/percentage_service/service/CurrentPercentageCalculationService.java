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

    // Calculates the percentages from the update message only; it never reads the hourly_usage table.
    public void updateCurrentPercentage(HourlyUsageUpdatedMessage message) {
        double produced = message.getCommunityProduced();
        double communityUsed = message.getCommunityUsed();
        double gridUsed = message.getGridUsed();
        double totalUsed = communityUsed + gridUsed;

        double communityDepleted = produced > 0 ? round(communityUsed / produced * 100.0) : 0.0;
        double gridPortion = totalUsed > 0 ? round(gridUsed / totalUsed * 100.0) : 0.0;

        // The table only ever holds the current hour, so replace the previous row.
        currentPercentageRepository.deleteAll();
        currentPercentageRepository.save(new CurrentPercentageEntity(message.getHour(), communityDepleted, gridPortion));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
