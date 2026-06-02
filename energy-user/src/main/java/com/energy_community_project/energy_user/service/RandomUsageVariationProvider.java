package com.energy_community_project.energy_user.service;

import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class RandomUsageVariationProvider implements UsageVariationProvider {

    private final Random random = new Random();

    @Override
    public double nextVariationKwh() {
        return random.nextDouble() * 0.002;
    }
}
