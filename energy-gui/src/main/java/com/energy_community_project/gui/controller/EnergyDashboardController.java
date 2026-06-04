package com.energy_community_project.gui.controller;

import com.energy_community_project.gui.client.EnergyApiClient;
import com.energy_community_project.gui.dto.CurrentPercentageDTO;
import com.energy_community_project.gui.dto.HistoricalUsageDTO;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

public class EnergyDashboardController {

    private static final String ERROR_MESSAGE = "Error fetching data";

    private final EnergyApiClient apiClient;
    private final DecimalFormat numberFormat =
            new DecimalFormat("0.#####", new DecimalFormatSymbols(Locale.ENGLISH));

    private Label communityPoolLabel;
    private Label gridPortionLabel;
    private TextField startField;
    private TextField endField;
    private Label communityProducedLabel;
    private Label communityUsedLabel;
    private Label gridUsedLabel;

    public EnergyDashboardController(EnergyApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public Parent createView() {
        communityPoolLabel = new Label("Community Pool: -% used");
        gridPortionLabel = new Label("Grid Portion: -%");

        Button refreshButton = new Button("refresh");
        refreshButton.setOnAction(ignored -> loadCurrentPercentages());

        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        startField = new TextField(now.toLocalDate().atStartOfDay().toString());
        startField.setPromptText("Start (yyyy-MM-ddTHH:mm)");

        endField = new TextField(now.toString());
        endField.setPromptText("End (yyyy-MM-ddTHH:mm)");

        Button showDataButton = new Button("show data");
        showDataButton.setOnAction(ignored -> loadHistoricalUsage());

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
        return root;
    }

    public void loadCurrentPercentages() {
        apiClient.fetchCurrentPercentage()
                .thenAccept(dto -> Platform.runLater(() -> showCurrentPercentage(dto)))
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    showCurrentError(ERROR_MESSAGE);
                    return null;
                });
    }

    private void loadHistoricalUsage() {
        LocalDateTime start = parseGuiDate(startField.getText());
        LocalDateTime end = parseGuiDate(endField.getText());
        if (start == null || end == null) {
            showHistoricalError("Invalid date format (use yyyy-MM-ddTHH:mm)");
            return;
        }
        if (start.isAfter(end)) {
            showHistoricalError("Start must not be after end");
            return;
        }

        apiClient.fetchHistoricalUsage(start.toString(), end.toString())
                .thenAccept(entries -> Platform.runLater(() -> showHistoricalUsage(entries)))
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    showHistoricalError(ERROR_MESSAGE);
                    return null;
                });
    }

    static LocalDateTime parseGuiDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private void showCurrentPercentage(CurrentPercentageDTO dto) {
        communityPoolLabel.setText("Community Pool: " + format(dto.getCommunityDepleted()) + "% used");
        gridPortionLabel.setText("Grid Portion: " + format(dto.getGridPortion()) + "%");
    }

    private void showHistoricalUsage(List<HistoricalUsageDTO> entries) {
        if (entries == null || entries.isEmpty()) {
            showHistoricalError(ERROR_MESSAGE);
            return;
        }

        double producedSum = entries.stream().mapToDouble(HistoricalUsageDTO::getCommunityProduced).sum();
        double usedSum = entries.stream().mapToDouble(HistoricalUsageDTO::getCommunityUsed).sum();
        double gridUsedSum = entries.stream().mapToDouble(HistoricalUsageDTO::getGridUsed).sum();

        communityProducedLabel.setText("Community produced: " + format(producedSum) + " kWh");
        communityUsedLabel.setText("Community used: " + format(usedSum) + " kWh");
        gridUsedLabel.setText("Grid used: " + format(gridUsedSum) + " kWh");
    }

    private String format(double value) {
        return numberFormat.format(value);
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
