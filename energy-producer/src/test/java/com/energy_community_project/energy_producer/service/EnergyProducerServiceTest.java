package com.energy_community_project.energy_producer.service;

import com.energy_community_project.energy_producer.messaging.EnergyMessage;
import com.energy_community_project.energy_producer.weather.WeatherClient;
import com.energy_community_project.energy_producer.weather.WeatherProductionCalculator;
import com.energy_community_project.energy_producer.weather.WeatherSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EnergyProducerServiceTest {

    @Test
    void createsProducerMessageFromWeatherData() {
        WeatherClient weatherClient = mock(WeatherClient.class);
        when(weatherClient.currentWeather()).thenReturn(new WeatherSnapshot(10.0, true, 700.0));

        Clock fixedClock = Clock.fixed(Instant.parse("2026-05-15T12:33:00Z"), ZoneId.of("UTC"));
        EnergyProducerService service = new EnergyProducerService(
                mock(RabbitTemplate.class),
                weatherClient,
                new WeatherProductionCalculator(10.0, 30.0),
                fixedClock,
                "energy_queue"
        );

        EnergyMessage message = service.createProductionMessage();

        assertThat(message.getType()).isEqualTo("PRODUCER");
        assertThat(message.getAssociation()).isEqualTo("COMMUNITY");
        assertThat(message.getKwh()).isGreaterThan(10.0);
        assertThat(message.getDatetime()).isEqualTo(LocalDateTime.of(2026, 5, 15, 12, 33));
    }
}
