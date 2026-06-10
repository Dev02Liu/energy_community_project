package com.energy_community_project.rest_api;

import com.energy_community_project.rest_api.controller.EnergyController;
import com.energy_community_project.rest_api.entity.CurrentPercentageEntity;
import com.energy_community_project.rest_api.entity.HourlyUsageEntity;
import com.energy_community_project.rest_api.repository.CurrentPercentageRepository;
import com.energy_community_project.rest_api.repository.HourlyUsageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EnergyControllerTest {

    @Mock
    private CurrentPercentageRepository currentPercentageRepository;

    @Mock
    private HourlyUsageRepository hourlyUsageRepository;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        EnergyController controller = new EnergyController(currentPercentageRepository, hourlyUsageRepository);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getCurrent_returnsLatestPercentageRow() throws Exception {
        CurrentPercentageEntity entity = new CurrentPercentageEntity();
        entity.setHour(LocalDateTime.of(2025, 1, 10, 14, 0));
        entity.setCommunityDepleted(100.0);
        entity.setGridPortion(5.63);
        when(currentPercentageRepository.findFirstByOrderByHourDesc()).thenReturn(Optional.of(entity));

        mvc.perform(get("/energy/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hour").value("2025-01-10T14:00"))
                .andExpect(jsonPath("$.communityDepleted").value(100.0))
                .andExpect(jsonPath("$.gridPortion").value(5.63));
    }

    @Test
    void getCurrent_whenNoDataExists_returnsZeros() throws Exception {
        when(currentPercentageRepository.findFirstByOrderByHourDesc()).thenReturn(Optional.empty());

        mvc.perform(get("/energy/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.communityDepleted").value(0.0))
                .andExpect(jsonPath("$.gridPortion").value(0.0));
    }

    @Test
    void getHistorical_validIsoDateRange_returnsMatchingRows() throws Exception {
        LocalDateTime hour = LocalDateTime.of(2025, 1, 10, 14, 0);
        HourlyUsageEntity entity = new HourlyUsageEntity();
        entity.setHour(hour);
        entity.setCommunityProduced(18.05);
        entity.setCommunityUsed(18.05);
        entity.setGridUsed(1.076);
        when(hourlyUsageRepository.findByHourBetween(
                LocalDateTime.of(2025, 1, 10, 0, 0),
                LocalDateTime.of(2025, 1, 10, 23, 0)))
                .thenReturn(List.of(entity));

        mvc.perform(get("/energy/historical")
                        .param("start", "2025-01-10T00:00:00")
                        .param("end", "2025-01-10T23:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].hour").value("2025-01-10T14:00"))
                .andExpect(jsonPath("$[0].communityProduced").value(18.05))
                .andExpect(jsonPath("$[0].communityUsed").value(18.05))
                .andExpect(jsonPath("$[0].gridUsed").value(1.076));
    }

    @Test
    void getHistorical_invalidDateFormat_returns400() throws Exception {
        mvc.perform(get("/energy/historical")
                        .param("start", "not-a-date")
                        .param("end", "2025-01-10T23:00:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getHistorical_emptyResult_returnsEmptyArray() throws Exception {
        when(hourlyUsageRepository.findByHourBetween(
                LocalDateTime.of(2025, 1, 10, 0, 0),
                LocalDateTime.of(2025, 1, 10, 23, 0)))
                .thenReturn(List.of());

        mvc.perform(get("/energy/historical")
                        .param("start", "2025-01-10T00:00:00")
                        .param("end", "2025-01-10T23:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
