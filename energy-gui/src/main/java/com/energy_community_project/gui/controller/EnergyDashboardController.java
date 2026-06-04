package com.energy_community_project.gui.controller;

import com.energy_community_project.gui.client.EnergyApiClient;
import com.energy_community_project.gui.dto.CurrentPercentageDTO;
import com.energy_community_project.gui.dto.HistoricalUsageDTO;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

public class EnergyDashboardController {

    private static final String ERROR_MESSAGE = "Error fetching data";

    private final EnergyApiClient apiClient;
    private final DecimalFormat numberFormat =
            new DecimalFormat("0.#####", new DecimalFormatSymbols(Locale.ENGLISH));

    private Label communityPoolLabel;
    private Label gridPortionLabel;
    private DatePicker startDate;
    private ComboBox<String> startHour;
    private DatePicker endDate;
    private ComboBox<String> endHour;
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

        LocalDate today = LocalDate.now();
        startDate = new DatePicker(today);
        startHour = hourComboBox("00:00");

        endDate = new DatePicker(today);
        endHour = hourComboBox(String.format("%02d:00", LocalDateTime.now().getHour()));

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
                new HBox(8, startDate, startHour),
                new Label("End"),
                new HBox(8, endDate, endHour),
                showDataButton,
                new Separator(),
                communityProducedLabel,
                communityUsedLabel,
                gridUsedLabel
        );
        root.setPadding(new Insets(16));
        return root;
    }

    // Dropdown with the 24 hours of a day (00:00 .. 23:00). The list is fixed in code, not loaded from the database.
    private ComboBox<String> hourComboBox(String selected) {
        ComboBox<String> box = new ComboBox<>();
        for (int hour = 0; hour < 24; hour++) {
            box.getItems().add(String.format("%02d:00", hour));
        }
        box.setValue(selected);
        return box;
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
        LocalDateTime start = toDateTime(startDate.getValue(), startHour.getValue());
        LocalDateTime end = toDateTime(endDate.getValue(), endHour.getValue());
        if (start == null || end == null) {
            showHistoricalError("Please select a start and end");
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

    // Combines the picked date and the selected "HH:00" hour into a LocalDateTime.
    static LocalDateTime toDateTime(LocalDate date, String hourLabel) {
        if (date == null || hourLabel == null) {
            return null;
        }
        int hour = Integer.parseInt(hourLabel.substring(0, 2));
        return date.atTime(hour, 0);
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
