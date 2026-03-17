package com.energy_community_project.energy_producer.service;

import com.energy_community_project.shared.dto.EnergyMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
@Service
public class EnergyProducerService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${app.queue.name}")
    private String queueName;

    // This runs every 5 seconds
    @Scheduled(fixedRate = 5000)
    public void sendProductionData() {
        EnergyMessage msg = new EnergyMessage();
        msg.setType("PRODUCER");
        msg.setAssociation("COMMUNITY");
        msg.setKwh(Math.random() * 0.01); // Simple random for now
        msg.setDatetime(LocalDateTime.now());

        rabbitTemplate.convertAndSend(queueName, msg);
        System.out.println("Sent: " + msg.getType() + " - " + msg.getKwh() + " kWh");
    }
}