package com.codeflow.frontend.controllers;

import com.codeflow.shared.AppConstants;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class OpeningController {

    public Node getView() {
        StackPane root = new StackPane();
        root.getStyleClass().add("opening-page");

        VBox mainContainer = new VBox(30);
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.setPadding(new Insets(50));

        // Large owl (owl2.png) at the top
        ImageView bigOwl = new ImageView();
        try {
            bigOwl.setImage(new Image(getClass().getResourceAsStream("/images/owl4.png")));
            bigOwl.setFitHeight(200);
            bigOwl.setPreserveRatio(true);
        } catch (Exception e) {
            System.err.println("owl2.png not found, using emoji fallback");
            bigOwl.setImage(null);
            Label fallback = new Label("");
            fallback.setStyle("-fx-font-size: 100px;");
            mainContainer.getChildren().add(fallback);
        }
        if (bigOwl.getImage() != null) {
            bigOwl.getStyleClass().add("opening-owl");
            mainContainer.getChildren().add(bigOwl);
        }

        // Welcome text
        Label title = new Label("CodeFlow");
        title.getStyleClass().add("welcome-title");

        Label subtitle = new Label("Learn, Code, Grow with Hoot!");
        subtitle.getStyleClass().add("welcome-subtitle");

        VBox textBox = new VBox(10, title, subtitle);
        textBox.setAlignment(Pos.CENTER);

        // Waving owl (hi.png) with animation
        ImageView wavingOwl = new ImageView();
        try {
            wavingOwl.setImage(new Image(getClass().getResourceAsStream("/images/hi.png")));
            wavingOwl.setFitHeight(120);
            wavingOwl.setPreserveRatio(true);
        } catch (Exception e) {
            System.err.println("hi.png not found, using emoji");
            wavingOwl.setImage(null);
            Label waveFallback = new Label("");
            waveFallback.setStyle("-fx-font-size: 60px;");
            mainContainer.getChildren().add(waveFallback);
        }

        if (wavingOwl.getImage() != null) {
            // Bounce animation
            ScaleTransition bounce = new ScaleTransition(Duration.seconds(1.5), wavingOwl);
            bounce.setFromX(1.0);
            bounce.setFromY(1.0);
            bounce.setToX(1.1);
            bounce.setToY(1.1);
            bounce.setAutoReverse(true);
            bounce.setCycleCount(ScaleTransition.INDEFINITE);
            bounce.play();
        }

        // Login form
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setMaxWidth(300);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setMaxWidth(300);

        Button loginBtn = new Button("Log In");
        loginBtn.getStyleClass().add("button");
        loginBtn.setMaxWidth(300);

        Hyperlink signupLink = new Hyperlink("New here? Sign up");
        signupLink.getStyleClass().add("hyperlink");

        VBox formBox = new VBox(15, usernameField, passwordField, loginBtn, signupLink);
        formBox.setAlignment(Pos.CENTER);
        formBox.setMaxWidth(400);

        // Combine owls and form
        HBox owlsRow = new HBox(50, wavingOwl, formBox);
        owlsRow.setAlignment(Pos.CENTER);

        mainContainer.getChildren().addAll(textBox, owlsRow);

        root.getChildren().add(mainContainer);
        return root;
    }
}