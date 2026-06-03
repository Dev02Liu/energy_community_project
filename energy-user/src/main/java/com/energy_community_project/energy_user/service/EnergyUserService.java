package com.energy_community_project.energy_user.service;

import com.energy_community_project.energy_user.messaging.EnergyMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

/** Builds time-of-day-dependent USER messages and publishes them to {@code energy_queue} (Energy User, 10%). */
@Service
public class EnergyUserService {

    private final RabbitTemplate rabbitTemplate;
    private final EnergyUsageCalculator usageCalculator;
    private final UsageVariationProvider usageVariationProvider;
    private final Clock clock;
    private final String queueName;

    public EnergyUserService(RabbitTemplate rabbitTemplate,
                             EnergyUsageCalculator usageCalculator,
                             UsageVariationProvider usageVariationProvider,
                             Clock clock,
                             @Value("${app.queue.name}") String queueName) {
        this.rabbitTemplate = rabbitTemplate;
        this.usageCalculator = usageCalculator;
        this.usageVariationProvider = usageVariationProvider;
        this.clock = clock;
        this.queueName = queueName;
    }

    /** Creates one usage message and sends it to the queue (called on each scheduler tick). */
    public void publishUsageData() {
        EnergyMessage msg = createUsageMessage();

        rabbitTemplate.convertAndSend(queueName, msg);
        System.out.println("Sent: " + msg.getType() + " - " + String.format("%.5f", msg.getKwh()) + " kWh");
    }

    /** Derives the demanded kWh from the current time of day plus random variation, and assembles the event. */
    public EnergyMessage createUsageMessage() {
        LocalDateTime now = LocalDateTime.now(clock);
        double usedKwh = usageCalculator.calculateKwh(now, usageVariationProvider.nextVariationKwh());

        EnergyMessage msg = new EnergyMessage();
        msg.setType("USER");
        msg.setAssociation("COMMUNITY");
        msg.setKwh(usedKwh);
        msg.setDatetime(now);
        return msg;
    }
}
