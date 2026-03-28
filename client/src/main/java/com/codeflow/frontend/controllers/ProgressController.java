package com.codeflow.frontend.controllers;

import com.codeflow.api.services.ProgressService;
import com.codeflow.shared.AppConstants;
import javafx.concurrent.Task;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ProgressController {

    private final ProgressService progressService = new ProgressService();

    private Map<LocalDate, Integer> dailyProgress = new HashMap<>();
    private int totalSolved = 0;
    private int currentStreak = 0;
    private int weekProgress = 0;
    private double averagePerDay = 0.0;

    private GridPane heatmapGrid;
    private VBox statsContainer;
    private HBox statsRow;

    public ProgressController() {
        // data loaded after view is built
    }

    public int getTotalSolved()   { return totalSolved;   }
    public int getCurrentStreak() { return currentStreak; }

    public Node getView() {
        StackPane root = new StackPane();

        // Background
        ImageView bgImage = new ImageView();
        try {
            bgImage.setImage(new Image(getClass().getResourceAsStream("/images/PinkBg.png")));
            bgImage.setPreserveRatio(false);
            bgImage.fitWidthProperty().bind(root.widthProperty());
            bgImage.fitHeightProperty().bind(root.heightProperty());
        } catch (Exception e) {
            root.setStyle("-fx-background-color: white;");
        }
        if (bgImage.getImage() != null) root.getChildren().add(bgImage);

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setMaxWidth(1100);
        content.setAlignment(Pos.TOP_CENTER);
        scroll.setContent(content);
        root.getChildren().add(scroll);
        StackPane.setAlignment(scroll, Pos.TOP_CENTER);

        // ===== Owl Header =====
        HBox owlHeader = new HBox(15);
        owlHeader.setAlignment(Pos.CENTER_LEFT);
        owlHeader.setStyle("-fx-background-color: rgba(255,255,255,0.8); -fx-background-radius: 30; -fx-padding: 10 20;");

        ImageView owlIcon;
        try {
            owlIcon = new ImageView(new Image(getClass().getResourceAsStream("/images/owl1.png")));
            owlIcon.setFitHeight(50);
            owlIcon.setFitWidth(50);
        } catch (Exception e) {
            owlIcon = new ImageView();
            Label fallback = new Label("🦉");
            fallback.setFont(Font.font("Arial", FontWeight.BOLD, 30));
            owlHeader.getChildren().add(fallback);
        }
        if (owlIcon.getImage() != null) owlHeader.getChildren().add(owlIcon);

        VBox welcomeText = new VBox(5);
        Label greeting = new Label("Your progress");
        greeting.setFont(Font.font("Arial Rounded", FontWeight.BOLD, 18));
        greeting.setTextFill(Color.web(AppConstants.DEEP_TEAL));
        Label subGreeting = new Label("Keep it up! 📈");
        subGreeting.setFont(Font.font("Arial", 12));
        subGreeting.setTextFill(Color.web(AppConstants.MEDIUM_TEAL));
        welcomeText.getChildren().addAll(greeting, subGreeting);
        owlHeader.getChildren().add(welcomeText);

        Region spacerHeader = new Region();
        HBox.setHgrow(spacerHeader, Priority.ALWAYS);
        owlHeader.getChildren().add(spacerHeader);

        // ===== Stats Cards =====
        statsContainer = new VBox(10);
        statsRow = new HBox(20);
        statsRow.setAlignment(Pos.CENTER);
        statsRow.getChildren().addAll(
                statCard("Current Streak", "0",   AppConstants.GREEN),
                statCard("Total Solved",   "0",   AppConstants.MAUVE),
                statCard("This Week",      "0",   AppConstants.BLUE),
                statCard("Avg / Day",      "0.0", AppConstants.YELLOW)
        );
        statsContainer.getChildren().add(statsRow);

        // ===== GitHub-style Heatmap Card =====
        VBox heatmapCard = new VBox(12);
        heatmapCard.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 20;" +
                        "-fx-padding: 24;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 3);"
        );

        HBox heatHeaderRow = new HBox(10);
        heatHeaderRow.setAlignment(Pos.CENTER_LEFT);
        Label heatHeader = new Label("🟩 Contribution Graph — Last 365 Days");
        heatHeader.setFont(Font.font("Arial Rounded", FontWeight.BOLD, 18));
        heatHeader.setTextFill(Color.web(AppConstants.DEEP_TEAL));

        Region heatSpacer = new Region();
        HBox.setHgrow(heatSpacer, Priority.ALWAYS);

        // Legend
        HBox legend = new HBox(4);
        legend.setAlignment(Pos.CENTER_RIGHT);
        Label lessLbl = new Label("Less");
        lessLbl.setFont(Font.font("Arial", 10));
        lessLbl.setTextFill(Color.web(AppConstants.SUBTEXT1));
        legend.getChildren().add(lessLbl);
        for (int i = 0; i <= 4; i++) {
            Label sq = new Label("  ");
            sq.setStyle("-fx-background-color: " + githubGreen(i) + "; -fx-background-radius: 3;");
            sq.setPrefSize(14, 14);
            sq.setMinSize(14, 14);
            legend.getChildren().add(sq);
        }
        Label moreLbl = new Label("More");
        moreLbl.setFont(Font.font("Arial", 10));
        moreLbl.setTextFill(Color.web(AppConstants.SUBTEXT1));
        legend.getChildren().add(moreLbl);

        heatHeaderRow.getChildren().addAll(heatHeader, heatSpacer, legend);

        // Month labels above grid
        HBox monthLabels = new HBox(0);
        monthLabels.setPadding(new Insets(0, 0, 0, 26));

        // Day-of-week labels + grid side by side
        HBox gridWithDays = new HBox(4);

        VBox dayLabels = new VBox(0);
        dayLabels.setAlignment(Pos.TOP_LEFT);
        String[] days = {"", "Mon", "", "Wed", "", "Fri", ""};
        for (String d : days) {
            Label dl = new Label(d);
            dl.setFont(Font.font("Arial", 9));
            dl.setTextFill(Color.web(AppConstants.SUBTEXT1));
            dl.setPrefHeight(17);
            dl.setPrefWidth(22);
            dayLabels.getChildren().add(dl);
        }

        heatmapGrid = new GridPane();
        heatmapGrid.setHgap(3);
        heatmapGrid.setVgap(3);

        gridWithDays.getChildren().addAll(dayLabels, heatmapGrid);

        VBox heatmapWrapper = new VBox(4);
        heatmapWrapper.getChildren().addAll(monthLabels, gridWithDays);

        heatmapCard.getChildren().addAll(heatHeaderRow, heatmapWrapper);
        content.getChildren().addAll(owlHeader, statsContainer, heatmapCard);

        loadDataAsync(monthLabels);
        return root;
    }

    private void loadDataAsync(HBox monthLabels) {
        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() {
                dailyProgress = progressService.getDailyProgress(365);
                totalSolved   = progressService.getTotalSolved();
                currentStreak = progressService.getCurrentStreak();
                weekProgress  = progressService.getWeekProgress();
                averagePerDay = progressService.getAveragePerDay();
                return null;
            }
        };
        loadTask.setOnSucceeded(e -> {
            updateStats();
            updateHeatmap(monthLabels);
        });
        loadTask.setOnFailed(e -> {
            Throwable ex = loadTask.getException();
            ex.printStackTrace();
            javafx.application.Platform.runLater(() ->
                    new Alert(Alert.AlertType.ERROR, "Failed to load progress: " + ex.getMessage()).showAndWait()
            );
        });
        new Thread(loadTask).start();
    }

    private void updateStats() {
        statsRow.getChildren().setAll(
                statCard("Current Streak", currentStreak + " days", AppConstants.GREEN),
                statCard("Total Solved",   String.valueOf(totalSolved),           AppConstants.MAUVE),
                statCard("This Week",      String.valueOf(weekProgress),          AppConstants.BLUE),
                statCard("Avg / Day",      String.format("%.1f", averagePerDay), AppConstants.YELLOW)
        );
    }

    private void updateHeatmap(HBox monthLabels) {
        heatmapGrid.getChildren().clear();
        monthLabels.getChildren().clear();

        LocalDate today   = LocalDate.now();
        LocalDate endDate = today;

        // Start from the Sunday on or before 364 days ago
        LocalDate startDate = today.minusDays(364);
        while (startDate.getDayOfWeek() != DayOfWeek.SUNDAY) {
            startDate = startDate.minusDays(1);
        }

        int col = 0;
        String lastMonth = "";
        LocalDate cursor = startDate;
        double cellSize = 14 + 3; // cell + gap

        while (!cursor.isAfter(endDate)) {
            String month = cursor.format(DateTimeFormatter.ofPattern("MMM"));
            if (!month.equals(lastMonth)) {
                // Add spacer for any skipped cols before first label
                if (lastMonth.isEmpty() && col > 0) {
                    Region spacer = new Region();
                    spacer.setPrefWidth(col * cellSize);
                    monthLabels.getChildren().add(spacer);
                }
                Label ml = new Label(month);
                ml.setFont(Font.font("Arial", FontWeight.BOLD, 10));
                ml.setTextFill(Color.web(AppConstants.SUBTEXT1));
                ml.setPrefWidth(cellSize * 4); // roughly one month width
                monthLabels.getChildren().add(ml);
                lastMonth = month;
            }

            for (int row = 0; row < 7; row++) {
                LocalDate cellDate = cursor.plusDays(row);
                if (cellDate.isAfter(endDate)) break;

                int count = dailyProgress.getOrDefault(cellDate, 0);

                Label cell = new Label();
                cell.setPrefSize(14, 14);
                cell.setMinSize(14, 14);
                cell.setMaxSize(14, 14);
                cell.setStyle(
                        "-fx-background-color: " + githubGreen(countToLevel(count)) + ";" +
                                "-fx-background-radius: 3;"
                );

                String tipText = cellDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) +
                        ": " + count + " problem" + (count == 1 ? "" : "s");
                Tooltip tip = new Tooltip(tipText);
                Tooltip.install(cell, tip);

                heatmapGrid.add(cell, col, row);
            }

            cursor = cursor.plusWeeks(1);
            col++;
        }
    }

    private int countToLevel(int n) {
        if (n == 0) return 0;
        if (n == 1) return 1;
        if (n <= 3) return 2;
        if (n <= 6) return 3;
        return 4;
    }

    private String githubGreen(int level) {
        return switch (level) {
            case 1 -> "#9be9a8";
            case 2 -> "#40c463";
            case 3 -> "#30a14e";
            case 4 -> "#216e39";
            default -> "#ebedf0";
        };
    }

    private VBox statCard(String title, String value, String color) {
        VBox card = new VBox(6);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-padding: 20;" +
                        "-fx-background-radius: 30;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2);"
        );
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(220);

        Label vl = new Label(value);
        vl.setFont(Font.font("Arial Rounded", FontWeight.BOLD, 26));
        vl.setTextFill(Color.web(color));

        Label tl = new Label(title);
        tl.setFont(Font.font("Arial", 12));
        tl.setTextFill(Color.web(AppConstants.SUBTEXT1));
        tl.setWrapText(true);
        tl.setAlignment(Pos.CENTER);

        card.getChildren().addAll(vl, tl);
        return card;
    }
}