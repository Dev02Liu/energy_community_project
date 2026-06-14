package com.energy_community_project.energy_user.scheduling;

import com.energy_community_project.energy_user.service.EnergyUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class EnergyUserScheduler {

    private final EnergyUserService energyUserService;
    private final Random random = new Random();
    private final int maxRandomDelayMs;

    public EnergyUserScheduler(EnergyUserService energyUserService,
                               @Value("${app.scheduling.max-random-delay-ms}") int maxRandomDelayMs) {
        this.energyUserService = energyUserService;
        this.maxRandomDelayMs = maxRandomDelayMs;
    }

    @Scheduled(
            fixedDelayString = "${app.scheduling.fixed-delay-ms:1000}",
            initialDelayString = "${app.scheduling.initial-delay-ms:1000}"
    )
    public void sendUsageData() {
        try {
            Thread.sleep(random.nextInt(maxRandomDelayMs));
            energyUserService.publishUsageData();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
