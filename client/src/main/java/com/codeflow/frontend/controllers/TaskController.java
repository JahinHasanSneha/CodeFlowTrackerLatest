package com.codeflow.frontend.controllers;

import com.codeflow.api.models.Priority;
import com.codeflow.api.models.Task;
import com.codeflow.api.services.TaskService;
import com.codeflow.shared.AppConstants;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class TaskController {

    private final TaskService taskService = new TaskService();
    private final ObservableList<Task> tasks = FXCollections.observableArrayList();

    private final IntegerProperty completedToday = new SimpleIntegerProperty(0);
    private final IntegerProperty activeTasks    = new SimpleIntegerProperty(0);
    private final ObservableList<Task> todayTasks = FXCollections.observableArrayList();

    private ListView<Task> taskListView;

    public TaskController() { loadTasksAsync(); }

    public ObservableList<Task> getTodayTasks() { return todayTasks; }
    public int getCompletedToday()              { return completedToday.get(); }
    public IntegerProperty completedTodayProperty() { return completedToday; }
    public int getActiveTasks()                 { return activeTasks.get(); }
    public IntegerProperty activeTasksProperty()    { return activeTasks; }

    public void saveTask(Task t) {
        taskService.updateTask(t);
        if (taskListView != null) taskListView.refresh();
        refreshStats();
    }

    private void refreshStats() {
        long completedTodayCount = tasks.stream()
                .filter(t -> t.isCompleted() && t.getDueDate() != null && t.getDueDate().equals(LocalDate.now()))
                .count();
        completedToday.set((int) completedTodayCount);
        activeTasks.set((int) tasks.stream().filter(t -> !t.isCompleted()).count());
        todayTasks.setAll(tasks.stream()
                .filter(t -> t.getDueDate() != null && t.getDueDate().equals(LocalDate.now()))
                .collect(Collectors.toList()));
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

        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setMaxWidth(1100);
        content.setAlignment(Pos.TOP_CENTER);
        root.getChildren().add(content);

        // ===== Task Management Header =====
        HBox taskHeader = new HBox(15);
        taskHeader.setAlignment(Pos.CENTER_LEFT);
        taskHeader.setPadding(new Insets(18, 28, 18, 28));
        taskHeader.setStyle("-fx-background-color: rgba(255,255,255,0.93); -fx-background-radius: 22;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 12, 0, 0, 4);");

        Label taskIcon = new Label("✅");
        taskIcon.setFont(Font.font("Arial", 32));

        VBox titleBox = new VBox(4);
        Label title = new Label("Task Manager");
        title.setFont(Font.font("Arial Rounded", FontWeight.BOLD, 24));
        title.setTextFill(Color.web(AppConstants.DEEP_TEAL));
        Label sub = new Label("Organize, track, and conquer your tasks with Hoot! 💛");
        sub.setFont(Font.font("Arial", 13));
        sub.setTextFill(Color.web(AppConstants.MEDIUM_TEAL));
        titleBox.getChildren().addAll(title, sub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button addBtn = new Button("➕ New Task");
        addBtn.setStyle("-fx-background-color: " + AppConstants.DEEP_TEAL + "; -fx-text-fill: white;" +
                        "-fx-background-radius: 10; -fx-padding: 10 22; -fx-font-weight: bold; -fx-cursor: hand;" +
                        "-fx-font-size: 14px;");
        addBtn.setOnMouseEntered(e -> addBtn.setStyle("-fx-background-color: " + AppConstants.MEDIUM_TEAL + "; -fx-text-fill: white;" +
                        "-fx-background-radius: 10; -fx-padding: 10 22; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 14px;"));
        addBtn.setOnMouseExited(e -> addBtn.setStyle("-fx-background-color: " + AppConstants.DEEP_TEAL + "; -fx-text-fill: white;" +
                        "-fx-background-radius: 10; -fx-padding: 10 22; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 14px;"));
        addBtn.setOnAction(e -> showAddDialog());

        taskHeader.getChildren().addAll(taskIcon, titleBox, spacer, addBtn);

        // ===== Stats row =====
        HBox statsRow = buildStatsRow();

        // ===== Filter Bar =====
        HBox filters = buildFilterBar();

        // ===== Task List =====
        taskListView = new ListView<>();
        taskListView.setCellFactory(lv -> new TaskCell());
        taskListView.setItems(tasks);
        taskListView.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(taskListView, javafx.scene.layout.Priority.ALWAYS);

        content.getChildren().addAll(taskHeader, statsRow, filters, taskListView);
        tasks.addListener((javafx.collections.ListChangeListener<Task>) c -> refreshStats());
        return root;
    }

    private HBox buildStatsRow() {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER);

        VBox active = statCard("📋", "Active", activeTasks.get() + "", "#E3F2FD", "#1565C0");
        activeTasks.addListener((obs, o, n) -> ((Label) ((VBox)active).getChildren().get(1)).setText(n.toString()));

        VBox completed = statCard("✅", "Done Today", completedToday.get() + "", "#E8F5E9", "#2E7D32");
        completedToday.addListener((obs, o, n) -> ((Label) ((VBox)completed).getChildren().get(1)).setText(n.toString()));

        row.getChildren().addAll(active, completed);
        return row;
    }

    private VBox statCard(String icon, String label, String value, String bg, String fg) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(14, 30, 14, 30));
        card.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 14;");

        Label iconLbl = new Label(icon + "  " + label);
        iconLbl.setFont(Font.font("Arial", 13));
        iconLbl.setTextFill(Color.web(fg));

        Label valLbl = new Label(value);
        valLbl.setFont(Font.font("Arial Rounded", FontWeight.BOLD, 28));
        valLbl.setTextFill(Color.web(fg));

        card.getChildren().addAll(iconLbl, valLbl);
        return card;
    }

    private void loadTasksAsync() {
        javafx.concurrent.Task<List<Task>> loadTask = new javafx.concurrent.Task<>() {
            @Override protected List<Task> call() { return taskService.getAllTasks(); }
        };
        loadTask.setOnSucceeded(e -> { tasks.setAll(loadTask.getValue()); refreshStats(); });
        loadTask.setOnFailed(e -> {
            Throwable ex = loadTask.getException();
            ex.printStackTrace();
            javafx.application.Platform.runLater(() ->
                new Alert(Alert.AlertType.ERROR, "Failed to load tasks: " + ex.getMessage()).showAndWait());
        });
        new Thread(loadTask).start();
    }

    private HBox buildFilterBar() {
        HBox bar = new HBox(10);
        bar.setPadding(new Insets(5, 0, 5, 0));
        ToggleGroup group = new ToggleGroup();
        RadioButton all = filterRadio("All", group);
        RadioButton today = filterRadio("Today", group);
        RadioButton active = filterRadio("Active", group);
        RadioButton completed = filterRadio("Completed", group);
        all.setSelected(true);

        all.setOnAction(e -> taskListView.setItems(tasks));
        today.setOnAction(e -> taskListView.setItems(
            FXCollections.observableArrayList(tasks.stream().filter(t -> t.isDueToday()).collect(Collectors.toList()))));
        active.setOnAction(e -> taskListView.setItems(
            FXCollections.observableArrayList(tasks.stream().filter(t -> !t.isCompleted()).collect(Collectors.toList()))));
        completed.setOnAction(e -> taskListView.setItems(
            FXCollections.observableArrayList(tasks.stream().filter(Task::isCompleted).collect(Collectors.toList()))));

        bar.getChildren().addAll(all, today, active, completed);
        return bar;
    }

    private RadioButton filterRadio(String text, ToggleGroup group) {
        RadioButton rb = new RadioButton(text);
        rb.setToggleGroup(group);
        rb.setStyle("-fx-text-fill: #4e0a7e; -fx-font-size: 13px;");
        return rb;
    }

    private void showAddDialog() {
        Dialog<Task> dialog = new Dialog<>();
        dialog.setTitle("Add New Task");
        dialog.setHeaderText("Create a task");

        ButtonType okType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));

        TextField titleF = new TextField(); titleF.setPromptText("Task title");
        TextArea descF = new TextArea(); descF.setPrefRowCount(3);
        ComboBox<Priority> prioC = new ComboBox<>();
        prioC.getItems().addAll(Priority.values()); prioC.setValue(Priority.MEDIUM);
        DatePicker dateP = new DatePicker(LocalDate.now());
        TextField catF = new TextField(); catF.setPromptText("e.g. LeetCode, Project");

        grid.add(new Label("Title:"), 0, 0); grid.add(titleF, 1, 0);
        grid.add(new Label("Description:"), 0, 1); grid.add(descF, 1, 1);
        grid.add(new Label("Priority:"), 0, 2); grid.add(prioC, 1, 2);
        grid.add(new Label("Due Date:"), 0, 3); grid.add(dateP, 1, 3);
        grid.add(new Label("Category:"), 0, 4); grid.add(catF, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(btn -> btn == okType
            ? new Task(titleF.getText(), descF.getText(), prioC.getValue(), dateP.getValue(), catF.getText())
            : null);

        dialog.showAndWait().ifPresent(task -> {
            if (taskService.addTask(task)) tasks.add(task);
            else showError("Failed to save task.");
        });
    }

    private void showError(String msg) { new Alert(Alert.AlertType.ERROR, msg).showAndWait(); }

    private class TaskCell extends ListCell<Task> {
        @Override
        protected void updateItem(Task task, boolean empty) {
            super.updateItem(task, empty);
            if (empty || task == null) { setGraphic(null); setStyle("-fx-background-color: transparent;"); return; }

            HBox cell = new HBox(15);
            cell.setPadding(new Insets(12));
            cell.setAlignment(Pos.CENTER_LEFT);
            cell.setStyle("-fx-background-color: rgba(255,255,255,0.95); -fx-background-radius: 16;" +
                          "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 6, 0, 0, 2);");

            CheckBox cb = new CheckBox();
            cb.setSelected(task.isCompleted());
            cb.setOnAction(e -> {
                task.setCompleted(cb.isSelected());
                taskService.updateTask(task);
                updateItem(task, false);
                refreshStats();
            });

            VBox info = new VBox(5);
            Label t = new Label(task.getTitle());
            t.setFont(Font.font("Arial Rounded", FontWeight.BOLD, 14));
            t.setTextFill(Color.web(task.isCompleted() ? AppConstants.OVERLAY0 : AppConstants.DEEP_TEAL));
            if (task.isCompleted()) t.setStyle("-fx-strikethrough: true;");

            Label d = new Label(task.getDescription());
            d.setFont(Font.font("Arial", 11));
            d.setTextFill(Color.web(AppConstants.SUBTEXT1));
            d.setWrapText(true);

            HBox meta = new HBox(10);
            Label cat = new Label("📁 " + task.getCategory());
            cat.setFont(Font.font("Arial", 10)); cat.setTextFill(Color.web(AppConstants.BLUE));
            Label due = new Label("📅 " + (task.getDueDate() != null ? task.getDueDate().toString() : "—"));
            due.setFont(Font.font("Arial", 10));
            due.setTextFill(Color.web(task.isOverdue() ? AppConstants.RED : AppConstants.GREEN));
            meta.getChildren().addAll(cat, due);

            info.getChildren().addAll(t, d, meta);
            HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);

            VBox right = new VBox(5); right.setAlignment(Pos.CENTER_RIGHT);
            Label prio = new Label(task.getPriority().toString());
            prio.setStyle("-fx-background-color: " + prioColor(task.getPriority()) + ";" +
                          "-fx-padding: 4 12; -fx-background-radius: 20;" +
                          "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px;");

            Button del = new Button("🗑");
            del.setStyle("-fx-background-color: " + AppConstants.RED + "; -fx-text-fill: white;" +
                         "-fx-background-radius: 20; -fx-padding: 5 10; -fx-cursor: hand;");
            del.setOnAction(e -> {
                if (taskService.deleteTask(task.getId())) { tasks.remove(task); refreshStats(); }
                else showError("Failed to delete task.");
            });

            right.getChildren().addAll(prio, del);
            cell.getChildren().addAll(cb, info, right);
            setGraphic(cell);
            setStyle("-fx-background-color: transparent; -fx-padding: 5;");
        }

        private String prioColor(Priority p) {
            return switch (p) { case HIGH -> AppConstants.RED; case MEDIUM -> AppConstants.YELLOW; case LOW -> AppConstants.GREEN; };
        }
    }
}
