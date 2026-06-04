package com.energy_community_project.energy_producer.service;

import com.energy_community_project.energy_producer.messaging.EnergyMessage;
import com.energy_community_project.energy_producer.weather.WeatherClient;
import com.energy_community_project.energy_producer.weather.WeatherProductionCalculator;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EnergyProducerServiceTest {

    @Test
    void createsProducerMessageFromWeatherData() {
        WeatherClient weatherClient = mock(WeatherClient.class);
        when(weatherClient.currentSolarRadiation()).thenReturn(700.0);

        EnergyProducerService service = new EnergyProducerService(
                mock(RabbitTemplate.class),
                weatherClient,
                new WeatherProductionCalculator(0.001, 0.006),
                "energy_queue"
        );

        LocalDateTime before = LocalDateTime.now();
        EnergyMessage message = service.createProductionMessage();
        LocalDateTime after = LocalDateTime.now();

        assertThat(message.getType()).isEqualTo("PRODUCER");
        assertThat(message.getAssociation()).isEqualTo("COMMUNITY");
        assertThat(message.getKwh()).isBetween(0.001, 0.006);
        assertThat(message.getDatetime()).isBetween(before, after);
    }
}
