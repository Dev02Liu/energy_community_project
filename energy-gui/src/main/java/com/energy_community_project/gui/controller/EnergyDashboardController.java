package com.energy_community_project.gui.controller;

import com.energy_community_project.gui.client.EnergyApiClient;
import com.energy_community_project.gui.dto.CurrentPercentageDTO;
import com.energy_community_project.gui.dto.HistoricalSummaryDTO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

public class EnergyDashboardController {

    // Shared error text used when API requests fail or return no displayable data.
    private static final String ERROR_MESSAGE = "Error fetching data";

    // Service dependencies and number formatting used by the dashboard view.
    private final EnergyApiClient apiClient;
    private final DecimalFormat numberFormat =
            new DecimalFormat("0.#####", new DecimalFormatSymbols(Locale.ENGLISH));

    // FXML-bound controls for the current percentage and historical usage sections.
    @FXML private Label communityPoolLabel;
    @FXML private Label gridPortionLabel;
    @FXML private DatePicker startDate;
    @FXML private ComboBox<String> startHour;
    @FXML private DatePicker endDate;
    @FXML private ComboBox<String> endHour;
    @FXML private Label communityProducedLabel;
    @FXML private Label communityUsedLabel;
    @FXML private Label gridUsedLabel;

    // Constructor injection keeps HTTP access outside of the controller logic.
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

    // Builds the selectable hour values for one full day.
    private void fillHours(ComboBox<String> box) {
        for (int hour = 0; hour < 24; hour++) {
            box.getItems().add(String.format("%02d:00", hour));
        }
    }

    // Handles the refresh button for the current percentage display.
    @FXML
    private void onRefresh() {
        loadCurrentPercentages();
    }

    // Handles the show-data button for the selected historical date range.
    @FXML
    private void onShowData() {
        loadHistoricalUsage();
    }

    // Loads the latest percentage values asynchronously and updates the JavaFX UI thread.
    public void loadCurrentPercentages() {
        apiClient.fetchCurrentPercentage()
                .thenAccept(dto -> Platform.runLater(() -> showCurrentPercentage(dto)))
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    showCurrentError(ERROR_MESSAGE);
                    return null;
                });
    }

    // Validates the selected date range, requests historical usage, and displays the result.
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

        apiClient.fetchHistoricalData(start.toString(), end.toString())
                .thenAccept(summary -> Platform.runLater(() -> showHistoricalUsage(summary)))
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

    // Writes current percentage values into the top dashboard labels.
    private void showCurrentPercentage(CurrentPercentageDTO dto) {
        communityPoolLabel.setText("Community Pool: " + format(dto.getCommunityDepleted()) + "% used");
        gridPortionLabel.setText("Grid Portion: " + format(dto.getGridPortion()) + "%");
    }

    // Writes the aggregated historical totals (summed by the REST API) into the bottom dashboard labels.
    private void showHistoricalUsage(HistoricalSummaryDTO summary) {
        communityProducedLabel.setText("Community produced: " + format(summary.getCommunityProduced()) + " kWh");
        communityUsedLabel.setText("Community used: " + format(summary.getCommunityUsed()) + " kWh");
        gridUsedLabel.setText("Grid used: " + format(summary.getGridUsed()) + " kWh");
    }

    // Formats numeric values consistently for all GUI labels.
    private String format(double value) {
        return numberFormat.format(value);
    }

    // Displays an error state for the current percentage labels on the JavaFX UI thread.
    private void showCurrentError(String message) {
        Platform.runLater(() -> {
            communityPoolLabel.setText("Community Pool: " + message);
            gridPortionLabel.setText("Grid Portion: " + message);
        });
    }

    // Displays an error state for the historical usage labels on the JavaFX UI thread.
    private void showHistoricalError(String message) {
        Platform.runLater(() -> {
            communityProducedLabel.setText("Community produced: " + message);
            communityUsedLabel.setText("Community used: " + message);
            gridUsedLabel.setText("Grid used: " + message);
        });
    }
}
