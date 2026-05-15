package com.energy_community_project.energy_producer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
// Note the change in the import name below:
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;

import java.time.Clock;

@SpringBootApplication
@EnableScheduling
public class EnergyProducerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnergyProducerApplication.class, args);
    }

    @Bean
    public JacksonJsonMessageConverter jsonMessageConverter() {
        // Renamed from Jackson2JsonMessageConverter
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
