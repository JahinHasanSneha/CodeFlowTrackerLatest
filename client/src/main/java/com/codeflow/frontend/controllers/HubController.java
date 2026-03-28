package com.codeflow.frontend.controllers;

import com.codeflow.shared.ApiClient;
import com.codeflow.shared.AppConstants;
import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.*;
import java.util.function.Consumer;

public class HubController {

    private static final int MAX_COURSES   = 12;
    private static final int CARDS_PER_PAGE = 6;

    private int currentPage = 0;

    private final String[] cardImages = {
            "/images/card1.jpeg",  "/images/card2.jpeg",  "/images/card3.jpeg",
            "/images/card4.jpeg",  "/images/card5.jpeg",  "/images/card6.jpeg",
            "/images/card7.jpeg",  "/images/card8.jpeg",  "/images/card9.jpeg",
            "/images/card10.jpeg", "/images/card11.jpeg", "/images/card12.jpeg"
    };

    // Images not yet assigned to a course
    private final List<String> imagePool = new ArrayList<>();

    // ── Model ──────────────────────────────────────────────────────────────
    public static class Course {
        public final String code;
        public final String name;
        public final String instructor;
        public final String imagePath;

        public Course(String code, String name, String instructor, String imagePath) {
            this.code       = code;
            this.name       = name;
            this.instructor = instructor;
            this.imagePath  = imagePath;
        }
    }

    private final ObservableList<Course> courses = FXCollections.observableArrayList();
    private GridPane grid;
    private Consumer<Course> openCourseCallback;

    // ── View ───────────────────────────────────────────────────────────────
    public Node getView(Runnable onBack, Consumer<Course> onOpenCourse) {
        this.openCourseCallback = onOpenCourse;

        // Build full pool; remove images already used by loaded courses later
        imagePool.clear();
        imagePool.addAll(Arrays.asList(cardImages));
        Collections.shuffle(imagePool);

        StackPane root = new StackPane();
        root.setStyle(
                "-fx-background-image: url('/images/GreyBg.png');" +
                "-fx-background-size: cover;" +
                "-fx-background-position: center center;"
        );

        VBox layout = new VBox(24);
        layout.setPadding(new Insets(32));
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setMaxWidth(1100);

        // Header
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle(
                "-fx-background-color: rgba(255,255,255,0.85);" +
                "-fx-background-radius: 30;" +
                "-fx-padding: 12 20;"
        );

        try {
            ImageView owl = new ImageView(new Image(getClass().getResourceAsStream("/images/owl1.png")));
            owl.setFitWidth(55); owl.setFitHeight(55);
            header.getChildren().add(owl);
        } catch (Exception ignored) {}

        Label title = new Label("Hub");
        title.setFont(Font.font("Arial Rounded", FontWeight.BOLD, 22));
        title.setTextFill(Color.web(AppConstants.DEEP_TEAL));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button add = new Button("➕ Add Course");
        add.setStyle(buttonStyle());
        add.setOnAction(e -> showAddDialog());

        Button back = new Button("⬅ Back");
        back.setStyle(ghostStyle());
        back.setOnAction(e -> { if (onBack != null) onBack.run(); });

        header.getChildren().addAll(title, spacer, add, back);

        // White card box
        VBox cardBox = new VBox(15);
        cardBox.setAlignment(Pos.TOP_CENTER);
        cardBox.setMaxWidth(760);
        cardBox.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 30;" +
                "-fx-padding: 20;"
        );

        Label section = new Label("Your Courses");
        section.setFont(Font.font("Arial Rounded", FontWeight.BOLD, 20));
        section.setTextFill(Color.web(AppConstants.DEEP_TEAL));
        section.setAlignment(Pos.CENTER);
        section.setMaxWidth(Double.MAX_VALUE);

        grid = new GridPane();
        grid.setHgap(30);
        grid.setVgap(30);
        grid.setAlignment(Pos.CENTER);

        // Pagination
        HBox pagination = new HBox(12);
        pagination.setAlignment(Pos.CENTER);

        Button prev = new Button("⬅");
        Button next = new Button("➡");
        prev.setStyle(buttonStyle());
        next.setStyle(buttonStyle());

        prev.setOnAction(e -> { if (currentPage > 0) { currentPage--; refreshGrid(); } });
        next.setOnAction(e -> {
            if ((currentPage + 1) * CARDS_PER_PAGE < courses.size()) { currentPage++; refreshGrid(); }
        });
        pagination.getChildren().addAll(prev, next);

        cardBox.getChildren().addAll(section, grid, pagination);
        layout.getChildren().addAll(header, cardBox);
        root.getChildren().add(layout);

        // Show loading state then fetch from backend
        showLoadingGrid();
        loadCoursesFromServer();

