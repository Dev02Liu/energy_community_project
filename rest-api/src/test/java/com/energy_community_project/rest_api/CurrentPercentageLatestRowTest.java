package com.energy_community_project.rest_api;

import com.energy_community_project.rest_api.entity.CurrentPercentageEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the chosen current_percentage table semantics:
 * one row per hour is stored; GET /current returns the row with the highest hour.
 * See docs/spec-code-mapping.md — "current_percentage Table Semantics".
 */
@SpringBootTest
@Transactional
class CurrentPercentageLatestRowTest {

    @Autowired
    private CurrentPercentageRepository currentPercentageRepository;

    @Test
    void findFirstByOrderByHourDesc_returnsRowWithHighestHour() {
        LocalDateTime earlierHour = LocalDateTime.of(2025, 1, 10, 13, 0, 0);
        LocalDateTime laterHour = LocalDateTime.of(2025, 1, 10, 14, 0, 0);

        CurrentPercentageEntity earlier = new CurrentPercentageEntity();
        earlier.setHour(earlierHour);
        earlier.setCommunityDepleted(50.0);
        earlier.setGridPortion(10.0);

        CurrentPercentageEntity later = new CurrentPercentageEntity();
        later.setHour(laterHour);
        later.setCommunityDepleted(100.0);
        later.setGridPortion(5.63);

        currentPercentageRepository.save(earlier);
        currentPercentageRepository.save(later);

        CurrentPercentageEntity result = currentPercentageRepository.findFirstByOrderByHourDesc();

        assertThat(result).isNotNull();
        assertThat(result.getHour()).isEqualTo(laterHour);
        assertThat(result.getCommunityDepleted()).isEqualTo(100.0);
        assertThat(result.getGridPortion()).isEqualTo(5.63);
    }

    @Test
    void findFirstByOrderByHourDesc_withSingleRow_returnsThatRow() {
        LocalDateTime hour = LocalDateTime.of(2025, 1, 10, 14, 0, 0);
        CurrentPercentageEntity entity = new CurrentPercentageEntity();
        entity.setHour(hour);
        entity.setCommunityDepleted(75.0);
        entity.setGridPortion(20.0);
        currentPercentageRepository.save(entity);

        CurrentPercentageEntity result = currentPercentageRepository.findFirstByOrderByHourDesc();

        assertThat(result).isNotNull();
        assertThat(result.getHour()).isEqualTo(hour);
    }

    @Test
    void sameHourRow_isOverwrittenByPercentageService_notDuplicated() {
        LocalDateTime hour = LocalDateTime.of(2025, 1, 10, 14, 0, 0);

        CurrentPercentageEntity first = new CurrentPercentageEntity();
        first.setHour(hour);
        first.setCommunityDepleted(50.0);
        first.setGridPortion(10.0);
        currentPercentageRepository.save(first);

        // percentage-service recalculates and overwrites the same hour
        CurrentPercentageEntity updated = new CurrentPercentageEntity();
        updated.setHour(hour);
        updated.setCommunityDepleted(100.0);
        updated.setGridPortion(5.63);
        currentPercentageRepository.save(updated);

        assertThat(currentPercentageRepository.count()).isEqualTo(1);
        assertThat(currentPercentageRepository.findFirstByOrderByHourDesc().getCommunityDepleted())
                .isEqualTo(100.0);
    }
}
