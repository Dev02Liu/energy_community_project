package com.energy_community_project.percentage_service.service;

import com.energy_community_project.percentage_service.entity.CurrentPercentageEntity;
import com.energy_community_project.percentage_service.entity.HourlyUsageEntity;
import com.energy_community_project.percentage_service.repository.CurrentPercentageRepository;
import com.energy_community_project.percentage_service.repository.HourlyUsageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class CurrentPercentageCalculationService {

    private final HourlyUsageRepository hourlyUsageRepository;
    private final CurrentPercentageRepository currentPercentageRepository;

    public CurrentPercentageCalculationService(HourlyUsageRepository hourlyUsageRepository,
                                               CurrentPercentageRepository currentPercentageRepository) {
        this.hourlyUsageRepository = hourlyUsageRepository;
        this.currentPercentageRepository = currentPercentageRepository;
    }

    @Transactional
    public void updateCurrentPercentage(LocalDateTime hour) {
        HourlyUsageEntity hourlyUsage = hourlyUsageRepository.findById(hour).orElse(null);
        if (hourlyUsage == null) {
            return;
        }

        double produced = hourlyUsage.getCommunityProduced();
        double communityUsed = hourlyUsage.getCommunityUsed();
        double gridUsed = hourlyUsage.getGridUsed();
        double totalUsed = communityUsed + gridUsed;

        double communityDepleted = produced > 0 ? round(communityUsed / produced * 100.0) : 0.0;
        double gridPortion = totalUsed > 0 ? round(gridUsed / totalUsed * 100.0) : 0.0;

        // The table only ever holds the current hour, so replace the previous row.
        currentPercentageRepository.deleteAll();
        currentPercentageRepository.save(new CurrentPercentageEntity(hour, communityDepleted, gridPortion));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
