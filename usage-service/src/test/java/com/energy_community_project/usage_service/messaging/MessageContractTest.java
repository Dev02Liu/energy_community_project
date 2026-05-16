package com.energy_community_project.usage_service.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MessageContractTest {

    private static final String DOCUMENTED_PRODUCER_JSON = """
            {
              "type": "PRODUCER",
              "association": "COMMUNITY",
              "kwh": 18.7,
              "datetime": "2026-05-15T14:33:00"
            }
            """;

    private static final String DOCUMENTED_USER_JSON = """
            {
              "type": "USER",
              "association": "COMMUNITY",
              "kwh": 5.0,
              "datetime": "2026-05-15T14:34:00"
            }
            """;

    private final JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void documentedProducerJsonDeserializesToUsageServiceEnergyMessage() throws Exception {
        EnergyMessage message = objectMapper.readValue(DOCUMENTED_PRODUCER_JSON, EnergyMessage.class);
        JsonNode root = objectMapper.readTree(DOCUMENTED_PRODUCER_JSON);

        assertEnergyMessageFieldsExist(root);
        assertThat(message.getType()).isEqualTo("PRODUCER");
        assertThat(message.getAssociation()).isEqualTo("COMMUNITY");
        assertThat(message.getKwh()).isEqualTo(18.7);
        assertThat(message.getDatetime()).isEqualTo(LocalDateTime.of(2026, 5, 15, 14, 33, 0));
    }

    @Test
    void documentedUserJsonDeserializesToUsageServiceEnergyMessage() throws Exception {
        EnergyMessage message = objectMapper.readValue(DOCUMENTED_USER_JSON, EnergyMessage.class);
        JsonNode root = objectMapper.readTree(DOCUMENTED_USER_JSON);

        assertEnergyMessageFieldsExist(root);
        assertThat(message.getType()).isEqualTo("USER");
        assertThat(message.getAssociation()).isEqualTo("COMMUNITY");
        assertThat(message.getKwh()).isEqualTo(5.0);
        assertThat(message.getDatetime()).isEqualTo(LocalDateTime.of(2026, 5, 15, 14, 34, 0));
    }

    @Test
    void usageUpdateSerializesToDocumentedPercentageUpdateContract() throws Exception {
        HourlyUsageUpdatedMessage update = new HourlyUsageUpdatedMessage(LocalDateTime.of(2026, 5, 15, 9, 0, 0));

        String json = new String(converter.toMessage(update, new MessageProperties()).getBody(), StandardCharsets.UTF_8);
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.has("hour")).isTrue();
        assertThat(root.get("hour").asString()).isEqualTo("2026-05-15T09:00:00");
    }

    private void assertEnergyMessageFieldsExist(JsonNode root) {
        assertThat(root.has("type")).isTrue();
        assertThat(root.has("association")).isTrue();
        assertThat(root.has("kwh")).isTrue();
        assertThat(root.has("datetime")).isTrue();
    }
}
