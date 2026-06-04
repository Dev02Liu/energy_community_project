package com.energy_community_project.percentage_service;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class PercentageServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PercentageServiceApplication.class, args);
	}

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
