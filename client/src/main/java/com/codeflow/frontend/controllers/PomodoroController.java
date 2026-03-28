package com.codeflow.frontend.controllers;

import com.codeflow.shared.AppConstants;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class PomodoroController {

    private static final int POMODORO_MINUTES = 25;
    private int secondsLeft = POMODORO_MINUTES * 60;
    private Timeline timer;
    private boolean pomodoroRunning = false;

    private Label timerLabel;
    private Label statusLabel;
    private ToggleButton musicToggle;
    private Button startBtn, pauseBtn, resetBtn;
    private MediaPlayer mediaPlayer;
    private boolean musicLoaded = false;

    // Owl animation frames
    private ImageView owlView;
    private Timeline owlAnimation;
    private Image owl1, owl2,owl3;

    public PomodoroController() {
        initMediaPlayer();
        loadOwlImages();
    }

    private void initMediaPlayer() {
        try {
            String path = getClass().getResource("/audio/soft-piano-inspiration_medium-1-399918.mp3").toURI().toString();
            Media media = new Media(path);
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            musicLoaded = true;
        } catch (Exception e) {
            System.err.println("Could not load study music: " + e.getMessage());
        }
    }

    private void loadOwlImages() {
        try {
            owl1 = new Image(getClass().getResourceAsStream("/images/owl1.png"));
            owl2 = new Image(getClass().getResourceAsStream("/images/owl2.png"));

        } catch (Exception e) {
            System.err.println("Owl images not found: " + e.getMessage());
        }
    }

    public Node getView() {
        StackPane root = new StackPane();

        // Background
        try {
            ImageView bg = new ImageView(new Image(getClass().getResourceAsStream("/images/PurpleBg.png")));
            bg.setPreserveRatio(false);
            bg.fitWidthProperty().bind(root.widthProperty());
            bg.fitHeightProperty().bind(root.heightProperty());
            root.getChildren().add(bg);
        } catch (Exception e) {
            root.setStyle("-fx-background-color: " + AppConstants.BASE + ";");
        }

        VBox content = new VBox(30);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(40, 30, 40, 30));
        root.getChildren().add(content);

        // ── Page title ──────────────────────────────────────────────────────
        Label pageTitle = new Label("💜 Pomodoro Focus Timer");
        pageTitle.setFont(Font.font("Arial Rounded", FontWeight.BOLD, 28));
        pageTitle.setTextFill(Color.web("#FFFFFF"));
        pageTitle.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 6, 0, 0, 2);");

        Label pageSub = new Label("Stay focused. Study smart. Hoot is with you!");
        pageSub.setFont(Font.font("Arial", 15));
        pageSub.setTextFill(Color.web("#EEE8FF"));

        VBox titleBox = new VBox(6, pageTitle, pageSub);
        titleBox.setAlignment(Pos.CENTER);

        // ── Main card ──────────────────────────────────────────────────────
        HBox mainCard = new HBox(50);
        mainCard.setAlignment(Pos.CENTER);
        mainCard.setPadding(new Insets(40, 60, 40, 60));
        mainCard.setStyle(
            "-fx-background-color: rgba(255,255,255,0.92);" +
            "-fx-background-radius: 28;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 24, 0, 0, 8);"
        );
        mainCard.setMaxWidth(860);

        // Left: animated owl
        VBox owlBox = buildOwlSection();

        // Right: timer + controls
        VBox timerBox = buildTimerSection();

        mainCard.getChildren().addAll(owlBox, timerBox);

        // ── Music card ─────────────────────────────────────────────────────
        HBox musicCard = buildMusicCard();

        // ── Tips card ──────────────────────────────────────────────────────
        HBox tipsCard = buildTipsCard();

        content.getChildren().addAll(titleBox, mainCard, musicCard, tipsCard);
        return root;
    }

    private VBox buildOwlSection() {
        VBox box = new VBox(16);
        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(280);

        owlView = new ImageView();
        if (owl1 != null) {
            owlView.setImage(owl1);
            owlView.setFitHeight(200);
            owlView.setPreserveRatio(true);
        } else {
            Label e = new Label("🦉");
            e.setStyle("-fx-font-size: 90px;");
            box.getChildren().add(e);
            return box;
        }

        Label owlLabel = new Label("Hoot is ready to study!");
        owlLabel.setFont(Font.font("Arial Rounded", FontWeight.BOLD, 15));
        owlLabel.setTextFill(Color.web(AppConstants.DEEP_TEAL));

        box.getChildren().addAll(owlView, owlLabel);
        return box;
    }

    private VBox buildTimerSection() {
        VBox box = new VBox(20);
        box.setAlignment(Pos.CENTER);

        // Timer display
        timerLabel = new Label(formatTime(secondsLeft));
        timerLabel.setFont(Font.font("Arial Rounded", FontWeight.BOLD, 72));
        timerLabel.setTextFill(Color.web(AppConstants.DEEP_TEAL));
        timerLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 6, 0, 0, 2);");

        // Status text
        statusLabel = new Label("Ready to focus?");
        statusLabel.setFont(Font.font("Arial", 14));
        statusLabel.setTextFill(Color.web(AppConstants.MEDIUM_TEAL));

        // Controls
        startBtn = buildCircleBtn("▶", "#4CAF50");
        pauseBtn = buildCircleBtn("⏸", "#FF9800");
        resetBtn = buildCircleBtn("↺", "#5F9598");

        startBtn.setOnAction(e -> startPomodoro());
        pauseBtn.setOnAction(e -> pausePomodoro());
        resetBtn.setOnAction(e -> resetPomodoro());

        HBox controls = new HBox(20, startBtn, pauseBtn, resetBtn);
        controls.setAlignment(Pos.CENTER);

        // Session info
        Label sessionInfo = new Label("25 min focus · 5 min break");
        sessionInfo.setFont(Font.font("Arial", 12));
        sessionInfo.setTextFill(Color.web(AppConstants.OVERLAY0));

        box.getChildren().addAll(timerLabel, statusLabel, controls, sessionInfo);
        return box;
    }

    private HBox buildMusicCard() {
        HBox card = new HBox(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(18, 30, 18, 30));
        card.setMaxWidth(860);
        card.setStyle(
            "-fx-background-color: rgba(255,255,255,0.88);" +
            "-fx-background-radius: 18;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 12, 0, 0, 4);"
        );

        Label musicIcon = new Label("🎵");
        musicIcon.setFont(Font.font("Arial", 28));

        VBox musicInfo = new VBox(3);
        Label musicTitle = new Label("Study Music");
        musicTitle.setFont(Font.font("Arial Rounded", FontWeight.BOLD, 16));
        musicTitle.setTextFill(Color.web(AppConstants.DEEP_TEAL));
        Label musicDesc = new Label("Soft piano · Auto-synced with Pomodoro timer");
        musicDesc.setFont(Font.font("Arial", 12));
        musicDesc.setTextFill(Color.web(AppConstants.MEDIUM_TEAL));
        musicInfo.getChildren().addAll(musicTitle, musicDesc);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        musicToggle = new ToggleButton("🎵  Music Off");
        musicToggle.setStyle(musicToggleStyle(false));
        musicToggle.setSelected(false);
        musicToggle.setOnAction(e -> handleMusicToggle());

        card.getChildren().addAll(musicIcon, musicInfo, spacer, musicToggle);
        return card;
    }

    private HBox buildTipsCard() {
        HBox card = new HBox(16);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(16, 30, 16, 30));
        card.setMaxWidth(860);
        card.setStyle(
            "-fx-background-color: rgba(255,255,255,0.75);" +
            "-fx-background-radius: 16;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 3);"
        );

        Label bulb = new Label("💡");
        bulb.setFont(Font.font("Arial", 22));

        Label tip = new Label("Pro tip: During a Pomodoro, silence notifications and put your phone away. " +
                              "After 4 sessions, take a longer 15-30 min break. 🌟");
        tip.setFont(Font.font("Arial", 12));
        tip.setTextFill(Color.web(AppConstants.MEDIUM_TEAL));
        tip.setWrapText(true);

        card.getChildren().addAll(bulb, tip);
        return card;
    }

    // ── Timer logic ──────────────────────────────────────────────────────────

    private void startPomodoro() {
        if (timer == null) {
            timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
            timer.setCycleCount(Timeline.INDEFINITE);
        }
        timer.play();
        pomodoroRunning = true;
        statusLabel.setText("🔥 Focusing...");
        startOwlAnimation();
        // Auto-start music if it's loaded and toggle is on
        autoSyncMusic(true);
        startBtn.setDisable(true);
        pauseBtn.setDisable(false);
    }

    private void tick() {
        if (secondsLeft > 0) {
            secondsLeft--;
            timerLabel.setText(formatTime(secondsLeft));
            // Pulse timer on last 10s
            if (secondsLeft <= 10) {
                ScaleTransition pulse = new ScaleTransition(Duration.millis(400), timerLabel);
                pulse.setFromX(1.0); pulse.setFromY(1.0);
                pulse.setToX(1.08); pulse.setToY(1.08);
                pulse.setAutoReverse(true); pulse.setCycleCount(2);
                pulse.play();
                timerLabel.setTextFill(Color.web(AppConstants.RED));
            }
        } else {
            finishSession();
        }
    }

    private void pausePomodoro() {
        if (timer != null) timer.pause();
        pomodoroRunning = false;
        stopOwlAnimation();
        autoSyncMusic(false);
        statusLabel.setText("⏸ Paused");
        startBtn.setDisable(false);
        pauseBtn.setDisable(true);
    }

    private void resetPomodoro() {
        if (timer != null) { timer.stop(); timer = null; }
        pomodoroRunning = false;
        secondsLeft = POMODORO_MINUTES * 60;
        timerLabel.setText(formatTime(secondsLeft));
        timerLabel.setTextFill(Color.web(AppConstants.DEEP_TEAL));
        statusLabel.setText("Ready to focus?");
        stopOwlAnimation();
        autoSyncMusic(false);
        startBtn.setDisable(false);
        pauseBtn.setDisable(false);
    }

    private void finishSession() {
        if (timer != null) { timer.stop(); timer = null; }
        pomodoroRunning = false;
        stopOwlAnimation();
        autoSyncMusic(false);
        musicToggle.setSelected(false);
        musicToggle.setStyle(musicToggleStyle(false));
        musicToggle.setText("🎵  Music Off");
        statusLabel.setText("✅ Session complete!");
        timerLabel.setTextFill(Color.web("#4CAF50"));
        startBtn.setDisable(false);

        // Show beautiful completion dialog
        showCompletionDialog();

        resetPomodoro();
    }

    private void showCompletionDialog() {
        // Build a custom dialog
        javafx.stage.Stage dialogStage = new javafx.stage.Stage();
        dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Session Complete!");
        dialogStage.setResizable(false);

        VBox dialogContent = new VBox(20);
        dialogContent.setAlignment(Pos.CENTER);
        dialogContent.setPadding(new Insets(50, 60, 50, 60));
        dialogContent.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #f0f7f4, #e8f5e9);" +
            "-fx-background-radius: 0;"
        );

        // Owl icon + stars
        Label celebIcon = new Label("🦉✨");
        celebIcon.setFont(Font.font("Arial", 56));

        // Animated title
        Label congrats = new Label("You Did It!");
        congrats.setFont(Font.font("Arial Rounded", FontWeight.BOLD, 32));
        congrats.setTextFill(Color.web("#2E7D32"));

        Label message = new Label(
            "You have successfully finished\nyour Pomodoro session!");
        message.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        message.setTextFill(Color.web("#388E3C"));
        message.setTextAlignment(TextAlignment.CENTER);
        message.setWrapText(true);

        Label subMessage = new Label(
            "🌟 Congratulations, superstar! \n" +
            "Every session brings you one step closer to your goals.\n" +
            "Take a well-deserved break — you've earned it! ☕");
        subMessage.setFont(Font.font("Arial", 14));
        subMessage.setTextFill(Color.web("#4CAF50"));
        subMessage.setTextAlignment(TextAlignment.CENTER);
        subMessage.setWrapText(true);

        Separator sep = new Separator();
        sep.setMaxWidth(300);

        Label breakSuggestion = new Label("⏱ Suggested break: 5 minutes");
        breakSuggestion.setFont(Font.font("Arial", 13));
        breakSuggestion.setTextFill(Color.web("#757575"));

        Button closeBtn = new Button("Start Break →");
        closeBtn.setStyle(
            "-fx-background-color: #4CAF50;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 15px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 25;" +
            "-fx-padding: 12 35;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(76,175,80,0.5), 10, 0, 0, 3);"
        );
        closeBtn.setOnAction(e -> dialogStage.close());
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(
            "-fx-background-color: #388E3C; -fx-text-fill: white; -fx-font-size: 15px;" +
            "-fx-font-weight: bold; -fx-background-radius: 25; -fx-padding: 12 35; -fx-cursor: hand;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle(
            "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 15px;" +
            "-fx-font-weight: bold; -fx-background-radius: 25; -fx-padding: 12 35; -fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(76,175,80,0.5), 10, 0, 0, 3);"));

        dialogContent.getChildren().addAll(
            celebIcon, congrats, message, subMessage, sep, breakSuggestion, closeBtn);

        // Entrance animation on the dialog
        dialogContent.setOpacity(0);
        dialogContent.setScaleX(0.85);
        dialogContent.setScaleY(0.85);

        javafx.scene.Scene scene = new javafx.scene.Scene(dialogContent, 480, 460);
        dialogStage.setScene(scene);

        FadeTransition ft = new FadeTransition(Duration.millis(350), dialogContent);
        ft.setToValue(1); ft.play();
        ScaleTransition st = new ScaleTransition(Duration.millis(350), dialogContent);
        st.setToX(1); st.setToY(1);
        st.setInterpolator(Interpolator.EASE_OUT); st.play();

        // Floating owl bounce
        ScaleTransition bounce = new ScaleTransition(Duration.seconds(1.2), celebIcon);
        bounce.setFromX(1); bounce.setFromY(1);
        bounce.setToX(1.12); bounce.setToY(1.12);
        bounce.setAutoReverse(true); bounce.setCycleCount(ScaleTransition.INDEFINITE);
        bounce.play();

        dialogStage.showAndWait();
    }

    // ── Music logic ──────────────────────────────────────────────────────────

    private void handleMusicToggle() {
        if (!musicLoaded) {
            new Alert(Alert.AlertType.WARNING, "Music file not found.").showAndWait();
            musicToggle.setSelected(false);
            return;
        }
        boolean on = musicToggle.isSelected();
        // Only allow music ON if pomodoro is running
        if (on && !pomodoroRunning) {
            musicToggle.setSelected(false);
            showInfo("Music only plays during an active Pomodoro session.\nStart the timer first! 🍅");
            return;
        }
        if (on) {
            mediaPlayer.play();
            musicToggle.setText("🎵  Music On");
            musicToggle.setStyle(musicToggleStyle(true));
        } else {
            mediaPlayer.pause();
            musicToggle.setText("🎵  Music Off");
            musicToggle.setStyle(musicToggleStyle(false));
        }
    }

    private void autoSyncMusic(boolean pomodoroActive) {
        if (!musicLoaded) return;
        if (!pomodoroActive) {
            mediaPlayer.pause();
            musicToggle.setSelected(false);
            musicToggle.setText("🎵  Music Off");
            musicToggle.setStyle(musicToggleStyle(false));
        }
        // When pomodoro starts, don't auto-enable – user must toggle manually
    }

    private String musicToggleStyle(boolean on) {
        return on
            ? "-fx-background-color: " + AppConstants.GREEN + "; -fx-text-fill: white;" +
              "-fx-background-radius: 25; -fx-padding: 10 24; -fx-font-size: 14px;" +
              "-fx-cursor: hand; -fx-font-weight: bold;"
            : "-fx-background-color: #E0E0E0; -fx-text-fill: #616161;" +
              "-fx-background-radius: 25; -fx-padding: 10 24; -fx-font-size: 14px;" +
              "-fx-cursor: hand;";
    }

    // ── Owl animation ────────────────────────────────────────────────────────

    private void startOwlAnimation() {
        if (owl1 == null || owl2 == null|| owlView == null) return;
        stopOwlAnimation();
        final boolean[] frame = {true};
        owlAnimation = new Timeline(new KeyFrame(Duration.millis(700), e -> {
            owlView.setImage(frame[0] ? owl2 : owl1);
            frame[0] = !frame[0];
        }));
        owlAnimation.setCycleCount(Timeline.INDEFINITE);
        owlAnimation.play();

        // Also do a gentle bob
        TranslateTransition bob = new TranslateTransition(Duration.millis(800), owlView);
        bob.setByY(-8); bob.setAutoReverse(true); bob.setCycleCount(Timeline.INDEFINITE);
        bob.play();
    }

    private void stopOwlAnimation() {
        if (owlAnimation != null) { owlAnimation.stop(); owlAnimation = null; }
        if (owlView != null) {
            owlView.setImage(owl3);
            owlView.setTranslateY(0);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Button buildCircleBtn(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle(
            "-fx-background-color: " + color + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 20px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 50;" +
            "-fx-min-width: 60; -fx-min-height: 60;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 6, 0, 0, 2);"
        );
        return btn;
    }

    private String formatTime(int totalSeconds) {
        return String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
