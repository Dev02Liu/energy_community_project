package com.energy_community_project.energy_user.service;

import com.energy_community_project.energy_user.messaging.EnergyMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

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

    public void publishUsageData() {
        EnergyMessage msg = createUsageMessage();

        rabbitTemplate.convertAndSend(queueName, msg);
        System.out.println("Sent: " + msg.getType() + " - " + String.format("%.2f", msg.getKwh()) + " kWh");
    }

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
