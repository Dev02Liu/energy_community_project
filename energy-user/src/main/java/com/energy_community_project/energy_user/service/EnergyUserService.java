package com.energy_community_project.energy_user.service;

import com.energy_community_project.shared.dto.EnergyMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class EnergyUserService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${app.queue.name}")
    private String queueName;

    private final Random random = new Random();

    @Scheduled(fixedDelay = 1000)
    public void sendUsageData() {
        try {
            // Random delay between 1 and 5 seconds
            Thread.sleep((long) (1000 + random.nextInt(4000)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        
        // Determine usage based on time of day (Peak morning 7-9, Peak evening 18-21)
        double baseUsage = 2.0; 
        double peakMultiplier = 1.0;

        if ((hour >= 7 && hour <= 9) || (hour >= 18 && hour <= 21)) {
            peakMultiplier = 3.0; // Peak times
        } else if (hour >= 23 || hour <= 5) {
            peakMultiplier = 0.5; // Night times (low usage)
        }

        double usedKwh = (baseUsage + random.nextDouble() * 3.0) * peakMultiplier;

        EnergyMessage msg = new EnergyMessage();
        msg.setType("USER");
        msg.setAssociation("COMMUNITY");
        msg.setKwh(usedKwh);
        msg.setDatetime(now);

        rabbitTemplate.convertAndSend(queueName, msg);
        System.out.println("Sent: " + msg.getType() + " - " + String.format("%.2f", msg.getKwh()) + " kWh");
    }
}
