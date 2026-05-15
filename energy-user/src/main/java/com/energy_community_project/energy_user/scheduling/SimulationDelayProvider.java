package com.energy_community_project.energy_user.scheduling;

public interface SimulationDelayProvider {

    void waitBeforeNextEvent() throws InterruptedException;
}
