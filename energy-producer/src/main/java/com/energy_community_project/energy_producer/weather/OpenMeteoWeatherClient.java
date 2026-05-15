package com.energy_community_project.energy_producer.weather;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class OpenMeteoWeatherClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final double latitude;
    private final double longitude;

    public OpenMeteoWeatherClient(ObjectMapper objectMapper,
                                  @Value("${app.weather.open-meteo.base-url:https://api.open-meteo.com/v1/forecast}") String baseUrl,
                                  @Value("${app.weather.latitude:48.2082}") double latitude,
                                  @Value("${app.weather.longitude:16.3738}") double longitude) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public WeatherSnapshot currentWeather() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(buildUrl()))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new WeatherClientException("Weather API returned status " + response.statusCode());
            }
            return parse(response.body());
        } catch (IOException ex) {
            throw new WeatherClientException("Weather API request failed", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new WeatherClientException("Weather API request was interrupted", ex);
        }
    }

    private String buildUrl() {
        String currentFields = URLEncoder.encode("cloud_cover,is_day,shortwave_radiation", StandardCharsets.UTF_8);
        return baseUrl
                + "?latitude=" + latitude
                + "&longitude=" + longitude
                + "&current=" + currentFields
                + "&timezone=auto";
    }

    private WeatherSnapshot parse(String responseBody) throws IOException {
        JsonNode current = objectMapper.readTree(responseBody).path("current");
        if (current.isMissingNode()) {
            throw new WeatherClientException("Weather API response does not contain current weather data");
        }

        double cloudCover = current.path("cloud_cover").asDouble(50.0);
        boolean daylight = current.path("is_day").asInt(1) == 1;
        double radiation = current.path("shortwave_radiation").asDouble(0.0);

        return new WeatherSnapshot(cloudCover, daylight, radiation);
    }
}
