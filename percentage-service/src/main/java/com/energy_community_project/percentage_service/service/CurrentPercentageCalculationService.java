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
        if (hour == null) {
            return;
        }

        HourlyUsageEntity hourlyUsage = hourlyUsageRepository.findById(hour).orElse(null);
        if (hourlyUsage == null) {
            return;
        }

        double produced = hourlyUsage.getCommunityProduced();
        double communityUsed = hourlyUsage.getCommunityUsed();
        double gridUsed = hourlyUsage.getGridUsed();

        double communityDepleted = produced > 0 ? (communityUsed / produced) * 100.0 : 0.0;
        double totalUsed = communityUsed + gridUsed;
        double gridPortion = totalUsed > 0 ? (gridUsed / totalUsed) * 100.0 : 0.0;

        CurrentPercentageEntity currentPercentage = currentPercentageRepository.findById(hour)
                .orElseGet(CurrentPercentageEntity::new);

        currentPercentage.setHour(hour);
        currentPercentage.setCommunityDepleted(communityDepleted);
        currentPercentage.setGridPortion(gridPortion);

        currentPercentageRepository.save(currentPercentage);
    }
}
