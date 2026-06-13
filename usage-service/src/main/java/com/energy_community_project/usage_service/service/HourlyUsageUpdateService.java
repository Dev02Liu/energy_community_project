package com.energy_community_project.usage_service.service;

import com.energy_community_project.usage_service.entity.HourlyUsageEntity;
import com.energy_community_project.usage_service.messaging.EnergyMessage;
import com.energy_community_project.usage_service.messaging.HourlyUsageUpdatedMessage;
import com.energy_community_project.usage_service.repository.HourlyUsageRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    public void handleEnergyMessage(EnergyMessage message) {
        LocalDateTime hour = toHour(message.getDatetime());
        HourlyUsageEntity hourlyUsage = hourlyUsageRepository.findById(hour)
                .orElseGet(() -> new HourlyUsageEntity(hour, 0.0, 0.0, 0.0));

        if ("PRODUCER".equals(message.getType())) {
            hourlyUsage.setCommunityProduced(hourlyUsage.getCommunityProduced() + message.getKwh());
        } else {
            addUsage(hourlyUsage, message.getKwh());
        }

        hourlyUsageRepository.save(hourlyUsage);
        // The update message carries the full hourly snapshot so the percentage service
        // never has to read the hourly_usage table itself.
        rabbitTemplate.convertAndSend(updateQueueName, new HourlyUsageUpdatedMessage(
                hour,
                hourlyUsage.getCommunityProduced(),
                hourlyUsage.getCommunityUsed(),
                hourlyUsage.getGridUsed()));
    }

    // Community energy is used first; only what the community pool cannot cover is taken from the grid.
    private void addUsage(HourlyUsageEntity hourlyUsage, double usedKwh) {
        double availableCommunity = hourlyUsage.getCommunityProduced() - hourlyUsage.getCommunityUsed();
        double communityPart = Math.min(usedKwh, Math.max(availableCommunity, 0.0));
        double gridPart = usedKwh - communityPart;

        hourlyUsage.setCommunityUsed(hourlyUsage.getCommunityUsed() + communityPart);
        hourlyUsage.setGridUsed(hourlyUsage.getGridUsed() + gridPart);
    }

    private LocalDateTime toHour(LocalDateTime dateTime) {
        return dateTime.withMinute(0).withSecond(0).withNano(0);
    }
}
