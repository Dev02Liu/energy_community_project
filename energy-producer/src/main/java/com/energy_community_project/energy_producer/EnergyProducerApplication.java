package com.energy_community_project.energy_producer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EnergyProducerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnergyProducerApplication.class, args);
    }
}
