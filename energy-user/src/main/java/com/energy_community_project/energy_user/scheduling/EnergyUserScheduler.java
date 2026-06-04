package com.energy_community_project.energy_user.scheduling;

import com.energy_community_project.energy_user.service.EnergyUserService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class EnergyUserScheduler {

    private final EnergyUserService energyUserService;
    private final Random random = new Random();

    public EnergyUserScheduler(EnergyUserService energyUserService) {
        this.energyUserService = energyUserService;
    }

    @Scheduled(
            fixedDelayString = "${app.scheduling.fixed-delay-ms:1000}",
            initialDelayString = "${app.scheduling.initial-delay-ms:1000}"
    )
    public void sendUsageData() {
        try {
            Thread.sleep(random.nextInt(4000));
            energyUserService.publishUsageData();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
