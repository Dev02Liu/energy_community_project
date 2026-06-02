package com.energy_community_project.percentage_service.service;

import com.energy_community_project.percentage_service.entity.CurrentPercentageEntity;
import com.energy_community_project.percentage_service.entity.HourlyUsageEntity;
import com.energy_community_project.percentage_service.repository.CurrentPercentageRepository;
import com.energy_community_project.percentage_service.repository.HourlyUsageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class CurrentPercentageCalculationService {

    private final HourlyUsageRepository hourlyUsageRepository;
    private final CurrentPercentageRepository currentPercentageRepository;
    private final Clock clock;

    public CurrentPercentageCalculationService(HourlyUsageRepository hourlyUsageRepository,
                                              CurrentPercentageRepository currentPercentageRepository,
                                              Clock clock) {
        this.hourlyUsageRepository = hourlyUsageRepository;
        this.currentPercentageRepository = currentPercentageRepository;
        this.clock = clock;
    }

    @Transactional
    public void updateCurrentPercentage(LocalDateTime hour) {
        if (hour == null || !hour.equals(currentHour())) {
            return;
        }

        HourlyUsageEntity hourlyUsage = hourlyUsageRepository.findById(hour).orElse(null);
        if (hourlyUsage == null) {
            return;
        }

        double produced = hourlyUsage.getCommunityProduced();
        double communityUsed = hourlyUsage.getCommunityUsed();
        double gridUsed = hourlyUsage.getGridUsed();

        double communityDepleted = produced > 0 ? roundPercentage((communityUsed / produced) * 100.0) : 0.0;
        double totalUsed = communityUsed + gridUsed;
        double gridPortion = totalUsed > 0 ? roundPercentage((gridUsed / totalUsed) * 100.0) : 0.0;

        CurrentPercentageEntity currentPercentage = currentPercentageRepository.findById(hour)
                .orElseGet(CurrentPercentageEntity::new);

        currentPercentage.setHour(hour);
        currentPercentage.setCommunityDepleted(communityDepleted);
        currentPercentage.setGridPortion(gridPortion);

        currentPercentageRepository.deleteAllByHourNot(hour);
        currentPercentageRepository.save(currentPercentage);
    }

    private LocalDateTime currentHour() {
        return LocalDateTime.now(clock).withMinute(0).withSecond(0).withNano(0);
    }

    private double roundPercentage(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
