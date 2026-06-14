package com.energy_community_project.rest_api.controller;

import com.energy_community_project.rest_api.dto.CurrentPercentageDTO;
import com.energy_community_project.rest_api.dto.HistoricalSummaryDTO;
import com.energy_community_project.rest_api.service.EnergyReadService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/energy")
public class EnergyController {

    private final EnergyReadService energyReadService;

    public EnergyController(EnergyReadService energyReadService) {
        this.energyReadService = energyReadService;
    }

    @GetMapping("/current")
    public CurrentPercentageDTO getCurrentPercentage() {
        return energyReadService.getCurrentPercentage();
    }

    @GetMapping("/historical")
    public HistoricalSummaryDTO getHistoricalData(
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end) {
        return energyReadService.getHistoricalData(start, end);
    }
}
