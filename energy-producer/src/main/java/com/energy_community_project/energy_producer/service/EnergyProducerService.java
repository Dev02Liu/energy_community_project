package com.energy_community_project.energy_producer.service;

import com.energy_community_project.energy_producer.messaging.EnergyMessage;
import com.energy_community_project.energy_producer.weather.WeatherClient;
import com.energy_community_project.energy_producer.weather.WeatherProductionCalculator;
import com.energy_community_project.energy_producer.weather.WeatherSnapshot;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

/** Builds weather-dependent PRODUCER messages and publishes them to {@code energy_queue} (Energy Producer, 10%). */
@Service
public class EnergyProducerService {

    private final RabbitTemplate rabbitTemplate;
    private final WeatherClient weatherClient;
    private final WeatherProductionCalculator productionCalculator;
    private final Clock clock;
    private final String queueName;

    public EnergyProducerService(RabbitTemplate rabbitTemplate,
                                 WeatherClient weatherClient,
                                 WeatherProductionCalculator productionCalculator,
                                 Clock clock,
                                 @Value("${app.queue.name}") String queueName) {
        this.rabbitTemplate = rabbitTemplate;
        this.weatherClient = weatherClient;
        this.productionCalculator = productionCalculator;
        this.clock = clock;
        this.queueName = queueName;
    }

    /** Creates one production message and sends it to the queue (called on each scheduler tick). */
    public void publishProductionData() {
        EnergyMessage msg = createProductionMessage();

        rabbitTemplate.convertAndSend(queueName, msg);
        System.out.println("Sent: " + msg.getType() + " - " + String.format("%.5f", msg.getKwh()) + " kWh");
    }

    /** Reads current weather, derives the produced kWh from it, and assembles the COMMUNITY/PRODUCER event. */
    public EnergyMessage createProductionMessage() {
        WeatherSnapshot weather = weatherClient.currentWeather();
        double producedKwh = productionCalculator.calculateKwh(weather);
        EnergyMessage msg = new EnergyMessage();
        msg.setType("PRODUCER");
        msg.setAssociation("COMMUNITY");
        msg.setKwh(producedKwh);
        msg.setDatetime(LocalDateTime.now(clock));
        return msg;
    }
}
