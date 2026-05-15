package com.energy_community_project.energy_producer.weather;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherProductionCalculatorTest {

    private final WeatherProductionCalculator calculator = new WeatherProductionCalculator(10.0, 30.0);

    @Test
    void sunnyHighRadiationWeatherProducesMoreKwhThanCloudyWeather() {
        WeatherSnapshot sunny = new WeatherSnapshot(5.0, true, 780.0);
        WeatherSnapshot cloudy = new WeatherSnapshot(95.0, true, 80.0);

        double sunnyKwh = calculator.calculateKwh(sunny);
        double cloudyKwh = calculator.calculateKwh(cloudy);

        assertThat(sunnyKwh).isGreaterThan(cloudyKwh);
    }

    @Test
    void nightWeatherProducesLowButNonNegativeKwh() {
        WeatherSnapshot night = new WeatherSnapshot(80.0, false, 0.0);

        double producedKwh = calculator.calculateKwh(night);

        assertThat(producedKwh).isBetween(10.0, 30.0);
    }
}
