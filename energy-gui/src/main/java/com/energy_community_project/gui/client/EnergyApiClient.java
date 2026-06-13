package com.energy_community_project.gui.client;

import com.energy_community_project.gui.dto.CurrentPercentageDTO;
import com.energy_community_project.gui.dto.HistoricalUsageDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Calls the REST API and parses the JSON responses into DTOs. Requests run asynchronously so the GUI stays responsive. */
public class EnergyApiClient {

    // REST configuration and reusable JSON/HTTP helpers for all API calls.
    private final String baseUrl;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Stores the REST API base URL supplied by the GUI application.
    public EnergyApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    // Requests the latest current percentage values and maps the JSON object to a DTO.
    public CompletableFuture<CurrentPercentageDTO> fetchCurrentPercentage() {
        return get("/energy/current").thenApply(body -> {
            try {
                return objectMapper.readValue(body, CurrentPercentageDTO.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    // Requests historical usage rows for the selected date range and maps the JSON array to DTOs.
    public CompletableFuture<List<HistoricalUsageDTO>> fetchHistoricalUsage(String start, String end) {
        return get("/energy/historical?start=" + encode(start) + "&end=" + encode(end)).thenApply(body -> {
            try {
                return objectMapper.readValue(body, new TypeReference<List<HistoricalUsageDTO>>() {});
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    // Executes an asynchronous HTTP GET request and returns the response body for successful calls.
    private CompletableFuture<String> get(String path) {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(baseUrl + path)).GET().build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("REST API returned status " + response.statusCode());
                    }
                    return response.body();
                });
    }

    // Encodes query parameter values so date-time strings are safe inside the request URL.
    private String encode(String value) {
        return URLEncoder.encode(value.trim(), StandardCharsets.UTF_8);
    }
}
