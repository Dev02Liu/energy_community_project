package com.energy_community_project.gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        String javaVersion = System.getProperty("java.version");
        String javafxVersion = System.getProperty("javafx.version");
        Label l = new Label("Energy Community GUI\nRunning on Java " + javaVersion + " with JavaFX " + javafxVersion);
        Scene scene = new Scene(new StackPane(l), 400, 200);
        stage.setScene(scene);
        stage.show();
    }

    static void main(String[] args) {
        launch();
    }
}