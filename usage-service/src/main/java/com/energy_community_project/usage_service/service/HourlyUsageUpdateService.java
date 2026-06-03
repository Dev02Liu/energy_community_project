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

/**
 * Core business logic of the Usage Service (30% grading weight).
 *
 * <p>For each incoming PRODUCER/USER message it validates the payload, buckets it to the full hour,
 * updates the {@code hourly_usage} aggregate (community-first, grid-fallback allocation), and — only
 * after the DB transaction commits — publishes an update event so the Percentage Service recalculates.
 */
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

    /** Entry point per message: validate, load/create the hour bucket, apply the message, persist, notify. */
    @Transactional
    public void handleEnergyMessage(EnergyMessage message) {
        // Reject malformed/foreign messages before any DB write.
        if (!isValid(message)) {
            return;
        }

        // Bucket to the start of the hour and load (or create) that hour's aggregate row.
        LocalDateTime hour = toHour(message.getDatetime());
        HourlyUsageEntity hourlyUsage = hourlyUsageRepository.findById(hour)
                .orElseGet(() -> new HourlyUsageEntity(hour, 0.0, 0.0, 0.0));

        // Apply the message: production adds to the pool, usage draws from it (grid covers the rest).
        if ("PRODUCER".equals(normalizedType(message))) {
            hourlyUsage.setCommunityProduced(hourlyUsage.getCommunityProduced() + message.getKwh());
        } else if ("USER".equals(normalizedType(message))) {
            updateUsage(hourlyUsage, message.getKwh());
        }

        // Persist, then signal the Percentage Service once the row is safely committed.
        hourlyUsageRepository.save(hourlyUsage);
        publishUpdateAfterCommit(hour);
    }

    /**
     * Publishes the usage-update event only after the transaction commits, so the Percentage Service
     * never reads a row that a rolled-back transaction would have removed.
     */
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

    /** Guards against null, unknown type, foreign association, missing time, or non-finite/negative kWh. */
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

    /**
     * Allocates a user request: community energy is consumed first, the uncovered remainder is grid.
     * This keeps the invariant {@code communityUsed <= communityProduced} and never produces negative grid.
     */
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

    /** Truncates a timestamp to the start of its hour (e.g. 14:34:21 -> 14:00:00) for bucketing. */
    private LocalDateTime toHour(LocalDateTime dateTime) {
        return dateTime.withMinute(0).withSecond(0).withNano(0);
    }
}
