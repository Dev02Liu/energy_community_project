package com.energy_community_project.usage_service.listener;

import com.energy_community_project.shared.dto.EnergyMessage;
import com.energy_community_project.usage_service.service.HourlyUsageUpdateService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class EnergyMessageListener {

    private final HourlyUsageUpdateService hourlyUsageUpdateService;

    public EnergyMessageListener(HourlyUsageUpdateService hourlyUsageUpdateService) {
        this.hourlyUsageUpdateService = hourlyUsageUpdateService;
    }

    @RabbitListener(queues = "${app.queue.name}")
    public void receiveEnergyMessage(EnergyMessage message) {
        hourlyUsageUpdateService.handleEnergyMessage(message);
    }
}
