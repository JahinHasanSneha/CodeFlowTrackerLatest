package com.codeflow.frontend.controllers;

import com.codeflow.api.models.Priority; // your model Priority
import com.codeflow.api.models.Project;
import com.codeflow.api.models.Task;
import com.codeflow.shared.AppConstants;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.format.DateTimeFormatter;

public class DashboardController {

    // ✅ keep old signature so existing calls don’t break
    public Node getView(TaskController taskCtrl, ProgressController progressCtrl,
                        CalendarController calendarCtrl) {
        return getView(taskCtrl, progressCtrl, calendarCtrl, null);
    }

    // ✅ new signature to enable Hub navigation
    public Node getView(TaskController taskCtrl, ProgressController progressCtrl,
                        CalendarController calendarCtrl, Runnable onOpenHub) {

        StackPane root = new StackPane();

        try {
            new Image(getClass().getResourceAsStream("/images/BlueBg.png"));
            root.setStyle(
                    "-fx-background-image: url('/images/BlueBg.png');" +
                            "-fx-background-size: cover;" +
                            "-fx-background-position: center center;" +
                            "-fx-background-repeat: no-repeat;"
            );
        } catch (Exception e) {
            root.setStyle("-fx-background-color: " + AppConstants.BASE + ";");
        }

        VBox dashboard = new VBox(24);
        dashboard.setPadding(new Insets(32));
        dashboard.setMaxWidth(1100);
        dashboard.setAlignment(Pos.TOP_CENTER);

        // IMPORTANT: use fully-qualified javafx Priority (because you also have backend Priority)
        VBox.setVgrow(dashboard, javafx.scene.layout.Priority.ALWAYS);

        root.getChildren().add(dashboard);
        StackPane.setAlignment(dashboard, Pos.TOP_CENTER);

        // ===== Owl Header =====
        HBox owlHeader = new HBox(15);
        owlHeader.setAlignment(Pos.CENTER_LEFT);
        owlHeader.getStyleClass().add("card");
        owlHeader.setStyle("-fx-background-color: rgba(255,255,255,0.8); -fx-background-radius: 30; -fx-padding: 10 20;");

        ImageView owlIcon;
        try {
            owlIcon = new ImageView(new Image(getClass().getResourceAsStream("/images/owl1.png")));
            owlIcon.setFitHeight(60);
            owlIcon.setFitWidth(60);
            owlHeader.getChildren().add(owlIcon);
        } catch (Exception e) {
            Label fallback = new Label("");
            fallback.setFont(Font.font("Arial", FontWeight.BOLD, 40));
            owlHeader.getChildren().add(fallback);
        }

        VBox welcomeText = new VBox(5);
        Label greeting = new Label("Hello, Coder!");
        greeting.setFont(Font.font("Arial Rounded", FontWeight.BOLD, 20));
        greeting.setTextFill(Color.web(AppConstants.DEEP_TEAL));

        Label subGreeting = new Label("Let's learn and grow together 🌱");
        subGreeting.setFont(Font.font("Arial", 14));
        subGreeting.setTextFill(Color.web(AppConstants.MEDIUM_TEAL));

        welcomeText.getChildren().addAll(greeting, subGreeting);
        owlHeader.getChildren().add(welcomeText);

        Region spacerHeader = new Region();
        HBox.setHgrow(spacerHeader, javafx.scene.layout.Priority.ALWAYS);
        owlHeader.getChildren().add(spacerHeader);

        // ===== Hub Button =====
        Button hubBtn = new Button("Hub");
        hubBtn.setStyle(
                "-fx-background-color: " +"#fabb55" + ";" +
                        "-fx-text-fill: #ffffff;" +
                        "-fx-background-radius: 20;" +
                        "-fx-padding: 8 20;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;"
        );
        hubBtn.setOnAction(e -> {
            if (onOpenHub != null) onOpenHub.run();
        });
        owlHeader.getChildren().add(hubBtn);

        // ===== Stats row =====
        HBox stats = new HBox(20);
        stats.setAlignment(Pos.CENTER);

        VBox tasksDoneCard = statCard("Tasks Done Today", "0", AppConstants.GREEN);
        VBox activeTasksCard = statCard("Active Tasks", "0", AppConstants.YELLOW);
        VBox streakCard = statCard("Current Streak", "0", AppConstants.PEACH);
        VBox totalSolvedCard = statCard("Total Solved", "0", AppConstants.MAUVE);

        Label tasksDoneVal = (Label) tasksDoneCard.getChildren().get(0);
        tasksDoneVal.textProperty().bind(taskCtrl.completedTodayProperty().asString());

        Label activeTasksVal = (Label) activeTasksCard.getChildren().get(0);
        activeTasksVal.textProperty().bind(taskCtrl.activeTasksProperty().asString());

        stats.getChildren().addAll(tasksDoneCard, activeTasksCard, streakCard, totalSolvedCard);

        // ===== Sections =====
        VBox todayTasks = buildTodayTasksSection(taskCtrl);
        VBox upcoming = buildUpcomingProjectsSection(calendarCtrl);

        dashboard.getChildren().addAll(owlHeader, stats, todayTasks, upcoming);

        return root;
    }

    private VBox buildTodayTasksSection(TaskController taskCtrl) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setStyle("-fx-background-color: white;");

        Label header = new Label("📋 Today's Tasks");
        header.setFont(Font.font("Arial Rounded", FontWeight.BOLD, 20));
        header.setTextFill(Color.web(AppConstants.DEEP_TEAL));

        VBox list = new VBox(8);
        list.setPadding(new Insets(8, 0, 0, 0));

