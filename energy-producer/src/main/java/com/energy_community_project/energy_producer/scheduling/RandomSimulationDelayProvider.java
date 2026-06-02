package com.energy_community_project.energy_producer.scheduling;

import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class RandomSimulationDelayProvider implements SimulationDelayProvider {

    private final Random random = new Random();

    @Override
    public void waitBeforeNextEvent() throws InterruptedException {
        Thread.sleep(random.nextInt(4000));
    }
}
