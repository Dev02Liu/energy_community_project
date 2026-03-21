package com.energy_community_project.rest_api;

import org.springframework.web.bind.annotation.*;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/energy")
public class EnergyController {

    // Endpunkt für die aktuelle Stunde
    @GetMapping("/current")
    public CurrentPercentageDTO getCurrentPercentage() {
        // Mock-Daten basierend auf deinem CurrentPercentageDTO
        return new CurrentPercentageDTO("2025-01-10T14:00:00", 100.00, 5.63);
    }

    // Endpunkt für historische Daten mit Zeitfilter
    @GetMapping("/historical")
    public List<HistoricalUsageDTO> getHistoricalData(
            @RequestParam String start,
            @RequestParam String end) {

        // Mock-Liste mit Beispieldaten aus der Spezifikation
        return Arrays.asList(
                new HistoricalUsageDTO("2025-01-10T14:00:00", 18.05, 18.05, 1.076),
                new HistoricalUsageDTO("2025-01-10T13:00:00", 15.015, 14.033, 2.049),
                new HistoricalUsageDTO("2025-01-10T12:00:00", 20.00, 19.00, 1.00)

        );
    }
}