package com.codeflow.frontend.controllers;

import com.codeflow.shared.ApiClient;
import com.codeflow.shared.AppConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.awt.Desktop;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class CoursePageController {

    public static class Course {
        public final String code;
        public final String name;
        public final String instructor;
        public final String imagePath;

        public Course(String code, String name, String instructor, String imagePath) {
            this.code = code;
            this.name = name;
            this.instructor = instructor;
            this.imagePath = imagePath;
        }
    }

    private String activeTab = "notes";
    private VBox contentPane;
    private Map<String, Button> tabBtns = new HashMap<>();

    public Node getView(Course course, Runnable onBack) {
        StackPane root = new StackPane();
        root.setStyle(
                "-fx-background-image: url('" + course.imagePath + "');" +
                        "-fx-background-size: cover;" +
                        "-fx-background-position: center center;" +
                        "-fx-background-repeat: no-repeat;"
        );

        VBox layout = new VBox(16);
        layout.setPadding(new Insets(32));
        layout.setMaxWidth(1100);
        layout.setAlignment(Pos.TOP_CENTER);

        // ===== Header =====
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: rgba(255,255,255,0.9); -fx-background-radius: 30; -fx-padding: 14 20;");

        try {
            ImageView owl = new ImageView(new Image(getClass().getResourceAsStream("/images/owl1.png")));
            owl.setFitHeight(50); owl.setFitWidth(50);
            header.getChildren().add(owl);
        } catch (Exception ignored) {}

        Button back = new Button("⬅ Back");
        back.setOnAction(e -> onBack.run());
        back.setStyle("-fx-background-color: rgba(0,0,0,0.08); -fx-text-fill: " + AppConstants.DEEP_TEAL +
                "; -fx-background-radius: 18; -fx-padding: 8 14; -fx-font-weight: bold;");

        VBox titleBox = new VBox(4);
        Label title = new Label(course.code + " — " + course.name);
        title.setFont(Font.font("Arial Rounded", FontWeight.BOLD, 20));
        title.setTextFill(Color.web(AppConstants.DEEP_TEAL));
        Label teacher = new Label("  " + course.instructor);
        teacher.setTextFill(Color.web(AppConstants.SUBTEXT1));
        titleBox.getChildren().addAll(title, teacher);
        header.getChildren().addAll(back, titleBox);

        // ===== Tab Bar =====
        HBox tabBar = new HBox(10);
        tabBar.setAlignment(Pos.CENTER_LEFT);
        tabBar.setStyle("-fx-background-color: rgba(255,255,255,0.85); -fx-background-radius: 24; -fx-padding: 10 16;");

        String[] tabs      = {"notes", "resources", "slides", "tasks"};
        String[] tabLabels = {"📝 Notes", "📂 Resources", "🖥 Slides", "✅ Tasks"};

        for (int i = 0; i < tabs.length; i++) {
            final String tabKey = tabs[i];
            Button tb = new Button(tabLabels[i]);
            styleTabBtn(tb, tabKey.equals(activeTab));
            tb.setOnAction(e -> {
                activeTab = tabKey;
                tabBtns.forEach((k, b) -> styleTabBtn(b, k.equals(tabKey)));
                refreshContent(course);
            });
            tabBtns.put(tabKey, tb);
            tabBar.getChildren().add(tb);
        }

        Button addBtn = new Button("＋ Add");
        addBtn.setStyle("-fx-background-color: " + AppConstants.DEEP_TEAL + "; -fx-text-fill: white;" +
                " -fx-background-radius: 18; -fx-padding: 8 18; -fx-font-weight: bold; -fx-cursor: hand;");
        addBtn.setOnAction(e -> showAddDialog(course));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        tabBar.getChildren().addAll(spacer, addBtn);

        // ===== Content Pane =====
        contentPane = new VBox(10);
        contentPane.setStyle("-fx-background-color: rgba(255,255,255,0.88); -fx-background-radius: 20; -fx-padding: 18;");
        contentPane.setMinHeight(300);

        layout.getChildren().addAll(header, tabBar, contentPane);
        root.getChildren().add(layout);

        refreshContent(course);
        return root;
    }

    private void styleTabBtn(Button b, boolean active) {
        if (active) {
            b.setStyle("-fx-background-color: " + AppConstants.DEEP_TEAL + "; -fx-text-fill: white;" +
                    " -fx-background-radius: 16; -fx-padding: 7 16; -fx-font-weight: bold; -fx-cursor: hand;");
        } else {
            b.setStyle("-fx-background-color: transparent; -fx-text-fill: " + AppConstants.DEEP_TEAL + ";" +
                    " -fx-background-radius: 16; -fx-padding: 7 16; -fx-font-weight: bold; -fx-cursor: hand;");
        }
    }

    private void refreshContent(Course course) {
        if (contentPane == null) return;
        contentPane.getChildren().clear();
        Label loading = new Label("Loading...");
        loading.setTextFill(Color.web(AppConstants.SUBTEXT1));
        contentPane.getChildren().add(loading);

        new Thread(() -> {
            try {
                var resp = ApiClient.get("/courses/" + encode(course.code));
                if (resp.statusCode() == 200) {
                    JsonNode doc = ApiClient.MAPPER.readTree(resp.body());
                    Platform.runLater(() -> renderTab(doc, course));
                } else {
                    Platform.runLater(() -> showError("Failed to load data (status " + resp.statusCode() + ")"));
                }
            } catch (Exception ex) {
                Platform.runLater(() -> showError("Connection error: " + ex.getMessage()));
            }
        }).start();
    }

    private void renderTab(JsonNode doc, Course course) {
        contentPane.getChildren().clear();
        JsonNode raw = doc.get(activeTab);
        if (!(raw instanceof ArrayNode) || raw.size() == 0) {
            Label empty = new Label("No " + activeTab + " yet. Use ＋ Add to add one.");
            empty.setTextFill(Color.web(AppConstants.SUBTEXT1));
            empty.setFont(Font.font("Arial Rounded", 14));
            contentPane.getChildren().add(empty);
            return;
        }
        for (JsonNode item : (ArrayNode) raw) {
            contentPane.getChildren().add(buildItemRow(item, course));
        }
    }

    private HBox buildItemRow(JsonNode item, Course course) {
        String id = item.path("id").asText();
        boolean completed = item.path("completed").asBoolean(false);

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 14, 10, 14));
        row.setStyle("-fx-background-color: " + (completed ? "rgba(180,230,180,0.6)" : "rgba(245,245,250,0.9)") +
                "; -fx-background-radius: 14;");

        VBox content = new VBox(3);
        HBox.setHgrow(content, Priority.ALWAYS);

        switch (activeTab) {
            case "notes": {
                Label lbl = new Label(item.path("text").asText());
                lbl.setWrapText(true);
                lbl.setFont(Font.font("Arial", 14));
                if (completed) lbl.setStyle("-fx-strikethrough: true; -fx-text-fill: #888;");
                content.getChildren().add(lbl);
                break;
            }
            case "resources":
            case "slides": {
                String name = item.path("name").asText();
                String link = item.path("link").asText();

                Label nameLbl = new Label(name.isEmpty() ? link : name);
                nameLbl.setFont(Font.font("Arial", FontWeight.BOLD, 14));
                if (completed) nameLbl.setStyle("-fx-strikethrough: true; -fx-text-fill: #888;");

                content.getChildren().add(nameLbl);
                if (!link.isEmpty()) {
                    Button openBtn = new Button("🔗 Open");
                    openBtn.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white;" +
                            " -fx-background-radius: 10; -fx-padding: 3 10; -fx-font-size: 12; -fx-cursor: hand;");
                    openBtn.setOnAction(e -> openLink(link));
                    content.getChildren().add(openBtn);
                }
                break;
            }
            case "tasks": {
                Label lbl = new Label(item.path("title").asText());
                lbl.setFont(Font.font("Arial", 14));
                if (completed) lbl.setStyle("-fx-strikethrough: true; -fx-text-fill: #888;");
                content.getChildren().add(lbl);
                break;
            }
        }

        Button toggleBtn;
        if (!completed) {
            toggleBtn = new Button("✓ Done");
            toggleBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 12;" +
                    " -fx-padding: 5 12; -fx-cursor: hand; -fx-font-weight: bold;");
        } else {
            toggleBtn = new Button("↩ Undo");
            toggleBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-background-radius: 12;" +
                    " -fx-padding: 5 12; -fx-cursor: hand; -fx-font-weight: bold;");
        }
        final boolean finalCompleted = completed;
        toggleBtn.setOnAction(e -> toggleComplete(course, id, !finalCompleted));

        Button deleteBtn = new Button("🗑");
        deleteBtn.setStyle("-fx-background-color: #e53935; -fx-text-fill: white; -fx-background-radius: 12;"
                + " -fx-padding: 5 10; -fx-cursor: hand; -fx-font-size: 12;");
        deleteBtn.setOnAction(e -> deleteItem(course, id));

        row.getChildren().addAll(content, toggleBtn, deleteBtn);
        return row;
    }

    private void toggleComplete(Course course, String itemId, boolean complete) {
        String action = complete ? "complete" : "uncomplete";
        new Thread(() -> {
            try {
                ApiClient.patch("/courses/" + encode(course.code) + "/" + activeTab + "/" + itemId + "/" + action);
                Platform.runLater(() -> refreshContent(course));
            } catch (Exception ex) {
                Platform.runLater(() -> showError("Could not update item."));
            }
        }).start();
    }

    private void deleteItem(Course course, String itemId) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete this item?", javafx.scene.control.ButtonType.YES, javafx.scene.control.ButtonType.CANCEL);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn != javafx.scene.control.ButtonType.YES) return;
            new Thread(() -> {
                try {
                    ApiClient.delete("/courses/" + encode(course.code) + "/" + activeTab + "/" + itemId);
                    Platform.runLater(() -> refreshContent(course));
                } catch (Exception ex) {
                    Platform.runLater(() -> showError("Could not delete item."));
                }
            }).start();
        });
    }

    private void showAddDialog(Course course) {
        switch (activeTab) {
            case "notes":     showNotesDialog(course); break;
            case "resources": showLinkDialog(course, "Resource"); break;
            case "slides":    showLinkDialog(course, "Slide"); break;
            case "tasks":     showTaskDialog(course); break;
        }
    }

    private void showNotesDialog(Course course) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Add Note");
        dialog.setHeaderText("Write your note:");
        TextArea area = new TextArea();
        area.setPromptText("Enter note text...");
        area.setPrefRowCount(5);
        area.setWrapText(true);
        DialogPane pane = dialog.getDialogPane();
        pane.setContent(area);
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(btn -> btn == ButtonType.OK ? area.getText().trim() : null);
        dialog.showAndWait().ifPresent(text -> {
            if (text.isEmpty()) return;
            Map<String, String> body = new HashMap<>();
            body.put("text", text);
            saveItem(course, "notes", body);
        });
    }

    private void showLinkDialog(Course course, String typeName) {
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Add " + typeName);

        TextField nameField = new TextField();
        nameField.setPromptText(typeName + " name (e.g. Chapter 1)");
        TextField linkField = new TextField();
        linkField.setPromptText("Paste URL / link");

        VBox content = new VBox(8,
                new Label(typeName + " Name:"), nameField,
                new Label("Link:"), linkField);
        content.setPadding(new Insets(10));

        DialogPane pane = dialog.getDialogPane();
        pane.setContent(content);
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(btn -> btn == ButtonType.OK
                ? new String[]{nameField.getText().trim(), linkField.getText().trim()} : null);

        dialog.showAndWait().ifPresent(arr -> {
            if (arr[0].isEmpty() && arr[1].isEmpty()) return;
            Map<String, String> body = new HashMap<>();
            body.put("name", arr[0]);
            body.put("link", arr[1]);
            saveItem(course, activeTab, body);
        });
    }

    private void showTaskDialog(Course course) {
        TextInputDialog d = new TextInputDialog();
        d.setTitle("Add Task");
        d.setHeaderText("Task title:");
        d.showAndWait().ifPresent(text -> {
            if (text.trim().isEmpty()) return;
            Map<String, String> body = new HashMap<>();
            body.put("title", text.trim());
            saveItem(course, "tasks", body);
        });
    }

    private void saveItem(Course course, String type, Map<String, String> body) {
        new Thread(() -> {
            try {
                ApiClient.post("/courses/" + encode(course.code) + "/" + type, body);
                Platform.runLater(() -> refreshContent(course));
            } catch (Exception ex) {
                Platform.runLater(() -> showError("Could not save item."));
            }
        }).start();
    }

    private void openLink(String link) {
        try {
            if (!link.startsWith("http://") && !link.startsWith("https://")) link = "https://" + link;
            Desktop.getDesktop().browse(new URI(link));
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Could not open link:\n" + e.getMessage()).showAndWait();
        }
    }

    private void showError(String msg) {
        if (contentPane != null) {
            Label err = new Label("⚠ " + msg);
            err.setTextFill(Color.RED);
            contentPane.getChildren().setAll(err);
        }
    }

    private String encode(String s) {
        try { return java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20"); }
        catch (Exception e) { return s; }
    }
}