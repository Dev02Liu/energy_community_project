package com.energy_community_project.energy_producer.scheduling;

import com.energy_community_project.energy_producer.service.EnergyProducerService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class EnergyProducerScheduler {

    private final EnergyProducerService energyProducerService;
    private final Random random = new Random();

    public EnergyProducerScheduler(EnergyProducerService energyProducerService) {
        this.energyProducerService = energyProducerService;
    }

    @Scheduled(
            fixedDelayString = "${app.scheduling.fixed-delay-ms:1000}",
            initialDelayString = "${app.scheduling.initial-delay-ms:1000}"
    )
    public void sendProductionData() {
        try {
            Thread.sleep(random.nextInt(4000));
            energyProducerService.publishProductionData();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
