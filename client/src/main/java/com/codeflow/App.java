package com.codeflow;

import com.codeflow.frontend.views.SplashView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("CodeFlow Tracker");
        primaryStage.setMinWidth(400);
        primaryStage.setMinHeight(600);

        SplashView splashView = new SplashView(primaryStage);
        Scene scene = new Scene(splashView.build(), 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
