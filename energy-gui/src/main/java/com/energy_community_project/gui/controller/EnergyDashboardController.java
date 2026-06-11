package com.energy_community_project.gui.controller;

import com.energy_community_project.gui.client.EnergyApiClient;
import com.energy_community_project.gui.dto.CurrentPercentageDTO;
import com.energy_community_project.gui.dto.HistoricalUsageDTO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;

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

    @FXML private Label communityPoolLabel;
    @FXML private Label gridPortionLabel;
    @FXML private DatePicker startDate;
    @FXML private ComboBox<String> startHour;
    @FXML private DatePicker endDate;
    @FXML private ComboBox<String> endHour;
    @FXML private Label communityProducedLabel;
    @FXML private Label communityUsedLabel;
    @FXML private Label gridUsedLabel;

    public EnergyDashboardController(EnergyApiClient apiClient) {
        this.apiClient = apiClient;
    }

    // Called by the FXMLLoader once the @FXML fields are injected.
    @FXML
    private void initialize() {
        fillHours(startHour);
        fillHours(endHour);

        LocalDate today = LocalDate.now();
        startDate.setValue(today);
        startHour.setValue("00:00");
        endDate.setValue(today);
        endHour.setValue(String.format("%02d:00", LocalDateTime.now().getHour()));
    }

    // Fills the 24 hours of a day (00:00 .. 23:00). The list is built in code, not loaded from the database.
    private void fillHours(ComboBox<String> box) {
        for (int hour = 0; hour < 24; hour++) {
            box.getItems().add(String.format("%02d:00", hour));
        }
    }

    @FXML
    private void onRefresh() {
        loadCurrentPercentages();
    }

    @FXML
    private void onShowData() {
        loadHistoricalUsage();
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

    // Sums the hourly rows into the three totals the GUI shows. Pure logic, no JavaFX needed.
    static UsageSummary summarize(List<HistoricalUsageDTO> entries) {
        double produced = entries.stream().mapToDouble(HistoricalUsageDTO::getCommunityProduced).sum();
        double used = entries.stream().mapToDouble(HistoricalUsageDTO::getCommunityUsed).sum();
        double gridUsed = entries.stream().mapToDouble(HistoricalUsageDTO::getGridUsed).sum();
        return new UsageSummary(produced, used, gridUsed);
    }

    record UsageSummary(double produced, double used, double gridUsed) {
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

        UsageSummary summary = summarize(entries);
        communityProducedLabel.setText("Community produced: " + format(summary.produced()) + " kWh");
        communityUsedLabel.setText("Community used: " + format(summary.used()) + " kWh");
        gridUsedLabel.setText("Grid used: " + format(summary.gridUsed()) + " kWh");
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
