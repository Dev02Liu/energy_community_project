package com.energy_community_project.rest_api.service;

import com.energy_community_project.rest_api.dto.CurrentPercentageDTO;
import com.energy_community_project.rest_api.dto.HistoricalSummaryDTO;
import com.energy_community_project.rest_api.dto.HistoricalUsageDTO;
import com.energy_community_project.rest_api.repository.CurrentPercentageRepository;
import com.energy_community_project.rest_api.repository.HourlyUsageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Read-side service between the REST controller and the JPA repositories: it loads the data and maps it
 * to response DTOs, so the controller stays a thin HTTP boundary without data-access logic.
 */
@Service
public class EnergyReadService {

    private final CurrentPercentageRepository currentPercentageRepository;
    private final HourlyUsageRepository hourlyUsageRepository;

    public EnergyReadService(CurrentPercentageRepository currentPercentageRepository,
                             HourlyUsageRepository hourlyUsageRepository) {
        this.currentPercentageRepository = currentPercentageRepository;
        this.hourlyUsageRepository = hourlyUsageRepository;
    }

    public CurrentPercentageDTO getCurrentPercentage() {
        return currentPercentageRepository.findFirstByOrderByHourDesc()
                .map(c -> new CurrentPercentageDTO(c.getHour().toString(), c.getCommunityDepleted(), c.getGridPortion()))
                .orElseGet(() -> new CurrentPercentageDTO(null, 0.0, 0.0));
    }

    public HistoricalSummaryDTO getHistoricalData(LocalDateTime start, LocalDateTime end) {
        if (start.isAfter(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "start must be before or equal to end");
        }

        List<HistoricalUsageDTO> rows = hourlyUsageRepository.findByHourBetween(start, end).stream()
                .map(entity -> new HistoricalUsageDTO(
                        entity.getHour().toString(),
                        entity.getCommunityProduced(),
                        entity.getCommunityUsed(),
                        entity.getGridUsed()))
                .toList();

        return new HistoricalSummaryDTO(
                rows.stream().mapToDouble(HistoricalUsageDTO::getCommunityProduced).sum(),
                rows.stream().mapToDouble(HistoricalUsageDTO::getCommunityUsed).sum(),
                rows.stream().mapToDouble(HistoricalUsageDTO::getGridUsed).sum());
    }
}
