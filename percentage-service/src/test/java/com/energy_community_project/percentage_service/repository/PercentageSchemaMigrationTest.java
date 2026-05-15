package com.energy_community_project.percentage_service.repository;

import com.energy_community_project.percentage_service.entity.CurrentPercentageEntity;
import com.energy_community_project.percentage_service.entity.HourlyUsageEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PercentageSchemaMigrationTest {

    @Autowired
    private HourlyUsageRepository hourlyUsageRepository;

    @Autowired
    private CurrentPercentageRepository currentPercentageRepository;

    @Test
    void flywaySchemaSupportsHourlyUsageAndCurrentPercentageRepositories() {
        LocalDateTime hour = LocalDateTime.of(2026, 5, 15, 14, 0);

        HourlyUsageEntity hourlyUsage = new HourlyUsageEntity();
        hourlyUsage.setHour(hour);
        hourlyUsage.setCommunityProduced(18.05);
        hourlyUsage.setCommunityUsed(18.05);
        hourlyUsage.setGridUsed(1.076);
        hourlyUsageRepository.save(hourlyUsage);

        CurrentPercentageEntity currentPercentage = new CurrentPercentageEntity(hour, 100.0, 5.63);
        currentPercentageRepository.save(currentPercentage);

        assertThat(hourlyUsageRepository.findById(hour)).isPresent();
        CurrentPercentageEntity saved = currentPercentageRepository.findById(hour).orElseThrow();
        assertThat(saved.getCommunityDepleted()).isEqualTo(100.0);
        assertThat(saved.getGridPortion()).isEqualTo(5.63);
    }
}
