package com.energy_community_project.energy_user.scheduling;

import com.energy_community_project.energy_user.service.EnergyUserService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class EnergyUserScheduler {

    private final EnergyUserService energyUserService;
    private final SimulationDelayProvider simulationDelayProvider;

    public EnergyUserScheduler(EnergyUserService energyUserService,
                               SimulationDelayProvider simulationDelayProvider) {
        this.energyUserService = energyUserService;
        this.simulationDelayProvider = simulationDelayProvider;
    }

    /**
     * Drives message publishing at random ~1–5 s intervals: a 1 s fixed delay after the previous run
     * plus a random extra wait (0–4 s) before each send. Disabled in tests via {@code app.scheduling.enabled}.
     */
    @Scheduled(fixedDelayString = "${app.scheduling.fixed-delay-ms:1000}")
    public void sendUsageData() {
        try {
            simulationDelayProvider.waitBeforeNextEvent();
            energyUserService.publishUsageData();
        } catch (InterruptedException e) {
            // Restore the interrupt flag so the scheduler can shut down cleanly.
            Thread.currentThread().interrupt();
        }
    }
}
