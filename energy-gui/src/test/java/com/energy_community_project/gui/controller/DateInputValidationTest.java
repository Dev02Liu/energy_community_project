package com.energy_community_project.gui.controller;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DateInputValidationTest {

    @Test
    void parseGuiDate_validGermanFormat_returnsLocalDateTime() {
        LocalDateTime result = EnergyDashboardController.parseGuiDate("10.01.2025 14:00");
        assertThat(result).isEqualTo(LocalDateTime.of(2025, 1, 10, 14, 0));
    }

    @Test
    void parseGuiDate_validGermanFormatMidnight_returnsLocalDateTime() {
        LocalDateTime result = EnergyDashboardController.parseGuiDate("15.05.2026 00:00");
        assertThat(result).isEqualTo(LocalDateTime.of(2026, 5, 15, 0, 0));
    }

    @Test
    void parseGuiDate_validIsoFormat_returnsLocalDateTime() {
        LocalDateTime result = EnergyDashboardController.parseGuiDate("2025-01-10T14:00:00");
        assertThat(result).isEqualTo(LocalDateTime.of(2025, 1, 10, 14, 0));
    }

    @Test
    void parseGuiDate_invalidFormat_returnsNull() {
        assertThat(EnergyDashboardController.parseGuiDate("not-a-date")).isNull();
    }

    @Test
    void parseGuiDate_nullInput_returnsNull() {
        assertThat(EnergyDashboardController.parseGuiDate(null)).isNull();
    }

    @Test
    void parseGuiDate_blankInput_returnsNull() {
        assertThat(EnergyDashboardController.parseGuiDate("   ")).isNull();
    }

    @Test
    void parseGuiDate_invalidDayOfMonth_returnsNull() {
        assertThat(EnergyDashboardController.parseGuiDate("31.02.2025 00:00")).isNull();
    }

    @Test
    void parseGuiDate_leadingAndTrailingSpacesAreTrimmed() {
        LocalDateTime result = EnergyDashboardController.parseGuiDate("  10.01.2025 14:00  ");
        assertThat(result).isEqualTo(LocalDateTime.of(2025, 1, 10, 14, 0));
    }
}
