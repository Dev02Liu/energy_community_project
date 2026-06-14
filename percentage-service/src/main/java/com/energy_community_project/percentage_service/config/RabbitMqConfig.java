package com.energy_community_project.percentage_service.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Central RabbitMQ infrastructure: the inbound queue and the JSON message converter. */
@Configuration
public class RabbitMqConfig {

    // Inbound queue of usage-update events (durable: survives broker restarts).
    @Bean
    public Queue percentageUpdateQueue(@Value("${app.update-queue.name}") String queueName) {
        return new Queue(queueName, true);
    }

    // Receive messages as JSON instead of Java serialization.
    @Bean
    public JacksonJsonMessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
