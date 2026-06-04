package com.energy_community_project.gui.controller;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DateSelectionTest {

    @Test
    void toDateTime_combinesDateAndHour() {
        LocalDateTime result = EnergyDashboardController.toDateTime(LocalDate.of(2025, 1, 10), "14:00");
        assertThat(result).isEqualTo(LocalDateTime.of(2025, 1, 10, 14, 0));
    }

    @Test
    void toDateTime_midnightHour() {
        LocalDateTime result = EnergyDashboardController.toDateTime(LocalDate.of(2025, 1, 10), "00:00");
        assertThat(result).isEqualTo(LocalDateTime.of(2025, 1, 10, 0, 0));
    }

    @Test
    void toDateTime_nullDate_returnsNull() {
        assertThat(EnergyDashboardController.toDateTime(null, "14:00")).isNull();
    }

    @Test
    void toDateTime_nullHour_returnsNull() {
        assertThat(EnergyDashboardController.toDateTime(LocalDate.of(2025, 1, 10), null)).isNull();
    }
}
