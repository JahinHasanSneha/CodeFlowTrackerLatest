package com.codeflow.frontend.views;

import com.codeflow.api.services.AuthService;
import com.codeflow.shared.AppConstants;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * SIGN-UP SCREEN – themed to match the rest of the app (owl + bg1 image).
 */
public class SignUpView {

    private final Stage       stage;
    private final AuthService authService = new AuthService();

    private TextField     usernameField;
    private TextField     emailField;
    private PasswordField passwordField;
    private PasswordField confirmField;
    private Label         errorLabel;
    private Label         strengthLabel;
    private Button        signUpBtn;

    public SignUpView(Stage stage) {
        this.stage = stage;
    }

    public Parent build() {
        StackPane root = new StackPane();

        // ── Background image (same as Splash) ─────────────────────────────
        try {
            ImageView bg = new ImageView(new Image(getClass().getResourceAsStream("/images/bg1.jpeg")));
            bg.setPreserveRatio(false);
            bg.fitWidthProperty().bind(root.widthProperty());
            bg.fitHeightProperty().bind(root.heightProperty());
            root.getChildren().add(bg);
        } catch (Exception ex) {
            root.setStyle("-fx-background-color: " + AppConstants.BASE + ";");
        }

        // ── Outer layout: owl on left, card on right ───────────────────────
        HBox outerRow = new HBox(60);
        outerRow.setAlignment(Pos.CENTER);
        outerRow.setPadding(new Insets(40));

        // Owl column
        VBox owlCol = new VBox(18);
        owlCol.setAlignment(Pos.CENTER);

        ImageView owlImg;
        try {
            owlImg = new ImageView(new Image(getClass().getResourceAsStream("/images/owl2.png")));
            owlImg.setFitHeight(180);
            owlImg.setPreserveRatio(true);
        } catch (Exception ex) {
            owlImg = null;
        }

        Label brandTitle = new Label("CodeFlow Tracker");
        brandTitle.getStyleClass().add("welcome-title");
        brandTitle.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #1D546D;");

        Label brandSub = new Label("Join Hoot's coding journey!");
        brandSub.setStyle("-fx-font-size: 14px; -fx-text-fill: #407d94;");

        if (owlImg != null) {
            ScaleTransition bounce = new ScaleTransition(Duration.seconds(1.8), owlImg);
            bounce.setFromX(1.0); bounce.setFromY(1.0);
            bounce.setToX(1.06); bounce.setToY(1.06);
            bounce.setAutoReverse(true);
            bounce.setCycleCount(ScaleTransition.INDEFINITE);
            bounce.play();
            owlCol.getChildren().add(owlImg);
        } else {
            Label e = new Label("🦉"); e.setStyle("-fx-font-size: 80px;");
            owlCol.getChildren().add(e);
        }
        owlCol.getChildren().addAll(brandTitle, brandSub);

        // ── Card ──────────────────────────────────────────────────────────
        VBox card = new VBox(16);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(40, 55, 40, 55));
        card.setMaxWidth(460);
        card.setStyle(
            "-fx-background-color: rgba(255,255,255,0.93);" +
            "-fx-background-radius: 22;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 28, 0, 0, 8);"
        );

