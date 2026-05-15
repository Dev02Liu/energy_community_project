package com.energy_community_project.energy_producer.weather;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ResilientWeatherClientTest {

    @Test
    void fallsBackWhenExternalWeatherApiFails() {
        OpenMeteoWeatherClient openMeteoWeatherClient = mock(OpenMeteoWeatherClient.class);
        FallbackWeatherClient fallbackWeatherClient = mock(FallbackWeatherClient.class);
        WeatherSnapshot fallbackWeather = new WeatherSnapshot(35.0, true, 520.0);

        when(openMeteoWeatherClient.currentWeather()).thenThrow(new WeatherClientException("offline"));
        when(fallbackWeatherClient.currentWeather()).thenReturn(fallbackWeather);

        ResilientWeatherClient client = new ResilientWeatherClient(openMeteoWeatherClient, fallbackWeatherClient, "open-meteo");

        WeatherSnapshot weather = client.currentWeather();

        assertThat(weather).isEqualTo(fallbackWeather);
    }

    @Test
    void fallbackModeDoesNotCallExternalWeatherApi() {
        OpenMeteoWeatherClient openMeteoWeatherClient = mock(OpenMeteoWeatherClient.class);
        FallbackWeatherClient fallbackWeatherClient = mock(FallbackWeatherClient.class);
        WeatherSnapshot fallbackWeather = new WeatherSnapshot(40.0, true, 480.0);

        when(fallbackWeatherClient.currentWeather()).thenReturn(fallbackWeather);

        ResilientWeatherClient client = new ResilientWeatherClient(openMeteoWeatherClient, fallbackWeatherClient, "fallback");

        WeatherSnapshot weather = client.currentWeather();

        assertThat(weather).isEqualTo(fallbackWeather);
        verifyNoInteractions(openMeteoWeatherClient);
    }
}