        Runnable refresh = () -> {
            list.getChildren().clear();
            if (taskCtrl.getTodayTasks().isEmpty()) {
                Label empty = new Label("No tasks due today – add some to get started!");
                empty.setTextFill(Color.web(AppConstants.SUBTEXT1));
                empty.setFont(Font.font("Arial", 14));
                list.getChildren().add(empty);
            } else {
                for (Task t : taskCtrl.getTodayTasks()) {
                    list.getChildren().add(taskRow(t, taskCtrl));
                }
            }
        };

        taskCtrl.getTodayTasks().addListener((javafx.collections.ListChangeListener<Task>) c -> refresh.run());
        refresh.run();

        card.getChildren().addAll(header, list);
        return card;
    }

    private HBox taskRow(Task task, TaskController ctrl) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8));
        row.setStyle("-fx-background-color: " + AppConstants.LIGHT_TEAL + "; -fx-background-radius: 30;");

        CheckBox cb = new CheckBox();
        cb.setSelected(task.isCompleted());
        cb.setOnAction(e -> {
            task.setCompleted(cb.isSelected());
            ctrl.saveTask(task);
        });

        Label titleLbl = new Label(task.getTitle());
        titleLbl.setTextFill(Color.web(AppConstants.DEEP_TEAL));
        titleLbl.setFont(Font.font("Arial Rounded", 14));
        if (task.isCompleted()) {
            titleLbl.setStyle("-fx-strikethrough: true; -fx-text-fill: " + AppConstants.OVERLAY0 + ";");
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label prio = new Label(task.getPriority().toString());
        prio.setStyle(
                "-fx-background-color: " + priorityColor(task.getPriority()) + ";" +
                        "-fx-padding: 4 12;" +
                        "-fx-background-radius: 20;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: bold;"
        );

        row.getChildren().addAll(cb, titleLbl, spacer, prio);
        return row;
    }

    private VBox buildUpcomingProjectsSection(CalendarController calendarCtrl) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setStyle("-fx-background-color: white;");

        Label header = new Label("🗓 Upcoming Projects (7 days)");
        header.setFont(Font.font("Arial Rounded", FontWeight.BOLD, 20));
        header.setTextFill(Color.web(AppConstants.DEEP_TEAL));

        VBox list = new VBox(8);
        list.setPadding(new Insets(8, 0, 0, 0));

        refreshUpcomingProjects(list, calendarCtrl);

        card.getChildren().addAll(header, list);
        return card;
    }

    private void refreshUpcomingProjects(VBox list, CalendarController calendarCtrl) {
        list.getChildren().clear();
        var projects = calendarCtrl.getUpcomingProjects(7);
        if (projects.isEmpty()) {
            Label empty = new Label("No projects due in the next 7 days.");
            empty.setTextFill(Color.web(AppConstants.SUBTEXT1));
            empty.setFont(Font.font("Arial", 14));
            list.getChildren().add(empty);
        } else {
            for (Project p : projects) {
                list.getChildren().add(projectRow(p));
            }
        }
    }

    private HBox projectRow(Project project) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10));
        row.setStyle("-fx-background-color: " + AppConstants.LIGHT_TEAL + "; -fx-background-radius: 30;");

        VBox dateBadge = new VBox(2);
        dateBadge.setAlignment(Pos.CENTER);
        dateBadge.setStyle("-fx-background-color: white; -fx-padding: 6; -fx-background-radius: 15;");

        Label day = new Label(project.getDueDate().format(DateTimeFormatter.ofPattern("dd")));
        day.setFont(Font.font("Arial Rounded", FontWeight.BOLD, 18));
        day.setTextFill(Color.web(AppConstants.DEEP_TEAL));

        Label month = new Label(project.getDueDate().format(DateTimeFormatter.ofPattern("MMM")));
        month.setFont(Font.font("Arial", 10));
        month.setTextFill(Color.web(AppConstants.MEDIUM_TEAL));

        dateBadge.getChildren().addAll(day, month);

        VBox details = new VBox(3);
        Label titleLbl = new Label(project.getTitle());
        titleLbl.setFont(Font.font("Arial Rounded", FontWeight.BOLD, 14));
        titleLbl.setTextFill(Color.web(AppConstants.DEEP_TEAL));

        Label descLbl = new Label(project.getDescription());
        descLbl.setFont(Font.font("Arial", 11));
        descLbl.setTextFill(Color.web(AppConstants.SUBTEXT1));
        descLbl.setWrapText(true);

        details.getChildren().addAll(titleLbl, descLbl);

        row.getChildren().addAll(dateBadge, details);
        return row;
    }

    private VBox statCard(String title, String value, String accent) {
        VBox card = new VBox(6);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-padding: 20;" +
                        "-fx-background-radius: 30;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2);"
        );
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(200);

        Label valLbl = new Label(value);
        valLbl.setFont(Font.font("Arial Rounded", FontWeight.BOLD, 28));
        valLbl.setTextFill(Color.web(accent));

        Label titleLbl = new Label(title);
        titleLbl.setFont(Font.font("Arial", 13));
        titleLbl.setTextFill(Color.web(AppConstants.SUBTEXT1));
        titleLbl.setWrapText(true);
        titleLbl.setAlignment(Pos.CENTER);

        card.getChildren().addAll(valLbl, titleLbl);
        return card;
    }

    private String priorityColor(Priority p) {
        return switch (p) {
            case HIGH -> AppConstants.RED;
            case MEDIUM -> AppConstants.YELLOW;
            case LOW -> AppConstants.GREEN;
        };
    }
}