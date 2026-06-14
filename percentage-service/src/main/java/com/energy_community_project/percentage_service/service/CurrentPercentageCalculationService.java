package com.energy_community_project.percentage_service.service;

import com.energy_community_project.percentage_service.entity.CurrentPercentageEntity;
import com.energy_community_project.percentage_service.messaging.HourlyUsageUpdatedMessage;
import com.energy_community_project.percentage_service.repository.CurrentPercentageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentPercentageCalculationService {

    private final CurrentPercentageRepository currentPercentageRepository;

    public CurrentPercentageCalculationService(CurrentPercentageRepository currentPercentageRepository) {
        this.currentPercentageRepository = currentPercentageRepository;
    }

    // Runs delete + save as one atomic unit: on any failure the whole step rolls back,
    // so the table is never left empty (no window between delete and save).
    @Transactional
    public void updateCurrentPercentage(HourlyUsageUpdatedMessage message) {
        double produced = message.getCommunityProduced();
        double communityUsed = message.getCommunityUsed();
        double gridUsed = message.getGridUsed();
        double totalUsed = communityUsed + gridUsed;

        double communityDepleted = produced > 0 ? round(communityUsed / produced * 100.0) : 0.0;
        double gridPortion = totalUsed > 0 ? round(gridUsed / totalUsed * 100.0) : 0.0;

        // The table only ever holds the current hour
        currentPercentageRepository.deleteAll();
        currentPercentageRepository.save(new CurrentPercentageEntity(message.getHour(), communityDepleted, gridPortion));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
