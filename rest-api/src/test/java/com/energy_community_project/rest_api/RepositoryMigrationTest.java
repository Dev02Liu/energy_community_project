package com.energy_community_project.rest_api;

import com.energy_community_project.rest_api.entity.CurrentPercentageEntity;
import com.energy_community_project.rest_api.entity.HourlyUsageEntity;
import com.energy_community_project.rest_api.repository.CurrentPercentageRepository;
import com.energy_community_project.rest_api.repository.HourlyUsageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RepositoryMigrationTest {

    @Autowired
    private HourlyUsageRepository hourlyUsageRepository;

    @Autowired
    private CurrentPercentageRepository currentPercentageRepository;

    @Test
    void flywaySchemaSupportsRestApiRepositories() {
        LocalDateTime hour = LocalDateTime.of(2026, 5, 15, 14, 0);

        HourlyUsageEntity hourlyUsage = new HourlyUsageEntity();
        hourlyUsage.setHour(hour);
        hourlyUsage.setCommunityProduced(18.05);
        hourlyUsage.setCommunityUsed(18.05);
        hourlyUsage.setGridUsed(1.076);
        hourlyUsageRepository.save(hourlyUsage);

        CurrentPercentageEntity currentPercentage = new CurrentPercentageEntity();
        currentPercentage.setHour(hour);
        currentPercentage.setCommunityDepleted(100.0);
        currentPercentage.setGridPortion(5.63);
        currentPercentageRepository.save(currentPercentage);

        List<HourlyUsageEntity> historical = hourlyUsageRepository.findByHourBetween(
                hour.minusHours(1),
                hour.plusHours(1)
        );

        assertThat(historical).hasSize(1);
        assertThat(currentPercentageRepository.findById(hour).orElseThrow().getHour()).isEqualTo(hour);
    }
}
