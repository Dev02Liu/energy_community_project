package com.energy_community_project.gui.app;

import com.energy_community_project.gui.client.EnergyApiClient;
import com.energy_community_project.gui.controller.EnergyDashboardController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class EnergyGuiApplication extends Application {

    private static final String BASE_URL = "http://localhost:8080";

    @Override
    public void start(Stage stage) {
        EnergyApiClient apiClient = new EnergyApiClient(BASE_URL);
        EnergyDashboardController controller = new EnergyDashboardController(apiClient);

        stage.setTitle("Energy Community GUI");
        stage.setScene(new Scene(controller.createView(), 420, 420));
        stage.show();

        controller.loadCurrentPercentages();
    }
}
