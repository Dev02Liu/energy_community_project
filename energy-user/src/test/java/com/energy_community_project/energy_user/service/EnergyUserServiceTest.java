package com.energy_community_project.energy_user.service;

import com.energy_community_project.energy_user.messaging.EnergyMessage;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class EnergyUserServiceTest {

    @Test
    void createsContractCompliantUsageMessageWithFixedClockAndVariation() {
        Clock fixedClock = Clock.fixed(
                Instant.parse("2026-05-15T06:00:00Z"),
                ZoneId.of("UTC")
        );
        EnergyUserService service = new EnergyUserService(
                mock(RabbitTemplate.class),
                new EnergyUsageCalculator(),
                () -> 0.001,
                fixedClock,
                "energy_queue"
        );

        EnergyMessage message = service.createUsageMessage();

        assertThat(message.getType()).isEqualTo("USER");
        assertThat(message.getAssociation()).isEqualTo("COMMUNITY");
        assertThat(message.getKwh()).isEqualTo(0.002);
        assertThat(message.getDatetime()).isEqualTo(LocalDateTime.of(2026, 5, 15, 6, 0));
    }
}
