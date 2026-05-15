package com.energy_community_project.energy_producer.service;

import com.energy_community_project.energy_producer.messaging.EnergyMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class EnergyProducerService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${app.queue.name}")
    private String queueName;

    private final Random random = new Random();

    // Runs periodically, we will introduce a random delay between 1-5 seconds inside the method
    @Scheduled(fixedDelay = 1000)
    public void sendProductionData() {
        try {
            // Random delay between 1 and 5 seconds to meet requirement "random 1-5 second intervals"
            Thread.sleep((long) (1000 + random.nextInt(4000)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Weather mockup (or simple implementation) to determine kWh
        // e.g. simulating sun/wind conditions randomly
        double weatherFactor = random.nextDouble(); // 0.0 to 1.0 (mocking weather condition)
        double producedKwh = 10.0 + (weatherFactor * 20.0); // 10 to 30 kWh based on "weather"

        EnergyMessage msg = new EnergyMessage();
        msg.setType("PRODUCER");
        msg.setAssociation("COMMUNITY");
        msg.setKwh(producedKwh);
        msg.setDatetime(LocalDateTime.now());

        rabbitTemplate.convertAndSend(queueName, msg);
        System.out.println("Sent: " + msg.getType() + " - " + String.format("%.2f", msg.getKwh()) + " kWh (Weather factor: " + String.format("%.2f", weatherFactor) + ")");
    }
}
