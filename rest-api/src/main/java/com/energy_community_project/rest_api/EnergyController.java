package com.energy_community_project.rest_api;

import com.energy_community_project.rest_api.entity.CurrentPercentageEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/energy")
public class EnergyController {

    private final CurrentPercentageRepository currentPercentageRepository;
    private final HourlyUsageRepository hourlyUsageRepository;

    public EnergyController(CurrentPercentageRepository currentPercentageRepository,
                            HourlyUsageRepository hourlyUsageRepository) {
        this.currentPercentageRepository = currentPercentageRepository;
        this.hourlyUsageRepository = hourlyUsageRepository;
    }

    @GetMapping("/current")
    public CurrentPercentageDTO getCurrentPercentage() {
        CurrentPercentageEntity current = currentPercentageRepository.findFirstByOrderByHourDesc().orElse(null);
        if (current == null) {
            return new CurrentPercentageDTO(LocalDateTime.now().toString(), 0.0, 0.0);
        }
        return new CurrentPercentageDTO(current.getHour().toString(), current.getCommunityDepleted(), current.getGridPortion());
    }

    @GetMapping("/historical")
    public List<HistoricalUsageDTO> getHistoricalData(
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end) {
        return hourlyUsageRepository.findByHourBetween(start, end).stream()
                .map(entity -> new HistoricalUsageDTO(
                        entity.getHour().toString(),
                        entity.getCommunityProduced(),
                        entity.getCommunityUsed(),
                        entity.getGridUsed()))
                .toList();
    }
}
