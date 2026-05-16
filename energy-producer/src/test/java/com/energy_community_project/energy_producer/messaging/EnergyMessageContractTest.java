package com.energy_community_project.energy_producer.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EnergyMessageContractTest {

    private final JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void producerMessageSerializesToDocumentedEnergyMessageJsonContract() throws Exception {
        EnergyMessage message = new EnergyMessage();
        message.setType("PRODUCER");
        message.setAssociation("COMMUNITY");
        message.setKwh(18.7);
        message.setDatetime(LocalDateTime.of(2026, 5, 15, 14, 33, 0));

        String json = new String(converter.toMessage(message, new MessageProperties()).getBody(), StandardCharsets.UTF_8);
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.has("type")).isTrue();
        assertThat(root.has("association")).isTrue();
        assertThat(root.has("kwh")).isTrue();
        assertThat(root.has("datetime")).isTrue();
        assertThat(root.get("type").asString()).isEqualTo("PRODUCER");
        assertThat(root.get("association").asString()).isEqualTo("COMMUNITY");
        assertThat(root.get("kwh").asDouble()).isEqualTo(18.7);
        assertThat(root.get("datetime").asString()).isEqualTo("2026-05-15T14:33:00");
    }
}
