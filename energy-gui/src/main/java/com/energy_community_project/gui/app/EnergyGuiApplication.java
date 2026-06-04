package com.energy_community_project.gui.app;

import com.energy_community_project.gui.client.EnergyApiClient;
import com.energy_community_project.gui.controller.EnergyDashboardController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class EnergyGuiApplication extends Application {

    @Override
    public void start(Stage stage) {
        EnergyApiClient apiClient = new EnergyApiClient("http://localhost:8080");
        EnergyDashboardController controller = new EnergyDashboardController(apiClient);

        stage.setTitle("Energy Community GUI");
        stage.setScene(new Scene(controller.createView(), 420, 420));
        stage.show();

        controller.loadCurrentPercentages();
    }
}
