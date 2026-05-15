package com.energy_community_project.usage_service.repository;

import com.energy_community_project.usage_service.entity.HourlyUsageEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class HourlyUsageRepositoryMigrationTest {

    @Autowired
    private HourlyUsageRepository hourlyUsageRepository;

    @Test
    void flywaySchemaSupportsHourlyUsageRepository() {
        LocalDateTime hour = LocalDateTime.of(2026, 5, 15, 14, 0);
        HourlyUsageEntity entity = new HourlyUsageEntity(hour, 18.05, 18.02, 1.056);

        hourlyUsageRepository.save(entity);

        HourlyUsageEntity saved = hourlyUsageRepository.findById(hour).orElseThrow();
        assertThat(saved.getCommunityProduced()).isEqualTo(18.05);
        assertThat(saved.getCommunityUsed()).isEqualTo(18.02);
        assertThat(saved.getGridUsed()).isEqualTo(1.056);
    }
}
