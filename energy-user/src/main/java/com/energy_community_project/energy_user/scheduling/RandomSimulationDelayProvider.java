package com.energy_community_project.energy_user.scheduling;

import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class RandomSimulationDelayProvider implements SimulationDelayProvider {

    private final Random random = new Random();

    @Override
    public void waitBeforeNextEvent() throws InterruptedException {
        Thread.sleep(1000L + random.nextInt(4000));
    }
}
