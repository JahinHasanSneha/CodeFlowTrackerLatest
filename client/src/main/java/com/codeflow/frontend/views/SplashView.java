package com.codeflow.frontend.views;

import com.codeflow.api.services.AuthService;
import com.codeflow.shared.AppConstants;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class SplashView {

    private final Stage primaryStage;

    public SplashView(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    /**
     * Builds the root layout (Parent) of the splash screen.
     * Uses bg1.png as the background image.
     */
    public StackPane build() {
        StackPane root = new StackPane();

        // Background image
        ImageView background = new ImageView();
        try {
            background.setImage(new Image(getClass().getResourceAsStream("/images/bg1.jpeg")));
            background.setFitWidth(1200);  // adjust to your scene size
            background.setFitHeight(800);
            background.setPreserveRatio(false);
            // Ensure it covers the whole area
            background.fitWidthProperty().bind(root.widthProperty());
            background.fitHeightProperty().bind(root.heightProperty());
        } catch (Exception e) {
            System.err.println("bg1.png not found – using CSS fallback");
            root.getStyleClass().add("opening-page"); // fallback gradient
        }

        // Only add background if loaded successfully
        if (background.getImage() != null) {
            root.getChildren().add(background);
        }

        // Main content container (semi-transparent or fully opaque – you decide)
        VBox mainContainer = new VBox(30);
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.setPadding(new Insets(50));
        // Optional: add a semi-transparent background to make text pop
        // mainContainer.setStyle("-fx-background-color: rgba(255,255,255,0.7); -fx-background-radius: 60;");
        // But we'll keep it clean – the background image is light enough.

        // Large owl (owl2.png) at the top
        ImageView bigOwl = new ImageView();
        try {
            bigOwl.setImage(new Image(getClass().getResourceAsStream("/images/owl2.png")));
            bigOwl.setFitHeight(200);
            bigOwl.setPreserveRatio(true);
            bigOwl.getStyleClass().add("opening-owl");
            mainContainer.getChildren().add(bigOwl);
        } catch (Exception e) {
            System.err.println("owl2.png not found – using emoji fallback");
            Label fallback = new Label("🦉");
            fallback.setStyle("-fx-font-size: 100px;");
            mainContainer.getChildren().add(fallback);
        }

        // Welcome text
        Label title = new Label("CodeFlow Tracker");
        title.getStyleClass().add("welcome-title");

        Label subtitle = new Label("Learn, Code, Grow with Hoot!");
        subtitle.getStyleClass().add("welcome-subtitle");

        VBox textBox = new VBox(10, title, subtitle);
        textBox.setAlignment(Pos.CENTER);

        // Waving owl (hi.png) with bounce animation
        Node wavingNode;
        try {
            ImageView wavingOwl = new ImageView(new Image(getClass().getResourceAsStream("/images/hi.png")));
            wavingOwl.setFitHeight(150);
            wavingOwl.setPreserveRatio(true);

            ScaleTransition bounce = new ScaleTransition(Duration.seconds(1.5), wavingOwl);
            bounce.setFromX(1.0);
            bounce.setFromY(1.0);
            bounce.setToX(1.1);
            bounce.setToY(1.1);
            bounce.setAutoReverse(true);
            bounce.setCycleCount(ScaleTransition.INDEFINITE);
            bounce.play();

            wavingNode = wavingOwl;
        } catch (Exception e) {
            System.err.println("hi.png not found – using emoji");
            Label waveFallback = new Label("👋🦉");
            waveFallback.setStyle("-fx-font-size: 60px;");
            wavingNode = waveFallback;
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
        loginBtn.setOnAction(e -> {
            // Replace with your actual login logic
            String username = usernameField.getText();
            String password = passwordField.getText();

            // Check if fields are empty
            if (username.isEmpty() || password.isEmpty()) {
                showAlert("Error", "Username and password are required");
                return;
            }

            // Call AuthService
            AuthService authService = new AuthService();
            String error = authService.login(username, password);

            if (error == null) {
                // Login successful
                boolean wasFullScreen = primaryStage.isFullScreen();
                boolean wasMaximized  = primaryStage.isMaximized();

                MainView mainView = new MainView(primaryStage);
                primaryStage.getScene().setRoot(mainView.build());

                primaryStage.setFullScreen(wasFullScreen);
                primaryStage.setMaximized(wasMaximized);
            } else {
                // Show error
                showAlert("Login Failed", error);
            }
        });

        Hyperlink signupLink = new Hyperlink("New here? Sign up");
        signupLink.getStyleClass().add("hyperlink");
        signupLink.setOnAction(e -> {
            boolean wasFullScreen = primaryStage.isFullScreen();
            boolean wasMaximized  = primaryStage.isMaximized();

            // Build SignUpView and swap root
            SignUpView signUpView = new SignUpView(primaryStage);
            primaryStage.getScene().setRoot(signUpView.build());

            // Restore fullscreen / maximized state
            primaryStage.setFullScreen(wasFullScreen);
            primaryStage.setMaximized(wasMaximized);
        });

        VBox formBox = new VBox(15, usernameField, passwordField, loginBtn, signupLink);
        formBox.setAlignment(Pos.CENTER);
        formBox.setMaxWidth(400);

        // Combine waving owl and form side by side
        HBox owlsRow = new HBox(50, wavingNode, formBox);
        owlsRow.setAlignment(Pos.CENTER);

        mainContainer.getChildren().addAll(textBox, owlsRow);

        // Add content on top of background
        root.getChildren().add(mainContainer);

        return root;
    }

    private void showAlert(String Title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(Title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Optional: Style the alert to match your theme
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle(
                "-fx-background-color: " + AppConstants.BASE + ";" +
                        "-fx-text-fill: " + AppConstants.TEXT + ";"
        );

        alert.showAndWait();
    }
}