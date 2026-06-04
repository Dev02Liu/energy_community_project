package com.energy_community_project.energy_producer.weather;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Reads the current solar radiation from the Open-Meteo weather API.
 * The more the sun shines, the higher the radiation - so the producer makes more energy.
 */
@Component
public class WeatherClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String url;

    public WeatherClient(@Value("${app.weather.latitude:48.2082}") double latitude,
                         @Value("${app.weather.longitude:16.3738}") double longitude) {
        this.url = "https://api.open-meteo.com/v1/forecast"
                + "?latitude=" + latitude
                + "&longitude=" + longitude
                + "&current=shortwave_radiation";
    }

    /** Current solar radiation in W/m2. High in bright sun, 0 at night. Returns 0 if the API cannot be reached. */
    public double currentSolarRadiation() {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode current = objectMapper.readTree(response.body()).path("current");
            return current.path("shortwave_radiation").asDouble(0.0);
        } catch (Exception e) {
            System.out.println("Weather API not reachable, using 0 W/m2: " + e.getMessage());
            return 0.0;
        }
    }
}