        return root;
    }

    // ── Load from server ───────────────────────────────────────────────────
    private void loadCoursesFromServer() {
        new Thread(() -> {
            try {
                var resp = ApiClient.get("/courses");
                if (resp.statusCode() == 200) {
                    JsonNode arr = ApiClient.MAPPER.readTree(resp.body());
                    Platform.runLater(() -> {
                        courses.clear();
                        imagePool.clear();
                        imagePool.addAll(Arrays.asList(cardImages));

                        Set<String> usedImages = new HashSet<>();
                        List<Course> loaded = new ArrayList<>();

                        for (JsonNode n : arr) {
                            String img = n.path("imagePath").asText();
                            loaded.add(new Course(
                                    n.path("code").asText(),
                                    n.path("name").asText(),
                                    n.path("instructor").asText(""),
                                    img
                            ));
                            usedImages.add(img);
                        }
                        // Remove used images from pool so new courses get unique ones
                        imagePool.removeAll(usedImages);
                        Collections.shuffle(imagePool);

                        courses.addAll(loaded);
                        refreshGrid();
                    });
                } else {
                    Platform.runLater(this::refreshGrid);
                }
            } catch (Exception e) {
                Platform.runLater(this::refreshGrid);
            }
        }).start();
    }

    // ── Grid ───────────────────────────────────────────────────────────────
    private void showLoadingGrid() {
        if (grid == null) return;
        grid.getChildren().clear();
        Label lbl = new Label("Loading courses...");
        lbl.setTextFill(Color.web(AppConstants.SUBTEXT1));
        grid.add(lbl, 0, 0);
    }

    private void refreshGrid() {
        if (grid == null) return;
        grid.getChildren().clear();

        int start = currentPage * CARDS_PER_PAGE;
        for (int i = 0; i < CARDS_PER_PAGE; i++) {
            int index = start + i;
            Node node = (index < courses.size())
                    ? courseCard(courses.get(index))
                    : placeholderCard(i);
            grid.add(node, i % 3, i / 3);
        }
    }

    // ── Course card ────────────────────────────────────────────────────────
    private VBox courseCard(Course c) {
        VBox card = new VBox();
        card.setPrefSize(220, 340);
        card.setCursor(Cursor.HAND);
        card.setStyle(
                "-fx-background-radius: 20;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 18, 0, 0, 6);"
        );

        // Image section with ✕ delete overlay
        StackPane imageSection = new StackPane();
        imageSection.setPrefHeight(255);

        ImageView bg = loadImage(c.imagePath);
        bg.setFitWidth(220);
        bg.setFitHeight(255);
        bg.setPreserveRatio(false);

        // Delete button overlay (top-right)
        Button deleteBtn = new Button("✕");
        deleteBtn.setStyle(
                "-fx-background-color: rgba(220,50,50,0.85);" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 50%;" +
                "-fx-padding: 2 7;" +
                "-fx-font-size: 12;" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;"
        );
        deleteBtn.setOnAction(e -> {
            e.consume();
            confirmDeleteCourse(c);
        });
        StackPane.setAlignment(deleteBtn, Pos.TOP_RIGHT);
        StackPane.setMargin(deleteBtn, new Insets(8, 8, 0, 0));

        imageSection.getChildren().addAll(bg, deleteBtn);

        // Text section
        VBox textSection = new VBox(6);
        textSection.setPadding(new Insets(12));
        textSection.setPrefHeight(85);
        textSection.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 20 20;");

        Label code = new Label(c.code);
        code.setFont(Font.font("Arial Rounded", FontWeight.BOLD, 14));
        code.setTextFill(Color.web(AppConstants.DEEP_TEAL));

        Label name = new Label(c.name);
        name.setWrapText(true);

        Label instructor = new Label(c.instructor);
        instructor.setTextFill(Color.GRAY);

        textSection.getChildren().addAll(code, name, instructor);
        card.getChildren().addAll(imageSection, textSection);

        // Open course on click (but not on delete button — consumed above)
        card.setOnMouseClicked(e -> {
            if (openCourseCallback != null) openCourseCallback.accept(c);
        });

        return card;
    }

    private void confirmDeleteCourse(Course c) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete \"" + c.code + " — " + c.name + "\"?\nAll notes, resources, slides and tasks will also be deleted.",
                ButtonType.YES, ButtonType.CANCEL);
        confirm.setTitle("Delete Course");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) deleteCourseOnServer(c);
        });
    }

    private void deleteCourseOnServer(Course c) {
        new Thread(() -> {
            try {
                String encoded = java.net.URLEncoder.encode(c.code, "UTF-8").replace("+", "%20");
                ApiClient.delete("/courses/" + encoded);
                Platform.runLater(() -> {
                    imagePool.add(c.imagePath); // return image to pool
                    courses.remove(c);
                    if (currentPage > 0 && currentPage * CARDS_PER_PAGE >= courses.size()) currentPage--;
                    refreshGrid();
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                        new Alert(Alert.AlertType.ERROR, "Could not delete course: " + e.getMessage()).showAndWait()
                );
            }
        }).start();
    }

    // ── Placeholder card ───────────────────────────────────────────────────
    private VBox placeholderCard(int index) {
        VBox card = new VBox();
        card.setPrefSize(220, 340);
        card.setCursor(Cursor.HAND);

        StackPane imageSection = new StackPane();
        imageSection.setPrefHeight(255);

        String img = cardImages[index % cardImages.length];
        ImageView bg = loadImage(img);
        bg.setFitWidth(220);
        bg.setFitHeight(255);
        bg.setPreserveRatio(false);
        imageSection.getChildren().add(bg);

        VBox textSection = new VBox();
        textSection.setPrefHeight(85);
        textSection.setAlignment(Pos.CENTER);
        textSection.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 20 20;");

        Label label = new Label("Add Course");
        label.setFont(Font.font("Arial Rounded", FontWeight.BOLD, 14));
        label.setTextFill(Color.web(AppConstants.DEEP_TEAL));
        textSection.getChildren().add(label);

        card.getChildren().addAll(imageSection, textSection);
        card.setOnMouseClicked(e -> showAddDialog());
        return card;
    }

    // ── Add course dialog ──────────────────────────────────────────────────
    private void showAddDialog() {
        if (courses.size() >= MAX_COURSES) {
            new Alert(Alert.AlertType.INFORMATION, "Maximum " + MAX_COURSES + " courses allowed.").showAndWait();
            return;
        }

        Dialog<Course> dialog = new Dialog<>();
        dialog.setTitle("Add Course");

        ButtonType addType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addType, ButtonType.CANCEL);

        GridPane gp = new GridPane();
        gp.setHgap(10); gp.setVgap(10);
        gp.setPadding(new Insets(15));

        TextField codeField       = new TextField();
        TextField nameField       = new TextField();
        TextField instructorField = new TextField();
        codeField.setPromptText("e.g. CS101");
        nameField.setPromptText("e.g. Data Structures");
        instructorField.setPromptText("e.g. Dr. Smith");

        gp.addRow(0, new Label("Code:"),       codeField);
        gp.addRow(1, new Label("Name:"),       nameField);
        gp.addRow(2, new Label("Instructor:"), instructorField);
        dialog.getDialogPane().setContent(gp);

        dialog.setResultConverter(btn -> {
            if (btn != addType) return null;
            String code       = codeField.getText().trim();
            String name       = nameField.getText().trim();
            String instructor = instructorField.getText().trim();
            if (code.isEmpty() || name.isEmpty()) return null;
            String img = imagePool.isEmpty()
                    ? cardImages[(int)(Math.random() * cardImages.length)]
                    : imagePool.remove(0);
            return new Course(code, name, instructor, img);
        });

        dialog.showAndWait().ifPresent(c -> {
            if (c == null) return;
            saveCourseOnServer(c);
        });
    }

    private void saveCourseOnServer(Course c) {
        new Thread(() -> {
            try {
                Map<String, String> body = new java.util.LinkedHashMap<>();
                body.put("code",       c.code);
                body.put("name",       c.name);
                body.put("instructor", c.instructor);
                body.put("imagePath",  c.imagePath);
                var resp = ApiClient.post("/courses", body);
                if (resp.statusCode() == 200) {
                    Platform.runLater(() -> {
                        courses.add(c);
                        currentPage = (courses.size() - 1) / CARDS_PER_PAGE;
                        refreshGrid();
                    });
                } else {
                    Platform.runLater(() ->
                            new Alert(Alert.AlertType.ERROR, "Could not save course.").showAndWait()
                    );
                }
            } catch (Exception e) {
                Platform.runLater(() ->
                        new Alert(Alert.AlertType.ERROR, "Connection error: " + e.getMessage()).showAndWait()
                );
            }
        }).start();
    }

    // ── Helpers ────────────────────────────────────────────────────────────
    private ImageView loadImage(String path) {
        try {
            return new ImageView(new Image(getClass().getResourceAsStream(path)));
        } catch (Exception e) {
            return new ImageView();
        }
    }

    private String buttonStyle() {
        return "-fx-background-color: " + AppConstants.DEEP_TEAL + ";" +
                "-fx-text-fill: white; -fx-background-radius: 20;" +
                "-fx-padding: 8 16; -fx-font-weight: bold;";
    }

    private String ghostStyle() {
        return "-fx-background-color: rgba(0,0,0,0.08);" +
                "-fx-text-fill: " + AppConstants.DEEP_TEAL + ";" +
                "-fx-background-radius: 20; -fx-padding: 8 16; -fx-font-weight: bold;";
    }
}
