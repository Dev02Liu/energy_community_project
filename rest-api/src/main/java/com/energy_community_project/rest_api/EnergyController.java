package com.energy_community_project.rest_api;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/energy")
public class EnergyController {

    private static final DateTimeFormatter GUI_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.GERMANY);

    private static final List<HistoricalUsageDTO> MOCK_HISTORICAL_DATA = List.of(
            new HistoricalUsageDTO("2025-01-10T12:00:00", 16.40, 15.90, 0.95),
            new HistoricalUsageDTO("2025-01-10T13:00:00", 17.10, 16.80, 1.10),
            new HistoricalUsageDTO("2025-01-10T14:00:00", 18.05, 18.05, 1.076),
            new HistoricalUsageDTO("2025-01-11T09:00:00", 14.20, 13.70, 1.35),
            new HistoricalUsageDTO("2025-01-11T10:00:00", 19.30, 18.40, 1.88),
            new HistoricalUsageDTO("2025-01-15T18:00:00", 12.60, 14.20, 2.95),
            new HistoricalUsageDTO("2025-01-20T14:00:00", 21.80, 20.90, 1.20),
            new HistoricalUsageDTO("2025-01-28T07:00:00", 10.40, 11.80, 3.10),
            new HistoricalUsageDTO("2025-02-01T14:00:00", 22.10, 21.50, 0.92),
            new HistoricalUsageDTO("2025-02-10T13:00:00", 20.90, 19.80, 1.42),
            new HistoricalUsageDTO("2025-02-10T14:00:00", 23.00, 22.10, 1.35)
    );

    // Endpunkt für die aktuelle Stunde
    @GetMapping("/current")
    public CurrentPercentageDTO getCurrentPercentage() {
        // Fester, nachvollziehbarer Mock-Wert passend zur letzten Datenstunde.
        return new CurrentPercentageDTO("2025-02-10T14:00:00", 92.30, 5.63);
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

        return MOCK_HISTORICAL_DATA.stream()
                .filter(item -> {
                    LocalDateTime itemHour = LocalDateTime.parse(item.getHour());
                    return !itemHour.isBefore(startDateTime) && !itemHour.isAfter(endDateTime);
                })
                .toList();
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