package com.energy_community_project.percentage_service.listener;

import com.energy_community_project.percentage_service.service.CurrentPercentageCalculationService;
import com.energy_community_project.shared.dto.HourlyUsageUpdatedMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class HourlyUsageUpdatedListener {

    private final CurrentPercentageCalculationService currentPercentageCalculationService;

    public HourlyUsageUpdatedListener(CurrentPercentageCalculationService currentPercentageCalculationService) {
        this.currentPercentageCalculationService = currentPercentageCalculationService;
    }

    @RabbitListener(queues = "${app.update-queue.name}")
    public void receiveUpdate(HourlyUsageUpdatedMessage message) {
        if (message == null) {
            return;
        }
        currentPercentageCalculationService.updateCurrentPercentage(message.getHour());
    }
}
