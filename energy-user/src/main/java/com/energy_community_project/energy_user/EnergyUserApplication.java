package com.energy_community_project.energy_user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Bean;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;

import java.time.Clock;

@SpringBootApplication
@EnableScheduling
public class EnergyUserApplication {

	public static void main(String[] args) {
		SpringApplication.run(EnergyUserApplication.class, args);
	}

	@Bean
	public JacksonJsonMessageConverter jsonMessageConverter() {
		return new JacksonJsonMessageConverter();
	}

	@Bean
	public Clock clock() {
		return Clock.systemDefaultZone();
	}
}
