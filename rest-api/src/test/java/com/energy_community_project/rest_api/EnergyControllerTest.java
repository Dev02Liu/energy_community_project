package com.energy_community_project.rest_api;

import com.energy_community_project.rest_api.entity.CurrentPercentageEntity;
import com.energy_community_project.rest_api.entity.HourlyUsageEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

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

    // --- GET /energy/current ---

    @Test
    void getCurrent_returnsLatestPercentageRow() throws Exception {
        LocalDateTime hour = LocalDateTime.of(2025, 1, 10, 14, 0, 0);
        CurrentPercentageEntity entity = new CurrentPercentageEntity();
        entity.setHour(hour);
        entity.setCommunityDepleted(100.0);
        entity.setGridPortion(5.63);
        when(currentPercentageRepository.findFirstByOrderByHourDesc()).thenReturn(entity);

        mvc.perform(get("/energy/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hour").value("2025-01-10T14:00"))
                .andExpect(jsonPath("$.communityDepleted").value(100.0))
                .andExpect(jsonPath("$.gridPortion").value(5.63));
    }

    @Test
    void getCurrent_whenNoDataExists_returnsZeros() throws Exception {
        when(currentPercentageRepository.findFirstByOrderByHourDesc()).thenReturn(null);

        mvc.perform(get("/energy/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.communityDepleted").value(0.0))
                .andExpect(jsonPath("$.gridPortion").value(0.0));
    }

    @Test
    void getCurrent_whenMultipleRowsExist_returnsRowWithHighestHour() throws Exception {
        LocalDateTime latestHour = LocalDateTime.of(2025, 1, 10, 15, 0, 0);
        CurrentPercentageEntity latest = new CurrentPercentageEntity();
        latest.setHour(latestHour);
        latest.setCommunityDepleted(75.0);
        latest.setGridPortion(12.5);
        when(currentPercentageRepository.findFirstByOrderByHourDesc()).thenReturn(latest);

        mvc.perform(get("/energy/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hour").value("2025-01-10T15:00"))
                .andExpect(jsonPath("$.communityDepleted").value(75.0));
    }

    // --- GET /energy/historical ---

    @Test
    void getHistorical_validIsoDateRange_returnsMatchingRows() throws Exception {
        LocalDateTime hour = LocalDateTime.of(2025, 1, 10, 14, 0, 0);
        HourlyUsageEntity entity = new HourlyUsageEntity();
        entity.setHour(hour);
        entity.setCommunityProduced(18.05);
        entity.setCommunityUsed(18.05);
        entity.setGridUsed(1.076);
        when(hourlyUsageRepository.findByHourBetween(
                LocalDateTime.of(2025, 1, 10, 0, 0, 0),
                LocalDateTime.of(2025, 1, 10, 23, 0, 0)))
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
    void getHistorical_validGermanDateFormat_returnsData() throws Exception {
        LocalDateTime hour = LocalDateTime.of(2025, 1, 10, 14, 0, 0);
        HourlyUsageEntity entity = new HourlyUsageEntity();
        entity.setHour(hour);
        entity.setCommunityProduced(10.0);
        entity.setCommunityUsed(8.0);
        entity.setGridUsed(2.0);
        when(hourlyUsageRepository.findByHourBetween(
                LocalDateTime.of(2025, 1, 10, 0, 0),
                LocalDateTime.of(2025, 1, 10, 23, 0)))
                .thenReturn(List.of(entity));

        mvc.perform(get("/energy/historical")
                        .param("start", "10.01.2025 00:00")
                        .param("end", "10.01.2025 23:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].communityProduced").value(10.0));
    }

    @Test
    void getHistorical_invalidDateFormat_returns400() throws Exception {
        mvc.perform(get("/energy/historical")
                        .param("start", "not-a-date")
                        .param("end", "2025-01-10T23:00:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getHistorical_startAfterEnd_returns400() throws Exception {
        mvc.perform(get("/energy/historical")
                        .param("start", "2025-01-10T23:00:00")
                        .param("end", "2025-01-10T00:00:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getHistorical_emptyResult_returnsEmptyArray() throws Exception {
        when(hourlyUsageRepository.findByHourBetween(
                LocalDateTime.of(2025, 1, 10, 0, 0, 0),
                LocalDateTime.of(2025, 1, 10, 23, 0, 0)))
                .thenReturn(List.of());

        mvc.perform(get("/energy/historical")
                        .param("start", "2025-01-10T00:00:00")
                        .param("end", "2025-01-10T23:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
