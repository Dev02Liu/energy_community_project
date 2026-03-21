package com.energy_community_project.gui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.List;

public class MainApp {

    public static void main(String[] args) {
        Application.launch(EnergyGuiApplication.class, args);
    }

    public static class EnergyGuiApplication extends Application {

        private static final String BASE_URL = "http://localhost:8080";

        private final HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        private final ObjectMapper objectMapper = new ObjectMapper();
        private final DecimalFormat decimalFormat = new DecimalFormat("0.###");

        private Label communityPoolLabel;
        private Label gridPortionLabel;

        private TextField startField;
        private TextField endField;

        private Label communityProducedLabel;
        private Label communityUsedLabel;
        private Label gridUsedLabel;

        @Override
        public void start(Stage stage) {
            communityPoolLabel = new Label("Community Pool: -% used");
            gridPortionLabel = new Label("Grid Portion: -%");
            Button refreshButton = new Button("refresh");
            refreshButton.setOnAction(ignored -> fetchCurrentPercentages());

            startField = new TextField("10.01.2025 14:00");
            startField.setPromptText("Start (dd.MM.yyyy HH:mm)");

            endField = new TextField("10.02.2025 14:00");
            endField.setPromptText("End (dd.MM.yyyy HH:mm)");

            Button showDataButton = new Button("show data");
            showDataButton.setOnAction(ignored -> fetchHistoricalUsage());

            communityProducedLabel = new Label("Community produced: - kWh");
            communityUsedLabel = new Label("Community used: - kWh");
            gridUsedLabel = new Label("Grid used: - kWh");

            VBox root = new VBox(10,
                    communityPoolLabel,
                    gridPortionLabel,
                    refreshButton,
                    new Separator(),
                    new Label("Start"),
                    startField,
                    new Label("End"),
                    endField,
                    showDataButton,
                    new Separator(),
                    communityProducedLabel,
                    communityUsedLabel,
                    gridUsedLabel
            );
            root.setPadding(new Insets(16));

            stage.setTitle("Energy Community GUI");
            stage.setScene(new Scene(root, 420, 420));
            stage.show();

            fetchCurrentPercentages();
        }

        private void fetchCurrentPercentages() {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/energy/current"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() < 200 || response.statusCode() >= 300) {
                            showCurrentError("Error fetching data");
                            return;
                        }
                        try {
                            CurrentPercentageDTO dto = objectMapper.readValue(response.body(), CurrentPercentageDTO.class);
                            Platform.runLater(() -> {
                                communityPoolLabel.setText("Community Pool: " + decimalFormat.format(dto.communityDepleted) + "% used");
                                gridPortionLabel.setText("Grid Portion: " + decimalFormat.format(dto.gridPortion) + "%");
                            });
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            showCurrentError("Error fetching data");
                        }
                    })
                    .exceptionally(ex -> {
                        ex.printStackTrace();
                        showCurrentError("Error fetching data");
                        return null;
                    });
        }

        private void fetchHistoricalUsage() {
            String start = URLEncoder.encode(startField.getText().trim(), StandardCharsets.UTF_8);
            String end = URLEncoder.encode(endField.getText().trim(), StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/energy/historical?start=" + start + "&end=" + end))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() < 200 || response.statusCode() >= 300) {
                            showHistoricalError("Error fetching data");
                            return;
                        }
                        try {
                            List<HistoricalUsageDTO> entries = objectMapper.readValue(
                                    response.body(),
                                    new TypeReference<List<HistoricalUsageDTO>>() {
                                    }
                            );

                            if (entries == null || entries.isEmpty()) {
                                showHistoricalError("Error fetching data");
                                return;
                            }

                            double producedSum = entries.stream().mapToDouble(e -> e.communityProduced).sum();
                            double usedSum = entries.stream().mapToDouble(e -> e.communityUsed).sum();
                            double gridUsedSum = entries.stream().mapToDouble(e -> e.gridUsed).sum();

                            Platform.runLater(() -> {
                                communityProducedLabel.setText("Community produced: " + decimalFormat.format(producedSum) + " kWh");
                                communityUsedLabel.setText("Community used: " + decimalFormat.format(usedSum) + " kWh");
                                gridUsedLabel.setText("Grid used: " + decimalFormat.format(gridUsedSum) + " kWh");
                            });
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            showHistoricalError("Error fetching data");
                        }
                    })
                    .exceptionally(ex -> {
                        ex.printStackTrace();
                        showHistoricalError("Error fetching data");
                        return null;
                    });
        }

        private void showCurrentError(String message) {
            Platform.runLater(() -> {
                communityPoolLabel.setText("Community Pool: " + message);
                gridPortionLabel.setText("Grid Portion: " + message);
            });
        }

        private void showHistoricalError(String message) {
            Platform.runLater(() -> {
                communityProducedLabel.setText("Community produced: " + message);
                communityUsedLabel.setText("Community used: " + message);
                gridUsedLabel.setText("Grid used: " + message);
            });
        }
    }

    public static class CurrentPercentageDTO {
        public String hour;
        public double communityDepleted;
        public double gridPortion;
    }

    public static class HistoricalUsageDTO {
        public String hour;
        public double communityProduced;
        public double communityUsed;
        public double gridUsed;
    }
}