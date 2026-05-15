package com.energy_community_project.energy_producer.weather;

public class WeatherClientException extends RuntimeException {

    public WeatherClientException(String message) {
        super(message);
    }

    public WeatherClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
