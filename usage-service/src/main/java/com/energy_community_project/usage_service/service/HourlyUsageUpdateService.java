package com.energy_community_project.usage_service.service;

import com.energy_community_project.usage_service.entity.HourlyUsageEntity;
import com.energy_community_project.usage_service.messaging.EnergyMessage;
import com.energy_community_project.usage_service.messaging.HourlyUsageUpdatedMessage;
import com.energy_community_project.usage_service.repository.HourlyUsageRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class HourlyUsageUpdateService {

    private final HourlyUsageRepository hourlyUsageRepository;
    private final RabbitTemplate rabbitTemplate;
    private final String updateQueueName;

    public HourlyUsageUpdateService(HourlyUsageRepository hourlyUsageRepository,
                                    RabbitTemplate rabbitTemplate,
                                    @Value("${app.update-queue.name}") String updateQueueName) {
        this.hourlyUsageRepository = hourlyUsageRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.updateQueueName = updateQueueName;
    }

    @Transactional
    public void handleEnergyMessage(EnergyMessage message) {
        if (message == null || message.getDatetime() == null || message.getType() == null) {
            return;
        }

        LocalDateTime hour = toHour(message.getDatetime());
        HourlyUsageEntity hourlyUsage = hourlyUsageRepository.findById(hour)
                .orElseGet(() -> new HourlyUsageEntity(hour, 0.0, 0.0, 0.0));

        if ("PRODUCER".equalsIgnoreCase(message.getType())) {
            hourlyUsage.setCommunityProduced(hourlyUsage.getCommunityProduced() + message.getKwh());
        } else if ("USER".equalsIgnoreCase(message.getType())) {
            updateUsage(hourlyUsage, message.getKwh());
        }

        hourlyUsageRepository.save(hourlyUsage);
        rabbitTemplate.convertAndSend(updateQueueName, new HourlyUsageUpdatedMessage(hour));
    }

    private void updateUsage(HourlyUsageEntity hourlyUsage, double requestedKwh) {
        double availableCommunityEnergy = Math.max(
                hourlyUsage.getCommunityProduced() - hourlyUsage.getCommunityUsed(),
                0.0
        );

        double communityPortion = Math.min(requestedKwh, availableCommunityEnergy);
        double gridPortion = Math.max(requestedKwh - communityPortion, 0.0);

        hourlyUsage.setCommunityUsed(hourlyUsage.getCommunityUsed() + communityPortion);
        hourlyUsage.setGridUsed(hourlyUsage.getGridUsed() + gridPortion);
    }

    private LocalDateTime toHour(LocalDateTime dateTime) {
        return dateTime.withMinute(0).withSecond(0).withNano(0);
    }
}
