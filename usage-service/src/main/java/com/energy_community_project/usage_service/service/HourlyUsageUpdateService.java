package com.energy_community_project.usage_service.service;

import com.energy_community_project.usage_service.entity.HourlyUsageEntity;
import com.energy_community_project.usage_service.messaging.EnergyMessage;
import com.energy_community_project.usage_service.messaging.HourlyUsageUpdatedMessage;
import com.energy_community_project.usage_service.repository.HourlyUsageRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Set;

@Service
public class HourlyUsageUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HourlyUsageUpdateService.class);
    private static final String ASSOCIATION_COMMUNITY = "COMMUNITY";
    private static final Set<String> VALID_TYPES = Set.of("PRODUCER", "USER");

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
        if (!isValid(message)) {
            return;
        }

        LocalDateTime hour = toHour(message.getDatetime());
        HourlyUsageEntity hourlyUsage = hourlyUsageRepository.findById(hour)
                .orElseGet(() -> new HourlyUsageEntity(hour, 0.0, 0.0, 0.0));

        if ("PRODUCER".equals(normalizedType(message))) {
            hourlyUsage.setCommunityProduced(hourlyUsage.getCommunityProduced() + message.getKwh());
        } else if ("USER".equals(normalizedType(message))) {
            updateUsage(hourlyUsage, message.getKwh());
        }

        hourlyUsageRepository.save(hourlyUsage);
        publishUpdateAfterCommit(hour);
    }

    private void publishUpdateAfterCommit(LocalDateTime hour) {
        HourlyUsageUpdatedMessage updateMessage = new HourlyUsageUpdatedMessage(hour);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            rabbitTemplate.convertAndSend(updateQueueName, updateMessage);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                rabbitTemplate.convertAndSend(updateQueueName, updateMessage);
            }
        });
    }

    private boolean isValid(EnergyMessage message) {
        if (message == null) {
            LOGGER.warn("Ignoring null energy message");
            return false;
        }
        if (message.getType() == null || !VALID_TYPES.contains(normalizedType(message))) {
            LOGGER.warn("Ignoring energy message with invalid type: {}", message.getType());
            return false;
        }
        if (message.getAssociation() == null || !ASSOCIATION_COMMUNITY.equalsIgnoreCase(message.getAssociation())) {
            LOGGER.warn("Ignoring energy message with invalid association: {}", message.getAssociation());
            return false;
        }
        if (message.getDatetime() == null) {
            LOGGER.warn("Ignoring energy message with null datetime");
            return false;
        }
        if (!Double.isFinite(message.getKwh()) || message.getKwh() < 0.0) {
            LOGGER.warn("Ignoring energy message with invalid kWh value: {}", message.getKwh());
            return false;
        }
        return true;
    }

    private String normalizedType(EnergyMessage message) {
        return message.getType().trim().toUpperCase();
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
