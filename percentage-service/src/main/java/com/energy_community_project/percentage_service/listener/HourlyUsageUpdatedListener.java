package com.energy_community_project.percentage_service.listener;

import com.energy_community_project.percentage_service.messaging.HourlyUsageUpdatedMessage;
import com.energy_community_project.percentage_service.service.CurrentPercentageCalculationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** RabbitMQ inbound boundary: receives usage-update events from {@code percentage_update_queue} and triggers recalculation. */
@Component
public class HourlyUsageUpdatedListener {

    private final CurrentPercentageCalculationService currentPercentageCalculationService;

    public HourlyUsageUpdatedListener(CurrentPercentageCalculationService currentPercentageCalculationService) {
        this.currentPercentageCalculationService = currentPercentageCalculationService;
    }

    @RabbitListener(queues = "${app.update-queue.name}")
    public void receiveUpdate(HourlyUsageUpdatedMessage message) {
        currentPercentageCalculationService.updateCurrentPercentage(message);
    }
}
