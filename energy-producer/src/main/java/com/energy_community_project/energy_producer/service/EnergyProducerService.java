package com.energy_community_project.energy_producer.service;

import com.energy_community_project.energy_producer.messaging.EnergyMessage;
import com.energy_community_project.energy_producer.weather.WeatherClient;
import com.energy_community_project.energy_producer.weather.WeatherProductionCalculator;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class EnergyProducerService {

    private final RabbitTemplate rabbitTemplate;
    private final WeatherClient weatherClient;
    private final WeatherProductionCalculator productionCalculator;
    private final String queueName;
    private final String messageType;
    private final String association;

    public EnergyProducerService(RabbitTemplate rabbitTemplate,
                                 WeatherClient weatherClient,
                                 WeatherProductionCalculator productionCalculator,
                                 @Value("${app.queue.name}") String queueName,
                                 @Value("${app.message.type}") String messageType,
                                 @Value("${app.message.association}") String association) {
        this.rabbitTemplate = rabbitTemplate;
        this.weatherClient = weatherClient;
        this.productionCalculator = productionCalculator;
        this.queueName = queueName;
        this.messageType = messageType;
        this.association = association;
    }

    public void publishProductionData() {
        EnergyMessage msg = createProductionMessage();

        rabbitTemplate.convertAndSend(queueName, msg);
        System.out.println("Sent: " + msg.getType() + " - " + String.format("%.5f", msg.getKwh()) + " kWh");
    }

    public EnergyMessage createProductionMessage() {
        double solarRadiation = weatherClient.currentSolarRadiation();
        double producedKwh = productionCalculator.calculateKwh(solarRadiation);
        EnergyMessage msg = new EnergyMessage();
        msg.setType(messageType);
        msg.setAssociation(association);
        msg.setKwh(producedKwh);
        msg.setDatetime(LocalDateTime.now());
        return msg;
    }
}
