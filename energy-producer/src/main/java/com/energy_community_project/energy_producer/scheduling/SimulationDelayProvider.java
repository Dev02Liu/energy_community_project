package com.energy_community_project.energy_producer.scheduling;

public interface SimulationDelayProvider {

    void waitBeforeNextEvent() throws InterruptedException;
}
