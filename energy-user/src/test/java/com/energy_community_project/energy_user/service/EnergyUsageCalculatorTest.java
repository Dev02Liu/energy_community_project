package com.energy_community_project.energy_user.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EnergyUsageCalculatorTest {

    private final EnergyUsageCalculator calculator = new EnergyUsageCalculator();

    @Test
    void morningPeakUsageIsHigherThanNightUsageForSameVariation() {
        double fixedVariationKwh = 1.0;

        double morningUsage = calculator.calculateKwh(
                LocalDateTime.of(2026, 5, 15, 8, 0),
                fixedVariationKwh
        );
        double nightUsage = calculator.calculateKwh(
                LocalDateTime.of(2026, 5, 15, 2, 0),
                fixedVariationKwh
        );

        assertThat(morningUsage).isGreaterThan(nightUsage);
    }

    @Test
    void eveningPeakUsageIsHigherThanDaytimeUsageForSameVariation() {
        double fixedVariationKwh = 1.0;

        double eveningUsage = calculator.calculateKwh(
                LocalDateTime.of(2026, 5, 15, 19, 0),
                fixedVariationKwh
        );
        double daytimeUsage = calculator.calculateKwh(
                LocalDateTime.of(2026, 5, 15, 14, 0),
                fixedVariationKwh
        );

        assertThat(eveningUsage).isGreaterThan(daytimeUsage);
    }
}
