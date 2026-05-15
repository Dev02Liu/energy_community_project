package com.energy_community_project.energy_producer.scheduling;

import com.energy_community_project.energy_producer.service.EnergyProducerService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class EnergyProducerScheduler {

    private final EnergyProducerService energyProducerService;
    private final SimulationDelayProvider simulationDelayProvider;

    public EnergyProducerScheduler(EnergyProducerService energyProducerService,
                                   SimulationDelayProvider simulationDelayProvider) {
        this.energyProducerService = energyProducerService;
        this.simulationDelayProvider = simulationDelayProvider;
    }

    @Scheduled(fixedDelayString = "${app.scheduling.fixed-delay-ms:1000}")
    public void sendProductionData() {
        try {
            simulationDelayProvider.waitBeforeNextEvent();
            energyProducerService.publishProductionData();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
