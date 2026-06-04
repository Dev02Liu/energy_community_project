package com.energy_community_project.energy_user.service;

import com.energy_community_project.energy_user.messaging.EnergyMessage;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class EnergyUserServiceTest {

    @Test
    void createsContractCompliantUsageMessage() {
        EnergyUserService service = new EnergyUserService(
                mock(RabbitTemplate.class),
                new EnergyUsageCalculator(),
                "energy_queue"
        );

        LocalDateTime before = LocalDateTime.now();
        EnergyMessage message = service.createUsageMessage();
        LocalDateTime after = LocalDateTime.now();

        assertThat(message.getType()).isEqualTo("USER");
        assertThat(message.getAssociation()).isEqualTo("COMMUNITY");
        assertThat(message.getKwh()).isBetween(0.0005, 0.009);
        assertThat(message.getDatetime()).isBetween(before, after);
    }
}
