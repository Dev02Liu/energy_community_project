package com.energy_community_project.percentage_service.messaging;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MessageContractTest {

    private static final String DOCUMENTED_USAGE_UPDATE_JSON = """
            {
              "hour": "2026-05-15T09:00:00"
            }
            """;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void documentedUsageUpdateJsonDeserializesToPercentageServiceMessage() throws Exception {
        HourlyUsageUpdatedMessage message = objectMapper.readValue(
                DOCUMENTED_USAGE_UPDATE_JSON,
                HourlyUsageUpdatedMessage.class
        );
        JsonNode root = objectMapper.readTree(DOCUMENTED_USAGE_UPDATE_JSON);

        assertThat(root.has("hour")).isTrue();
        assertThat(message.getHour()).isEqualTo(LocalDateTime.of(2026, 5, 15, 9, 0, 0));
    }
}
