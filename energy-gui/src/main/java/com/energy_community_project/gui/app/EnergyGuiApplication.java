package com.energy_community_project.gui.app;

import com.energy_community_project.gui.client.EnergyApiClient;
import com.energy_community_project.gui.controller.EnergyDashboardController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class EnergyGuiApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        EnergyApiClient apiClient = new EnergyApiClient("http://localhost:8080");

        FXMLLoader loader = new FXMLLoader(getClass().getResource("energy-view.fxml"));
        loader.setControllerFactory(type -> new EnergyDashboardController(apiClient));
        Parent root = loader.load();

        stage.setTitle("Energy Community GUI");
        stage.setScene(new Scene(root, 420, 420));
        stage.show();

        EnergyDashboardController controller = loader.getController();
        controller.loadCurrentPercentages();
    }
}
