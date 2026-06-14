package com.energy_community_project.energy_producer.scheduling;

import com.energy_community_project.energy_producer.service.EnergyProducerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class EnergyProducerScheduler {

    private final EnergyProducerService energyProducerService;
    private final Random random = new Random();
    private final int maxRandomDelayMs;

    public EnergyProducerScheduler(EnergyProducerService energyProducerService,
                                   @Value("${app.scheduling.max-random-delay-ms}") int maxRandomDelayMs) {
        this.energyProducerService = energyProducerService;
        this.maxRandomDelayMs = maxRandomDelayMs;
    }

    @Scheduled(
            fixedDelayString = "${app.scheduling.fixed-delay-ms:1000}",
            initialDelayString = "${app.scheduling.initial-delay-ms:1000}"
    )
    public void sendProductionData() {
        try {
            Thread.sleep(random.nextInt(maxRandomDelayMs));
            energyProducerService.publishProductionData();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
