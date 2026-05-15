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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentPercentageCalculationServiceTest {

    @Mock
    private HourlyUsageRepository hourlyUsageRepository;

    @Mock
    private CurrentPercentageRepository currentPercentageRepository;

    private CurrentPercentageCalculationService service;

    private static final LocalDateTime HOUR = LocalDateTime.of(2025, 1, 10, 14, 0, 0);

    @BeforeEach
    void setUp() {
        service = new CurrentPercentageCalculationService(hourlyUsageRepository, currentPercentageRepository);
    }

    private HourlyUsageEntity usage(double produced, double communityUsed, double gridUsed) {
        HourlyUsageEntity e = new HourlyUsageEntity();
        e.setHour(HOUR);
        e.setCommunityProduced(produced);
        e.setCommunityUsed(communityUsed);
        e.setGridUsed(gridUsed);
        return e;
    }

    // --- Spec example (Acceptance Criteria) ---

    @Test
    void specExample_produced18_05_used18_05_grid1_076() {
        // communityDepleted = (18.05 / 18.05) * 100 = 100.0
        // totalUsed = 18.05 + 1.076 = 19.126
        // gridPortion = (1.076 / 19.126) * 100 ≈ 5.63
        when(hourlyUsageRepository.findById(HOUR)).thenReturn(Optional.of(usage(18.05, 18.05, 1.076)));
        when(currentPercentageRepository.findById(HOUR)).thenReturn(Optional.empty());
        when(currentPercentageRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.updateCurrentPercentage(HOUR);

        ArgumentCaptor<CurrentPercentageEntity> captor = ArgumentCaptor.forClass(CurrentPercentageEntity.class);
        verify(currentPercentageRepository).save(captor.capture());
        CurrentPercentageEntity saved = captor.getValue();
        assertThat(saved.getCommunityDepleted()).isCloseTo(100.0, within(0.01));
        assertThat(saved.getGridPortion()).isCloseTo(5.63, within(0.01));
    }

    // --- Normal case ---

    @Test
    void normalCase_partialDepletionAndPartialGrid() {
        // communityDepleted = (15 / 20) * 100 = 75.0
        // gridPortion = (5 / 20) * 100 = 25.0
        when(hourlyUsageRepository.findById(HOUR)).thenReturn(Optional.of(usage(20.0, 15.0, 5.0)));
        when(currentPercentageRepository.findById(HOUR)).thenReturn(Optional.empty());
        when(currentPercentageRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.updateCurrentPercentage(HOUR);

        ArgumentCaptor<CurrentPercentageEntity> captor = ArgumentCaptor.forClass(CurrentPercentageEntity.class);
        verify(currentPercentageRepository).save(captor.capture());
        assertThat(captor.getValue().getCommunityDepleted()).isCloseTo(75.0, within(0.001));
        assertThat(captor.getValue().getGridPortion()).isCloseTo(25.0, within(0.001));
    }

    // --- Full depletion ---

    @Test
    void fullDepletion_communityDepletedIs100_gridPortionIsZero() {
        when(hourlyUsageRepository.findById(HOUR)).thenReturn(Optional.of(usage(10.0, 10.0, 0.0)));
        when(currentPercentageRepository.findById(HOUR)).thenReturn(Optional.empty());
        when(currentPercentageRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.updateCurrentPercentage(HOUR);

        ArgumentCaptor<CurrentPercentageEntity> captor = ArgumentCaptor.forClass(CurrentPercentageEntity.class);
        verify(currentPercentageRepository).save(captor.capture());
        assertThat(captor.getValue().getCommunityDepleted()).isCloseTo(100.0, within(0.001));
        assertThat(captor.getValue().getGridPortion()).isEqualTo(0.0);
    }

    // --- Zero production ---

    @Test
    void zeroProduction_communityDepletedIsZero() {
        when(hourlyUsageRepository.findById(HOUR)).thenReturn(Optional.of(usage(0.0, 0.0, 0.0)));
        when(currentPercentageRepository.findById(HOUR)).thenReturn(Optional.empty());
        when(currentPercentageRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.updateCurrentPercentage(HOUR);

        ArgumentCaptor<CurrentPercentageEntity> captor = ArgumentCaptor.forClass(CurrentPercentageEntity.class);
        verify(currentPercentageRepository).save(captor.capture());
        assertThat(captor.getValue().getCommunityDepleted()).isEqualTo(0.0);
        assertThat(captor.getValue().getGridPortion()).isEqualTo(0.0);
    }

    @Test
    void zeroProduction_allFromGrid_communityDepletedIsZero_gridPortionIs100() {
        // No community production, user draws entirely from grid
        when(hourlyUsageRepository.findById(HOUR)).thenReturn(Optional.of(usage(0.0, 0.0, 5.0)));
        when(currentPercentageRepository.findById(HOUR)).thenReturn(Optional.empty());
        when(currentPercentageRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.updateCurrentPercentage(HOUR);

        ArgumentCaptor<CurrentPercentageEntity> captor = ArgumentCaptor.forClass(CurrentPercentageEntity.class);
        verify(currentPercentageRepository).save(captor.capture());
        assertThat(captor.getValue().getCommunityDepleted()).isEqualTo(0.0);
        assertThat(captor.getValue().getGridPortion()).isCloseTo(100.0, within(0.001));
    }

    // --- Zero total usage ---

    @Test
    void zeroTotalUsage_gridPortionIsZero() {
        when(hourlyUsageRepository.findById(HOUR)).thenReturn(Optional.of(usage(10.0, 0.0, 0.0)));
        when(currentPercentageRepository.findById(HOUR)).thenReturn(Optional.empty());
        when(currentPercentageRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.updateCurrentPercentage(HOUR);

        ArgumentCaptor<CurrentPercentageEntity> captor = ArgumentCaptor.forClass(CurrentPercentageEntity.class);
        verify(currentPercentageRepository).save(captor.capture());
        assertThat(captor.getValue().getCommunityDepleted()).isEqualTo(0.0);
        assertThat(captor.getValue().getGridPortion()).isEqualTo(0.0);
    }

    // --- Existing row is updated ---

    @Test
    void existingRowIsOverwrittenWithNewValues() {
        CurrentPercentageEntity existing = new CurrentPercentageEntity(HOUR, 50.0, 10.0);
        when(hourlyUsageRepository.findById(HOUR)).thenReturn(Optional.of(usage(20.0, 20.0, 0.0)));
        when(currentPercentageRepository.findById(HOUR)).thenReturn(Optional.of(existing));
        when(currentPercentageRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.updateCurrentPercentage(HOUR);

        ArgumentCaptor<CurrentPercentageEntity> captor = ArgumentCaptor.forClass(CurrentPercentageEntity.class);
        verify(currentPercentageRepository).save(captor.capture());
        assertThat(captor.getValue().getCommunityDepleted()).isCloseTo(100.0, within(0.001));
        assertThat(captor.getValue().getGridPortion()).isEqualTo(0.0);
        assertThat(captor.getValue().getHour()).isEqualTo(HOUR);
    }

    @Test
    void newRowIsCreatedWhenNoExistingPercentageRow() {
        when(hourlyUsageRepository.findById(HOUR)).thenReturn(Optional.of(usage(10.0, 5.0, 2.0)));
        when(currentPercentageRepository.findById(HOUR)).thenReturn(Optional.empty());
        when(currentPercentageRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.updateCurrentPercentage(HOUR);

        ArgumentCaptor<CurrentPercentageEntity> captor = ArgumentCaptor.forClass(CurrentPercentageEntity.class);
        verify(currentPercentageRepository).save(captor.capture());
        assertThat(captor.getValue().getHour()).isEqualTo(HOUR);
        assertThat(captor.getValue().getCommunityDepleted()).isCloseTo(50.0, within(0.001));
        assertThat(captor.getValue().getGridPortion()).isCloseTo(28.57, within(0.01));
    }

    // --- Missing hourly_usage row ---

    @Test
    void missingHourlyUsageRow_nothingIsSaved() {
        when(hourlyUsageRepository.findById(HOUR)).thenReturn(Optional.empty());

        service.updateCurrentPercentage(HOUR);

        verify(currentPercentageRepository, never()).save(any());
    }

    // --- Null hour ---

    @Test
    void nullHour_nothingHappens() {
        service.updateCurrentPercentage(null);

        verifyNoInteractions(hourlyUsageRepository, currentPercentageRepository);
    }
}
