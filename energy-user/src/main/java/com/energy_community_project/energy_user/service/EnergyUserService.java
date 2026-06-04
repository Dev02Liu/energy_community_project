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

    public EnergyUserService(RabbitTemplate rabbitTemplate,
                             EnergyUsageCalculator usageCalculator,
                             @Value("${app.queue.name}") String queueName) {
        this.rabbitTemplate = rabbitTemplate;
        this.usageCalculator = usageCalculator;
        this.queueName = queueName;
    }

    public void publishUsageData() {
        EnergyMessage msg = createUsageMessage();

        rabbitTemplate.convertAndSend(queueName, msg);
        System.out.println("Sent: " + msg.getType() + " - " + String.format("%.5f", msg.getKwh()) + " kWh");
    }

    public EnergyMessage createUsageMessage() {
        LocalDateTime now = LocalDateTime.now();
        double usedKwh = usageCalculator.calculateKwh(now, random.nextDouble() * 0.002);

        EnergyMessage msg = new EnergyMessage();
        msg.setType("USER");
        msg.setAssociation("COMMUNITY");
        msg.setKwh(usedKwh);
        msg.setDatetime(now);
        return msg;
    }
}
