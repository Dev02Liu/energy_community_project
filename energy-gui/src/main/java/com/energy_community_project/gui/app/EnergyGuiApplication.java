package com.energy_community_project.gui.app;

import com.energy_community_project.gui.client.EnergyApiClient;
import com.energy_community_project.gui.controller.EnergyDashboardController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class EnergyGuiApplication extends Application {

    private static final String DEFAULT_BASE_URL = "http://localhost:8080";

    @Override
    public void start(Stage stage) {
        EnergyApiClient apiClient = new EnergyApiClient(resolveBaseUrl());
        EnergyDashboardController controller = new EnergyDashboardController(apiClient);

        stage.setTitle("Energy Community GUI");
        stage.setScene(new Scene(controller.createView(), 420, 420));
        stage.show();

        controller.loadCurrentPercentages();
    }

    /**
     * Resolves the REST API base URL. Overrideable without recompiling via the
     * {@code energy.api.base-url} system property or the {@code ENERGY_API_BASE_URL}
     * environment variable; falls back to {@value #DEFAULT_BASE_URL}.
     */
    static String resolveBaseUrl() {
        String property = System.getProperty("energy.api.base-url");
        if (property != null && !property.isBlank()) {
            return property.trim();
        }
        String env = System.getenv("ENERGY_API_BASE_URL");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        return DEFAULT_BASE_URL;
    }
}
