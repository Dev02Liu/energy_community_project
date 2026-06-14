package com.energy_community_project.percentage_service.listener;

import com.energy_community_project.percentage_service.messaging.HourlyUsageUpdatedMessage;
import com.energy_community_project.percentage_service.service.CurrentPercentageCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class HourlyUsageUpdatedListener {

    private static final Logger log = LoggerFactory.getLogger(HourlyUsageUpdatedListener.class);

    private final CurrentPercentageCalculationService currentPercentageCalculationService;

    public HourlyUsageUpdatedListener(CurrentPercentageCalculationService currentPercentageCalculationService) {
        this.currentPercentageCalculationService = currentPercentageCalculationService;
    }

    @RabbitListener(queues = "${app.update-queue.name}")
    public void receiveUpdate(HourlyUsageUpdatedMessage message) {
        try {
            currentPercentageCalculationService.updateCurrentPercentage(message);
        } catch (Exception e) {
            log.error("Failed to process usage update for hour {}: {}", message.getHour(), e.getMessage(), e);
        }
    }
}
