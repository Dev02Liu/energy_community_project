package com.energy_community_project.usage_service.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Declares the durable queues this service touches and the JSON (de)serializer for message payloads. */
@Configuration
public class RabbitMqConfig {

    // Inbound queue consumed by this service (durable: survives broker restarts).
    @Bean
    public Queue energyQueue(@Value("${app.queue.name}") String queueName) {
        return new Queue(queueName, true);
    }

    // Outbound queue this service publishes usage updates to.
    @Bean
    public Queue percentageUpdateQueue(@Value("${app.update-queue.name}") String queueName) {
        return new Queue(queueName, true);
    }

    // Send/receive messages as JSON instead of Java serialization.
    @Bean
    public JacksonJsonMessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
