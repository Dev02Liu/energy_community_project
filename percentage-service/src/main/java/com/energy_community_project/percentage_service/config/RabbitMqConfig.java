package com.energy_community_project.percentage_service.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public Queue percentageUpdateQueue(@Value("${app.update-queue.name}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    public JacksonJsonMessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
