package com.energy_community_project.energy_user.config;

import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Central RabbitMQ infrastructure: the JSON message converter used to publish energy messages. */
@Configuration
public class RabbitMqConfig {

    // Send messages as JSON instead of Java serialization.
    @Bean
    public JacksonJsonMessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
