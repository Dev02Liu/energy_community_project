package com.energy_community_project.gui.client;

import com.energy_community_project.gui.dto.CurrentPercentageDTO;
import com.energy_community_project.gui.dto.HistoricalUsageDTO;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnergyApiClientTest {

    private HttpServer server;
    private EnergyApiClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        int port = server.getAddress().getPort();
        client = new EnergyApiClient("http://localhost:" + port);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void fetchCurrentPercentage_parsesJsonResponse() throws Exception {
        server.createContext("/energy/current", exchange -> {
            byte[] body = "{\"hour\":\"2025-01-10T14:00\",\"communityDepleted\":100.0,\"gridPortion\":5.63}".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        CurrentPercentageDTO dto = client.fetchCurrentPercentage().get();

        assertThat(dto.getCommunityDepleted()).isEqualTo(100.0);
        assertThat(dto.getGridPortion()).isEqualTo(5.63);
        assertThat(dto.getHour()).isEqualTo("2025-01-10T14:00");
    }

    @Test
    void fetchHistoricalUsage_encodesDateParametersCorrectly() throws Exception {
        AtomicReference<String> capturedQuery = new AtomicReference<>();

        server.createContext("/energy/historical", exchange -> {
            capturedQuery.set(exchange.getRequestURI().getRawQuery());
            byte[] body = "[]".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        client.fetchHistoricalUsage("2025-01-10T14:00", "2025-01-10T23:00").get();

        assertThat(capturedQuery.get())
                .contains("start=2025-01-10T14%3A00")
                .contains("end=2025-01-10T23%3A00");
    }

    @Test
    void fetchHistoricalUsage_parsesJsonArrayResponse() throws Exception {
        server.createContext("/energy/historical", exchange -> {
            byte[] body = ("[{\"hour\":\"2025-01-10T14:00\"," +
                    "\"communityProduced\":18.05,\"communityUsed\":18.05,\"gridUsed\":1.076}]").getBytes();
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        List<HistoricalUsageDTO> entries = client.fetchHistoricalUsage("2025-01-10T00:00:00", "2025-01-10T23:00:00").get();

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getCommunityProduced()).isEqualTo(18.05);
        assertThat(entries.get(0).getCommunityUsed()).isEqualTo(18.05);
        assertThat(entries.get(0).getGridUsed()).isEqualTo(1.076);
    }

    @Test
    void fetchHistoricalUsage_emptyArray_returnsEmptyList() throws Exception {
        server.createContext("/energy/historical", exchange -> {
            byte[] body = "[]".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        List<HistoricalUsageDTO> entries = client.fetchHistoricalUsage("2025-01-10T00:00:00", "2025-01-10T23:00:00").get();

        assertThat(entries).isEmpty();
    }

    @Test
    void fetchCurrentPercentage_serverReturns400_throwsRuntimeException() {
        server.createContext("/energy/current", exchange -> {
            exchange.sendResponseHeaders(400, -1);
            exchange.close();
        });

        assertThatThrownBy(() -> client.fetchCurrentPercentage().get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RuntimeException.class);
    }
}
