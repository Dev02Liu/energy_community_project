package com.energy_community_project.rest_api;

import com.energy_community_project.rest_api.entity.CurrentPercentageEntity;
import com.energy_community_project.rest_api.entity.HourlyUsageEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
class EnergyEndpointContractTest {

    private static final LocalDateTime HISTORICAL_HOUR = LocalDateTime.of(2025, 1, 10, 14, 0);
    private static final LocalDateTime PERCENTAGE_HOUR = LocalDateTime.of(2025, 1, 10, 15, 0);

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private HourlyUsageRepository hourlyUsageRepository;

    @Autowired
    private CurrentPercentageRepository currentPercentageRepository;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(wac).build();

        HourlyUsageEntity usage = new HourlyUsageEntity();
        usage.setHour(HISTORICAL_HOUR);
        usage.setCommunityProduced(18.05);
        usage.setCommunityUsed(18.05);
        usage.setGridUsed(1.076);
        hourlyUsageRepository.save(usage);

        CurrentPercentageEntity percentage = new CurrentPercentageEntity();
        percentage.setHour(PERCENTAGE_HOUR);
        percentage.setCommunityDepleted(100.0);
        percentage.setGridPortion(5.63);
        currentPercentageRepository.save(percentage);

        hourlyUsageRepository.flush();
        currentPercentageRepository.flush();
    }

    @Test
    void getCurrent_returnsLatestPercentageRowFromDatabase() throws Exception {
        CurrentPercentageEntity oldPercentage = new CurrentPercentageEntity();
        oldPercentage.setHour(PERCENTAGE_HOUR.minusHours(1));
        oldPercentage.setCommunityDepleted(75.0);
        oldPercentage.setGridPortion(12.0);
        currentPercentageRepository.saveAndFlush(oldPercentage);

        mvc.perform(get("/energy/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hour").value(PERCENTAGE_HOUR.toString()))
                .andExpect(jsonPath("$.communityDepleted").value(100.0))
                .andExpect(jsonPath("$.gridPortion").value(5.63));
    }

    @Test
    void getCurrent_jsonFieldNamesMatchGuiDto() throws Exception {
        mvc.perform(get("/energy/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hour").exists())
                .andExpect(jsonPath("$.communityDepleted").exists())
                .andExpect(jsonPath("$.gridPortion").exists());
    }

    @Test
    void getHistorical_isoFormat_returnsMatchingRow() throws Exception {
        mvc.perform(get("/energy/historical")
                        .param("start", "2025-01-10T00:00:00")
                        .param("end", "2025-01-10T23:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].hour").value("2025-01-10T14:00"))
                .andExpect(jsonPath("$[0].communityProduced").value(18.05))
                .andExpect(jsonPath("$[0].communityUsed").value(18.05))
                .andExpect(jsonPath("$[0].gridUsed").value(1.076));
    }

    @Test
    void getHistorical_isoFormat_jsonFieldNamesMatchGuiDto() throws Exception {
        mvc.perform(get("/energy/historical")
                        .param("start", "2025-01-10T00:00:00")
                        .param("end", "2025-01-10T23:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].hour").exists())
                .andExpect(jsonPath("$[0].communityProduced").exists())
                .andExpect(jsonPath("$[0].communityUsed").exists())
                .andExpect(jsonPath("$[0].gridUsed").exists());
    }

    @Test
    void getHistorical_isoFormat_rowOutsideRangeIsExcluded() throws Exception {
        mvc.perform(get("/energy/historical")
                        .param("start", "2025-01-10T15:00:00")
                        .param("end", "2025-01-10T23:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getHistorical_invalidStartFormat_returns400() throws Exception {
        mvc.perform(get("/energy/historical")
                        .param("start", "not-a-date")
                        .param("end", "2025-01-10T23:00:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getHistorical_invalidEndFormat_returns400() throws Exception {
        mvc.perform(get("/energy/historical")
                        .param("start", "2025-01-10T00:00:00")
                        .param("end", "31.02.2025 99:99"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getHistorical_startEqualsEnd_isAllowed() throws Exception {
        mvc.perform(get("/energy/historical")
                        .param("start", "2025-01-10T14:00:00")
                        .param("end", "2025-01-10T14:00:00"))
                .andExpect(status().isOk());
    }
}
