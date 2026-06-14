package com.energy_community_project.energy_user.service;

import com.energy_community_project.energy_user.messaging.EnergyMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class EnergyUserService {

    private final RabbitTemplate rabbitTemplate;
    private final EnergyUsageCalculator usageCalculator;
    private final Random random = new Random();
    private final String queueName;
    private final String messageType;
    private final String association;
    private final double variationKwh;

    public EnergyUserService(RabbitTemplate rabbitTemplate,
                             EnergyUsageCalculator usageCalculator,
                             @Value("${app.queue.name}") String queueName,
                             @Value("${app.message.type}") String messageType,
                             @Value("${app.message.association}") String association,
                             @Value("${app.usage.variation-kwh}") double variationKwh) {
        this.rabbitTemplate = rabbitTemplate;
        this.usageCalculator = usageCalculator;
        this.queueName = queueName;
        this.messageType = messageType;
        this.association = association;
        this.variationKwh = variationKwh;
    }

    public void publishUsageData() {
        EnergyMessage msg = createUsageMessage();

        rabbitTemplate.convertAndSend(queueName, msg);
        System.out.println("Sent: " + msg.getType() + " - " + String.format("%.5f", msg.getKwh()) + " kWh");
    }

    public EnergyMessage createUsageMessage() {
        LocalDateTime now = LocalDateTime.now();
        double usedKwh = usageCalculator.calculateKwh(now, random.nextDouble() * variationKwh);

        EnergyMessage msg = new EnergyMessage();
        msg.setType(messageType);
        msg.setAssociation(association);
        msg.setKwh(usedKwh);
        msg.setDatetime(now);
        return msg;
    }
}
