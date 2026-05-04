package com.energy_community_project.rest_api;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.energy_community_project.rest_api.entity.CurrentPercentageEntity;
import com.energy_community_project.rest_api.entity.HourlyUsageEntity;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/energy")
public class EnergyController {

    private static final DateTimeFormatter GUI_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.GERMANY);

    private final CurrentPercentageRepository currentPercentageRepository;
    private final HourlyUsageRepository hourlyUsageRepository;

    public EnergyController(CurrentPercentageRepository currentPercentageRepository, HourlyUsageRepository hourlyUsageRepository) {
        this.currentPercentageRepository = currentPercentageRepository;
        this.hourlyUsageRepository = hourlyUsageRepository;
    }

    // Endpunkt für die aktuelle Stunde
    @GetMapping("/current")
    public CurrentPercentageDTO getCurrentPercentage() {
        CurrentPercentageEntity latest = currentPercentageRepository.findFirstByOrderByHourDesc();
        if (latest == null) {
            return new CurrentPercentageDTO(LocalDateTime.now().toString(), 0.0, 0.0);
        }
        return new CurrentPercentageDTO(latest.getHour().toString(), latest.getCommunityDepleted(), latest.getGridPortion());
    }

    // Endpunkt für historische Daten mit Zeitfilter
    @GetMapping("/historical")
    public List<HistoricalUsageDTO> getHistoricalData(
            @RequestParam String start,
            @RequestParam String end) {
        LocalDateTime startDateTime = parseDateTime(start, "start");
        LocalDateTime endDateTime = parseDateTime(end, "end");

        if (startDateTime.isAfter(endDateTime)) {
            throw new ResponseStatusException(BAD_REQUEST, "start must be before or equal to end");
        }

        List<HourlyUsageEntity> entities = hourlyUsageRepository.findByHourBetween(startDateTime, endDateTime);
        return entities.stream().map(e -> new HistoricalUsageDTO(
                e.getHour().toString(),
                e.getCommunityProduced(),
                e.getCommunityUsed(),
                e.getGridUsed()
        )).collect(Collectors.toList());
    }

    private LocalDateTime parseDateTime(String value, String fieldName) {
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(value, GUI_FORMATTER);
            } catch (DateTimeParseException ex) {
                throw new ResponseStatusException(BAD_REQUEST, "Invalid " + fieldName + " format. Use ISO or dd.MM.yyyy HH:mm");
            }
        }
    }
}