        // Back button
        Button backBtn = new Button("← Back");
        backBtn.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: " + AppConstants.DEEP_TEAL + ";" +
            "-fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 0;"
        );
        backBtn.setOnAction(e -> stage.getScene().setRoot(new SplashView(stage).build()));
        HBox backRow = new HBox(backBtn);
        backRow.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("🚀");
        icon.setFont(Font.font("Arial", 34));

        Label title = new Label("Create Account");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setTextFill(Color.web(AppConstants.DEEP_TEAL));

        Label subtitle = new Label("Start your coding adventure with Hoot");
        subtitle.setFont(Font.font("Arial", 13));
        subtitle.setTextFill(Color.web(AppConstants.MEDIUM_TEAL));

        VBox titleBox = new VBox(4, icon, title, subtitle);
        titleBox.setAlignment(Pos.CENTER);

        // Form
        VBox form = new VBox(10);

        usernameField = styledField("e.g. john_dev", false);
        emailField    = styledField("e.g. john@example.com", false);
        passwordField = (PasswordField) styledField("At least 6 characters", true);
        confirmField  = (PasswordField) styledField("Repeat your password", true);

        strengthLabel = new Label();
        strengthLabel.setFont(Font.font("Arial", 11));
        strengthLabel.setManaged(false);
        strengthLabel.setVisible(false);
        passwordField.textProperty().addListener((obs, o, n) -> updateStrength(n));

        Label termsLabel = new Label("By signing up you agree to our Terms of Service.");
        termsLabel.setFont(Font.font("Arial", 11));
        termsLabel.setTextFill(Color.web(AppConstants.OVERLAY0));
        termsLabel.setWrapText(true);

        errorLabel = new Label();
        errorLabel.setTextFill(Color.web(AppConstants.RED));
        errorLabel.setFont(Font.font("Arial", 12));
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        signUpBtn = new Button("Create Account");
        signUpBtn.setPrefWidth(Double.MAX_VALUE);
        signUpBtn.setPrefHeight(44);
        signUpBtn.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        applyPrimaryStyle(signUpBtn);
        signUpBtn.setOnAction(e -> handleSignUp());
        confirmField.setOnAction(e -> handleSignUp());

        form.getChildren().addAll(
            fieldGroup("Username", usernameField),
            fieldGroup("Email Address", emailField),
            fieldGroup("Password", passwordField),
            strengthLabel,
            fieldGroup("Confirm Password", confirmField),
            termsLabel,
            errorLabel,
            signUpBtn
        );

        // Login link
        Label hasAccount = new Label("Already have an account? ");
        hasAccount.setTextFill(Color.web(AppConstants.SUBTEXT1));
        hasAccount.setFont(Font.font("Arial", 13));

        Label loginLink = new Label("Log in");
        loginLink.setTextFill(Color.web(AppConstants.DEEP_TEAL));
        loginLink.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        loginLink.setStyle("-fx-cursor: hand;");
        loginLink.setOnMouseClicked(e -> stage.getScene().setRoot(new SplashView(stage).build()));

        HBox loginRow = new HBox(hasAccount, loginLink);
        loginRow.setAlignment(Pos.CENTER);

        card.getChildren().addAll(backRow, titleBox, form, loginRow);

        // Animate card
        card.setOpacity(0);
        card.setTranslateY(20);
        FadeTransition ft = new FadeTransition(Duration.millis(450), card);
        ft.setToValue(1); ft.play();
        TranslateTransition tt = new TranslateTransition(Duration.millis(450), card);
        tt.setToY(0); tt.setInterpolator(Interpolator.EASE_OUT); tt.play();

        outerRow.getChildren().addAll(owlCol, card);

        ScrollPane scroll = new ScrollPane(outerRow);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");

        root.getChildren().add(scroll);
        return root;
    }

    private void handleSignUp() {
        setError(null);
        if (!passwordField.getText().equals(confirmField.getText())) {
            setError("Passwords do not match.");
            return;
        }
        signUpBtn.setDisable(true);
        signUpBtn.setText("Creating account…");

        String error = authService.register(
            usernameField.getText(), emailField.getText(), passwordField.getText());

        if (error != null) {
            setError(error);
            signUpBtn.setDisable(false);
            signUpBtn.setText("Create Account");
        } else {
            authService.login(usernameField.getText(), passwordField.getText());
            stage.getScene().setRoot(new MainView(stage).build());
        }
    }

    private void updateStrength(String password) {
        if (password.isEmpty()) { strengthLabel.setVisible(false); strengthLabel.setManaged(false); return; }
        strengthLabel.setVisible(true); strengthLabel.setManaged(true);
        int score = 0;
        if (password.length() >= 8)                   score++;
        if (password.matches(".*[A-Z].*"))            score++;
        if (password.matches(".*[0-9].*"))            score++;
        if (password.matches(".*[!@#$%^&*()_+].*"))  score++;
        if (score <= 1) { strengthLabel.setText("● Weak"); strengthLabel.setTextFill(Color.web(AppConstants.RED)); }
        else if (score == 2) { strengthLabel.setText("●● Fair"); strengthLabel.setTextFill(Color.web(AppConstants.YELLOW)); }
        else if (score == 3) { strengthLabel.setText("●●● Good"); strengthLabel.setTextFill(Color.web(AppConstants.GREEN)); }
        else { strengthLabel.setText("●●●● Strong"); strengthLabel.setTextFill(Color.web(AppConstants.DEEP_TEAL)); }
    }

    private void setError(String message) {
        if (message == null || message.isEmpty()) {
            errorLabel.setVisible(false); errorLabel.setManaged(false);
        } else {
            errorLabel.setText("⚠ " + message); errorLabel.setVisible(true); errorLabel.setManaged(true);
        }
    }

    private VBox fieldGroup(String labelText, TextField field) {
        Label lbl = new Label(labelText);
        lbl.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        lbl.setTextFill(Color.web(AppConstants.MEDIUM_TEAL));
        return new VBox(5, lbl, field);
    }

    private TextField styledField(String prompt, boolean password) {
        TextField field = password ? new PasswordField() : new TextField();
        field.setPromptText(prompt);
        field.setPrefHeight(42);
        String base = "-fx-background-color: #f0f7fb;" +
                      "-fx-text-fill: #1D546D;" +
                      "-fx-prompt-text-fill: #8fb0c2;" +
                      "-fx-background-radius: 8;" +
                      "-fx-border-color: #5F9598;" +
                      "-fx-border-radius: 8;" +
                      "-fx-border-width: 1.5;" +
                      "-fx-padding: 0 12;";
        field.setStyle(base);
        field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused) field.setStyle(base.replace("#5F9598", AppConstants.DEEP_TEAL));
            else field.setStyle(base);
        });
        return field;
    }

    private void applyPrimaryStyle(Button btn) {
        String base = "-fx-background-color: " + AppConstants.DEEP_TEAL + ";" +
                      "-fx-text-fill: white;" +
                      "-fx-background-radius: 10;" +
                      "-fx-cursor: hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(base.replace(AppConstants.DEEP_TEAL, AppConstants.MEDIUM_TEAL)));
        btn.setOnMouseExited(e -> btn.setStyle(base));
    }
}
