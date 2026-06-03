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

/**
 * Core business logic of the Percentage Service (30% grading weight).
 *
 * <p>On each usage-update event it recomputes the two percentages for the current hour from the
 * {@code hourly_usage} aggregate and stores them in {@code current_percentage}, keeping only the
 * current hour's row.
 */
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
        // Only the current hour is tracked; ignore null or stale (past-hour) update events.
        if (hour == null || !hour.equals(currentHour())) {
            return;
        }

        // Read the aggregate the Usage Service produced; nothing to do if it does not exist yet.
        HourlyUsageEntity hourlyUsage = hourlyUsageRepository.findById(hour).orElse(null);
        if (hourlyUsage == null) {
            return;
        }

        // Percentage formulas (division by zero -> 0): how depleted the community pool is, and how
        // much of total consumption came from the grid. Both rounded to two decimals.
        double produced = hourlyUsage.getCommunityProduced();
        double communityUsed = hourlyUsage.getCommunityUsed();
        double gridUsed = hourlyUsage.getGridUsed();

        double communityDepleted = produced > 0 ? roundPercentage((communityUsed / produced) * 100.0) : 0.0;
        double totalUsed = communityUsed + gridUsed;
        double gridPortion = totalUsed > 0 ? roundPercentage((gridUsed / totalUsed) * 100.0) : 0.0;

        // Upsert the current-hour row.
        CurrentPercentageEntity currentPercentage = currentPercentageRepository.findById(hour)
                .orElseGet(CurrentPercentageEntity::new);

        currentPercentage.setHour(hour);
        currentPercentage.setCommunityDepleted(communityDepleted);
        currentPercentage.setGridPortion(gridPortion);

        // Keep only the current hour: drop any previous-hour rows, then persist this one.
        currentPercentageRepository.deleteAllByHourNot(hour);
        currentPercentageRepository.save(currentPercentage);
    }

    /** The wall-clock hour, truncated — injected {@link Clock} keeps this testable. */
    private LocalDateTime currentHour() {
        return LocalDateTime.now(clock).withMinute(0).withSecond(0).withNano(0);
    }

    /** Rounds a percentage to two decimals using HALF_UP, matching the documented contract. */
    private double roundPercentage(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
