package com.energy_community_project.usage_service;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class UsageServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(UsageServiceApplication.class, args);
	}

	// Inbound queue this service consumes (durable: survives broker restarts).
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
