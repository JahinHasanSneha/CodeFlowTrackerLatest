package com.codeflow.frontend.controllers;

import com.codeflow.api.models.LeetCodeProblem;
import com.codeflow.api.services.LeetCodeService;
import com.codeflow.shared.AppConstants;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class LeetCodeController {

    private final LeetCodeService leetService = new LeetCodeService();
    private final ObservableList<LeetCodeProblem> problems = FXCollections.observableArrayList();
    private ListView<LeetCodeProblem> listView;

    // Store label references as fields
    private Label totalVal;
    private Label solvedVal;
    private Label easyVal;
    private Label mediumVal;
    private Label hardVal;

    private final AtomicBoolean dataLoaded = new AtomicBoolean(false);

    public LeetCodeController() {
        // Don't load in constructor - load when view is created
    }

    // Public getters for Dashboard
    public int getSolvedCount() {
        return (int) problems.stream().filter(LeetCodeProblem::isSolved).count();
    }

    public int getCountByDifficulty(String difficulty) {
        return (int) problems.stream()
                .filter(p -> p.getDifficulty().equals(difficulty))
                .count();
    }

    public Node getView() {
        StackPane root = new StackPane();

        // Background
        ImageView bgImage = new ImageView();
        try {
            bgImage.setImage(new Image(getClass().getResourceAsStream("/images/BlueBg.png")));
            bgImage.setPreserveRatio(false);
            bgImage.fitWidthProperty().bind(root.widthProperty());
            bgImage.fitHeightProperty().bind(root.heightProperty());
        } catch (Exception e) {
            System.err.println("BlueBg.png not found – using fallback color");
            root.setStyle("-fx-background-color: " + AppConstants.BASE + ";");
        }
        if (bgImage.getImage() != null) {
            root.getChildren().add(bgImage);
        }

        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setMaxWidth(1100);
        content.setAlignment(Pos.TOP_CENTER);

        // Header
        HBox owlHeader = createOwlHeader();

        // Title header
        HBox header = createHeader();

        // Create stat labels FIRST (as fields)
        totalVal = createStatLabel(AppConstants.BLUE);
        solvedVal = createStatLabel(AppConstants.GREEN);
        easyVal = createStatLabel(AppConstants.GREEN);
        mediumVal = createStatLabel(AppConstants.YELLOW);
        hardVal = createStatLabel(AppConstants.RED);

        // Stats cards using the labels
        HBox stats = new HBox(20);
        stats.setAlignment(Pos.CENTER);
        stats.getChildren().addAll(
                statCardWithLabel("Total", totalVal),
                statCardWithLabel("Solved", solvedVal),
                statCardWithLabel("Easy", easyVal),
                statCardWithLabel("Medium", mediumVal),
                statCardWithLabel("Hard", hardVal)
        );

        // Filters
        HBox filters = createFilters();

        // ListView
        listView = new ListView<>();
        listView.setCellFactory(lv -> new ProblemCell());
        listView.setItems(problems);
        listView.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(listView, Priority.ALWAYS);

        // Assemble content
        content.getChildren().addAll(owlHeader, header, stats, filters, listView);
        root.getChildren().add(content);

        // Add listener for problem changes
        problems.addListener((javafx.collections.ListChangeListener<LeetCodeProblem>) c -> {
            updateStatsUI();
        });

        // Load data if not already loaded
        if (!dataLoaded.get()) {
            loadProblemsAsync();
        } else {
            // If data already loaded, just update UI
            updateStatsUI();
        }

        return root;
    }

    private HBox createOwlHeader() {
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
            System.err.println("Owl image not found – using emoji.");
            Label fallback = new Label("🦉");
            fallback.setFont(Font.font("Arial", FontWeight.BOLD, 30));
            owlHeader.getChildren().add(fallback);
        }
        if (owlIcon.getImage() != null) {
            owlHeader.getChildren().add(owlIcon);
        }

        VBox welcomeText = new VBox(5);
        Label greeting = new Label("LeetCode Progress");
        greeting.setFont(Font.font("Arial Rounded", FontWeight.BOLD, 18));
        greeting.setTextFill(Color.web(AppConstants.DEEP_TEAL));

        Label subGreeting = new Label("Keep grinding! 💪");
        subGreeting.setFont(Font.font("Arial", 12));
        subGreeting.setTextFill(Color.web(AppConstants.MEDIUM_TEAL));

        welcomeText.getChildren().addAll(greeting, subGreeting);
        owlHeader.getChildren().add(welcomeText);

        Region spacerOwl = new Region();
        HBox.setHgrow(spacerOwl, Priority.ALWAYS);
        owlHeader.getChildren().add(spacerOwl);

        return owlHeader;
    }

    private HBox createHeader() {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("💻 LeetCode Integration");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setTextFill(Color.web(AppConstants.ROSEWATER));
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button addBtn = new Button("➕ Add Problem");
        addBtn.setStyle("-fx-background-color: " + AppConstants.GREEN +
                "; -fx-text-fill: " + AppConstants.BASE +
                "; -fx-padding: 10 20; -fx-cursor: hand;" +
                "-fx-font-weight: bold; -fx-background-radius: 6;");
        addBtn.setOnAction(e -> showAddDialog());
        header.getChildren().addAll(title, sp, addBtn);
        return header;
    }

    private HBox createFilters() {
        HBox filters = new HBox(10);
        filters.setAlignment(Pos.CENTER_LEFT);
        Label fLbl = new Label("Filter:");
        fLbl.setTextFill(Color.web(AppConstants.TEXT));

        ComboBox<String> diffFilter = new ComboBox<>();
        diffFilter.getItems().addAll("All", "Easy", "Medium", "Hard");
        diffFilter.setValue("All");

        ComboBox<String> statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("All", "Solved", "Unsolved");
        statusFilter.setValue("All");

        diffFilter.setStyle("-fx-background-color: " + "#F4E397FF" + "; -fx-text-fill: " + AppConstants.TEXT + ";");
        statusFilter.setStyle("-fx-background-color: " + "#F4E397FF" + "; -fx-text-fill: " + AppConstants.TEXT + ";");

        diffFilter.setOnAction(e -> applyFilters(diffFilter.getValue(), statusFilter.getValue()));
        statusFilter.setOnAction(e -> applyFilters(diffFilter.getValue(), statusFilter.getValue()));

        Label dlbl = new Label("Difficulty:");
        dlbl.setTextFill(Color.web(AppConstants.SUBTEXT1));
        Label slbl = new Label("Status:");
        slbl.setTextFill(Color.web(AppConstants.SUBTEXT1));

        filters.getChildren().addAll(fLbl, dlbl, diffFilter, slbl, statusFilter);
        return filters;
    }

    private void loadProblemsAsync() {
        Task<List<LeetCodeProblem>> loadTask = new Task<>() {
            @Override
            protected List<LeetCodeProblem> call() {
                return leetService.getAllProblems();
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<LeetCodeProblem> loaded = loadTask.getValue();
            Platform.runLater(() -> {
                problems.setAll(loaded);
                dataLoaded.set(true);
                updateStatsUI();
                System.out.println("Loaded " + loaded.size() + " problems");
            });
        });

        loadTask.setOnFailed(e -> {
            Throwable ex = loadTask.getException();
            ex.printStackTrace();
            Platform.runLater(() ->
                    new Alert(Alert.AlertType.ERROR, "Failed to load problems: " + ex.getMessage()).showAndWait()
            );
        });

        new Thread(loadTask).start();
    }

    private void updateStatsUI() {
        if (totalVal == null) return; // UI not ready

        int total = problems.size();
        int solved = (int) problems.stream().filter(LeetCodeProblem::isSolved).count();
        int easy = (int) problems.stream().filter(p -> "Easy".equals(p.getDifficulty())).count();
        int medium = (int) problems.stream().filter(p -> "Medium".equals(p.getDifficulty())).count();
        int hard = (int) problems.stream().filter(p -> "Hard".equals(p.getDifficulty())).count();

        Platform.runLater(() -> {
            totalVal.setText(String.valueOf(total));
            solvedVal.setText(String.valueOf(solved));
            easyVal.setText(String.valueOf(easy));
            mediumVal.setText(String.valueOf(medium));
            hardVal.setText(String.valueOf(hard));

            if (listView != null) {
                listView.refresh();
            }
        });
    }

    private void applyFilters(String diff, String status) {
        List<LeetCodeProblem> filtered = new ArrayList<>(problems);
        if (!"All".equals(diff)) {
            filtered.removeIf(p -> !p.getDifficulty().equals(diff));
        }
        if ("Solved".equals(status)) {
            filtered.removeIf(p -> !p.isSolved());
        } else if ("Unsolved".equals(status)) {
            filtered.removeIf(LeetCodeProblem::isSolved);
        }
        listView.setItems(FXCollections.observableArrayList(filtered));
    }

    private void showAddDialog() {
        Dialog<LeetCodeProblem> dialog = new Dialog<>();
        dialog.setTitle("Add LeetCode Problem");
        ButtonType ok = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField titleF = new TextField();
        titleF.setPromptText("Problem title");
        TextField tagsF = new TextField();
        tagsF.setPromptText("Tags comma-separated");
        ComboBox<String> diffC = new ComboBox<>();
        diffC.getItems().addAll("Easy", "Medium", "Hard");
        diffC.setValue("Easy");
        TextField urlF = new TextField();
        urlF.setPromptText("https://leetcode.com/...");
        CheckBox solvedC = new CheckBox("Already solved?");

        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleF, 1, 0);
        grid.add(new Label("Tags:"), 0, 1);
        grid.add(tagsF, 1, 1);
        grid.add(new Label("Difficulty:"), 0, 2);
        grid.add(diffC, 1, 2);
        grid.add(new Label("URL:"), 0, 3);
        grid.add(urlF, 1, 3);
        grid.add(solvedC, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> btn == ok
                ? new LeetCodeProblem(titleF.getText(), tagsF.getText(),
                diffC.getValue(), solvedC.isSelected(), urlF.getText())
                : null);

        dialog.showAndWait().ifPresent(p -> {
            if (leetService.addProblem(p)) {
                problems.add(p);
            } else {
                new Alert(Alert.AlertType.ERROR, "Failed to save problem.").showAndWait();
            }
        });
    }

    private Label createStatLabel(String color) {
        Label l = new Label("0");
        l.setFont(Font.font("Arial Rounded", FontWeight.BOLD, 22));
        l.setTextFill(Color.web(color));
        return l;
    }

    private VBox statCardWithLabel(String title, Label valueLabel) {
        VBox card = new VBox(6);
        card.setStyle("-fx-background-color: white; -fx-padding: 15;" +
                "-fx-background-radius: 25;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2);");
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(130);
        Label tl = new Label(title);
        tl.setFont(Font.font("Arial", 11));
        tl.setTextFill(Color.web(AppConstants.SUBTEXT1));
        card.getChildren().addAll(valueLabel, tl);
        return card;
    }

    private void openUrl(String url) {
        try {
            if (url == null || url.isEmpty()) return;

            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                    desktop.browse(new java.net.URI(url));
                    return;
                }
            }
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec(new String[]{"open", url});
            } else {
                Runtime.getRuntime().exec(new String[]{"xdg-open", url});
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Could not open URL:\n" + url).showAndWait();
        }
    }

    // ===== Inner cell =====
    private class ProblemCell extends ListCell<LeetCodeProblem> {
        @Override
        protected void updateItem(LeetCodeProblem p, boolean empty) {
            super.updateItem(p, empty);
            if (empty || p == null) {
                setGraphic(null);
                setStyle("-fx-background-color: transparent;");
                return;
            }

            HBox cell = new HBox(15);
            cell.setPadding(new Insets(12));
            cell.setAlignment(Pos.CENTER_LEFT);
            cell.setStyle("-fx-background-color: white; -fx-background-radius: 30; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 1);");

            CheckBox cb = new CheckBox();
            cb.setSelected(p.isSolved());
            cb.setOnAction(e -> {
                p.setSolved(cb.isSelected());
                leetService.updateProblem(p);
                updateItem(p, false);
                // Update stats
                updateStatsUI();
            });

            VBox info = new VBox(5);
            Label tl = new Label(p.getTitle());
            tl.setFont(Font.font("Arial Rounded", FontWeight.BOLD, 14));
            tl.setTextFill(Color.web(AppConstants.DEEP_TEAL));
            if (p.isSolved()) tl.setStyle("-fx-strikethrough: true;");

            Label tags = new Label("🏷 " + (p.getTags() == null || p.getTags().isEmpty() ? "No tags" : p.getTags()));
            tags.setFont(Font.font("Arial", 11));
            tags.setTextFill(Color.web(AppConstants.SUBTEXT1));

            info.getChildren().addAll(tl, tags);
            HBox.setHgrow(info, Priority.ALWAYS);

            VBox right = new VBox(5);
            right.setAlignment(Pos.CENTER_RIGHT);

            Label dl = new Label(p.getDifficulty());
            dl.setStyle(
                    "-fx-background-color: " + diffColor(p.getDifficulty()) + ";" +
                            "-fx-padding: 4 12;" +
                            "-fx-background-radius: 20;" +
                            "-fx-text-fill: white;" +
                            "-fx-font-weight: bold; -fx-font-size: 11px;"
            );

            HBox actions = new HBox(5);
            Button open = new Button("🔗");
            open.setStyle("-fx-background-color: " + AppConstants.BLUE + "; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 5 10;");
            open.setOnAction(e -> openUrl(p.getUrl()));

            Button del = new Button("🗑");
            del.setStyle("-fx-background-color: " + AppConstants.RED + "; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 5 10;");
            del.setOnAction(e -> {
                if (leetService.deleteProblem(p.getId())) {
                    problems.remove(p);
                } else {
                    showError("Failed to delete problem.");
                }
            });

            actions.getChildren().addAll(open, del);
            right.getChildren().addAll(dl, actions);

            cell.getChildren().addAll(cb, info, right);
            setGraphic(cell);
            setStyle("-fx-background-color: transparent; -fx-padding: 5;");
        }

        private String diffColor(String d) {
            return switch (d) {
                case "Easy" -> AppConstants.GREEN;
                case "Medium" -> AppConstants.YELLOW;
                case "Hard" -> AppConstants.RED;
                default -> AppConstants.SUBTEXT1;
            };
        }
    }

    private void showError(String msg) {
        Platform.runLater(() ->
                new Alert(Alert.AlertType.ERROR, msg).showAndWait()
        );
    }
}