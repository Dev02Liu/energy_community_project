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
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class EnergyApiClient {

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public EnergyApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public CompletableFuture<CurrentPercentageDTO> fetchCurrentPercentage() {
        HttpRequest request = newGetRequest(baseUrl + "/energy/current");
        return send(request, responseBody -> objectMapper.readValue(responseBody, CurrentPercentageDTO.class));
    }

    public CompletableFuture<List<HistoricalUsageDTO>> fetchHistoricalUsage(String start, String end) {
        String encodedStart = encode(start);
        String encodedEnd = encode(end);
        HttpRequest request = newGetRequest(baseUrl + "/energy/historical?start=" + encodedStart + "&end=" + encodedEnd);

        return send(request, responseBody -> objectMapper.readValue(
                responseBody,
                new TypeReference<List<HistoricalUsageDTO>>() {
                }
        ));
    }

    private HttpRequest newGetRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
    }

    private <T> CompletableFuture<T> send(HttpRequest request, ResponseMapper<T> mapper) {
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        throw new CompletionException(new EnergyApiException(
                                "REST API returned status " + response.statusCode()
                        ));
                    }

                    try {
                        return mapper.map(response.body());
                    } catch (IOException ex) {
                        throw new CompletionException(ex);
                    }
                });
    }

    private String encode(String value) {
        return URLEncoder.encode(value.trim(), StandardCharsets.UTF_8);
    }

    @FunctionalInterface
    private interface ResponseMapper<T> {
        T map(String responseBody) throws IOException;
    }
}
