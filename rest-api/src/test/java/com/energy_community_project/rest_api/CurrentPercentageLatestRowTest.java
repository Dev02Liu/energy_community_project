package com.energy_community_project.rest_api;

import com.energy_community_project.rest_api.entity.CurrentPercentageEntity;
import com.energy_community_project.rest_api.repository.CurrentPercentageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CurrentPercentageLatestRowTest {

    @Autowired
    private CurrentPercentageRepository currentPercentageRepository;

    @Test
    void findFirstByOrderByHourDesc_returnsLatestHour() {
        LocalDateTime earlierHour = LocalDateTime.of(2025, 1, 10, 13, 0);
        LocalDateTime laterHour = LocalDateTime.of(2025, 1, 10, 14, 0);

        currentPercentageRepository.save(percentage(earlierHour, 50.0, 10.0));
        currentPercentageRepository.save(percentage(laterHour, 100.0, 5.63));

        CurrentPercentageEntity result = currentPercentageRepository.findFirstByOrderByHourDesc().orElseThrow();

        assertThat(result.getHour()).isEqualTo(laterHour);
        assertThat(result.getCommunityDepleted()).isEqualTo(100.0);
        assertThat(result.getGridPortion()).isEqualTo(5.63);
    }

    @Test
    void sameHourRow_isOverwritten_notDuplicated() {
        LocalDateTime hour = LocalDateTime.of(2025, 1, 10, 14, 0);

        currentPercentageRepository.save(percentage(hour, 50.0, 10.0));
        currentPercentageRepository.save(percentage(hour, 100.0, 5.63));

        assertThat(currentPercentageRepository.count()).isEqualTo(1);
        assertThat(currentPercentageRepository.findById(hour).orElseThrow().getCommunityDepleted())
                .isEqualTo(100.0);
    }

    private CurrentPercentageEntity percentage(LocalDateTime hour, double communityDepleted, double gridPortion) {
        CurrentPercentageEntity percentage = new CurrentPercentageEntity();
        percentage.setHour(hour);
        percentage.setCommunityDepleted(communityDepleted);
        percentage.setGridPortion(gridPortion);
        return percentage;
    }
}
