package com.energy_community_project.percentage_service.service;

import com.energy_community_project.percentage_service.entity.CurrentPercentageEntity;
import com.energy_community_project.percentage_service.entity.HourlyUsageEntity;
import com.energy_community_project.percentage_service.repository.CurrentPercentageRepository;
import com.energy_community_project.percentage_service.repository.HourlyUsageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentPercentageCalculationServiceTest {

    private static final LocalDateTime HOUR = LocalDateTime.of(2025, 1, 10, 14, 0, 0);

    @Mock
    private HourlyUsageRepository hourlyUsageRepository;

    @Mock
    private CurrentPercentageRepository currentPercentageRepository;

    private CurrentPercentageCalculationService service;

    @BeforeEach
    void setUp() {
        service = new CurrentPercentageCalculationService(hourlyUsageRepository, currentPercentageRepository);
    }

    @Test
    void specExample_produced18_05_used18_05_grid1_076() {
        when(hourlyUsageRepository.findById(HOUR)).thenReturn(Optional.of(usage(18.05, 18.05, 1.076)));

        service.updateCurrentPercentage(HOUR);

        CurrentPercentageEntity saved = savedPercentage();
        assertThat(saved.getCommunityDepleted()).isEqualTo(100.0);
        assertThat(saved.getGridPortion()).isEqualTo(5.63);
    }

    @Test
    void normalCase_partialDepletionAndPartialGrid() {
        when(hourlyUsageRepository.findById(HOUR)).thenReturn(Optional.of(usage(20.0, 15.0, 5.0)));

        service.updateCurrentPercentage(HOUR);

        CurrentPercentageEntity saved = savedPercentage();
        assertThat(saved.getCommunityDepleted()).isCloseTo(75.0, within(0.001));
        assertThat(saved.getGridPortion()).isCloseTo(25.0, within(0.001));
    }

    @Test
    void fullDepletion_communityDepletedIs100_gridPortionIsZero() {
        when(hourlyUsageRepository.findById(HOUR)).thenReturn(Optional.of(usage(10.0, 10.0, 0.0)));

        service.updateCurrentPercentage(HOUR);

        CurrentPercentageEntity saved = savedPercentage();
        assertThat(saved.getCommunityDepleted()).isCloseTo(100.0, within(0.001));
        assertThat(saved.getGridPortion()).isEqualTo(0.0);
    }

    @Test
    void zeroProduction_allFromGrid_communityDepletedIsZero_gridPortionIs100() {
        when(hourlyUsageRepository.findById(HOUR)).thenReturn(Optional.of(usage(0.0, 0.0, 5.0)));

        service.updateCurrentPercentage(HOUR);

        CurrentPercentageEntity saved = savedPercentage();
        assertThat(saved.getCommunityDepleted()).isEqualTo(0.0);
        assertThat(saved.getGridPortion()).isCloseTo(100.0, within(0.001));
    }

    @Test
    void zeroTotalUsage_gridPortionIsZero() {
        when(hourlyUsageRepository.findById(HOUR)).thenReturn(Optional.of(usage(10.0, 0.0, 0.0)));

        service.updateCurrentPercentage(HOUR);

        CurrentPercentageEntity saved = savedPercentage();
        assertThat(saved.getCommunityDepleted()).isEqualTo(0.0);
        assertThat(saved.getGridPortion()).isEqualTo(0.0);
    }

    @Test
    void tableIsClearedBeforeCurrentValueIsSaved() {
        when(hourlyUsageRepository.findById(HOUR)).thenReturn(Optional.of(usage(10.0, 5.0, 0.0)));

        service.updateCurrentPercentage(HOUR);

        verify(currentPercentageRepository).deleteAll();
        verify(currentPercentageRepository).save(any(CurrentPercentageEntity.class));
    }

    @Test
    void percentageValuesAreRoundedToTwoDecimals() {
        when(hourlyUsageRepository.findById(HOUR)).thenReturn(Optional.of(usage(3.0, 1.0, 2.0)));

        service.updateCurrentPercentage(HOUR);

        CurrentPercentageEntity saved = savedPercentage();
        assertThat(saved.getCommunityDepleted()).isEqualTo(33.33);
        assertThat(saved.getGridPortion()).isEqualTo(66.67);
    }

    @Test
    void missingHourlyUsageRow_nothingIsSaved() {
        when(hourlyUsageRepository.findById(HOUR)).thenReturn(Optional.empty());

        service.updateCurrentPercentage(HOUR);

        verify(currentPercentageRepository, never()).save(any());
    }

    @Test
    void anyHourWithUsageCanBeCalculated() {
        LocalDateTime pastHour = HOUR.minusHours(1);
        when(hourlyUsageRepository.findById(pastHour)).thenReturn(Optional.of(usage(8.0, 4.0, 4.0)));

        service.updateCurrentPercentage(pastHour);

        assertThat(savedPercentage().getHour()).isEqualTo(pastHour);
    }

    private HourlyUsageEntity usage(double produced, double communityUsed, double gridUsed) {
        HourlyUsageEntity usage = new HourlyUsageEntity();
        usage.setHour(HOUR);
        usage.setCommunityProduced(produced);
        usage.setCommunityUsed(communityUsed);
        usage.setGridUsed(gridUsed);
        return usage;
    }

    private CurrentPercentageEntity savedPercentage() {
        ArgumentCaptor<CurrentPercentageEntity> captor = ArgumentCaptor.forClass(CurrentPercentageEntity.class);
        verify(currentPercentageRepository).save(captor.capture());
        return captor.getValue();
    }
}
