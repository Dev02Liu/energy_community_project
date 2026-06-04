package com.energy_community_project.energy_producer.weather;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherProductionCalculatorTest {

    private final WeatherProductionCalculator calculator = new WeatherProductionCalculator(0.001, 0.006);

    @Test
    void sunnyWeatherProducesMoreKwhThanCloudyWeather() {
        double sunnyKwh = calculator.calculateKwh(780.0);
        double cloudyKwh = calculator.calculateKwh(80.0);

        assertThat(sunnyKwh).isGreaterThan(cloudyKwh);
    }

    @Test
    void nightProducesLowButNonNegativeKwh() {
        double producedKwh = calculator.calculateKwh(0.0);

        assertThat(producedKwh).isBetween(0.001, 0.006);
    }

    @Test
    void resultStaysWithinConfiguredBounds() {
        for (int i = 0; i < 20; i++) {
            double producedKwh = calculator.calculateKwh(400.0);
            assertThat(producedKwh).isBetween(0.001, 0.006);
        }
    }
}
