package com.codeflow.frontend.controllers;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import com.codeflow.api.models.Project;
import com.codeflow.api.services.ProjectService;
import com.codeflow.shared.AppConstants;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.Comparator;

/**
 * CALENDAR CONTROLLER  (was: CalendarView)
 */
public class CalendarController {

    private final ProjectService           projectService = new ProjectService();
    private final ObservableList<Project>  projects       = FXCollections.observableArrayList();

    private GridPane   calendarGrid;
    private Label      monthYearLabel;
    private YearMonth  currentYearMonth;

    public CalendarController() {
        currentYearMonth = YearMonth.now();
        reload();
    }

    public List<Project> getUpcomingProjects(int days) {
        return projectService.getUpcomingProjects(days);
    }

    public Node getView() {
        reload();

        // Root StackPane to hold background image and content
        StackPane root = new StackPane();

        // Background ImageView – fills the whole area
        ImageView bgImage = new ImageView();
        try {
            bgImage.setImage(new Image(getClass().getResourceAsStream("/images/OrangeBg.png")));
            bgImage.setPreserveRatio(false);
            bgImage.fitWidthProperty().bind(root.widthProperty());
            bgImage.fitHeightProperty().bind(root.heightProperty());
        } catch (Exception e) {
            System.err.println("OrangeBg.png not found – using fallback color");
            root.setStyle("-fx-background-color: #f4a261;"); // fallback orange
        }
        if (bgImage.getImage() != null) {
            root.getChildren().add(bgImage);
        }

        // Main content VBox (transparent background)
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setMaxWidth(1100);
        content.setAlignment(Pos.TOP_CENTER);

        // ===== Owl Header (new) =====
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
        Label greeting = new Label("Your Calendar");
        greeting.setFont(Font.font("Arial Rounded", FontWeight.BOLD, 18));
        greeting.setTextFill(Color.web(AppConstants.DEEP_TEAL));

        Label subGreeting = new Label("Stay organized! 📅");
        subGreeting.setFont(Font.font("Arial", 12));
        subGreeting.setTextFill(Color.web(AppConstants.MEDIUM_TEAL));

        welcomeText.getChildren().addAll(greeting, subGreeting);
        owlHeader.getChildren().add(welcomeText);

        Region spacerOwl = new Region();
        HBox.setHgrow(spacerOwl, Priority.ALWAYS);
        owlHeader.getChildren().add(spacerOwl);
        // ===== End Owl Header =====

        // Original header (title + add button)
        HBox headerBox = new HBox(15);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("📅 Project Calendar");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setTextFill(Color.web(AppConstants.ROSEWATER));
        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);
        Button addBtn = new Button("➕ Add Project");
        addBtn.setStyle("-fx-background-color: " + AppConstants.GREEN +
                "; -fx-text-fill: " + AppConstants.BASE +
                "; -fx-padding: 10 20; -fx-cursor: hand; -fx-font-weight: bold; -fx-background-radius: 6;");
        addBtn.setOnAction(e -> showAddDialog(content));
        headerBox.getChildren().addAll(title, hSpacer, addBtn);

        // Nav
        HBox nav = new HBox(15);
        nav.setAlignment(Pos.CENTER);
        Button prev = navBtn("◀ Previous");
        Button next = navBtn("Next ▶");
        Button today = navBtn("Today");
        monthYearLabel = new Label();
        monthYearLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        monthYearLabel.setTextFill(Color.web(AppConstants.TEXT));

        prev.setOnAction(e -> { currentYearMonth = currentYearMonth.minusMonths(1); updateCalendar(); });
        next.setOnAction(e -> { currentYearMonth = currentYearMonth.plusMonths(1); updateCalendar(); });
        today.setOnAction(e -> { currentYearMonth = YearMonth.now(); updateCalendar(); });

        Region n1 = new Region(); HBox.setHgrow(n1, Priority.ALWAYS);
        Region n2 = new Region(); HBox.setHgrow(n2, Priority.ALWAYS);
        nav.getChildren().addAll(prev, n1, monthYearLabel, n2, next, today);

        // Calendar
        calendarGrid = new GridPane();
        calendarGrid.setHgap(5); calendarGrid.setVgap(5); calendarGrid.setPadding(new Insets(10));
        calendarGrid.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        updateCalendar();

        // Project list below
        Label pListTitle = new Label("📋 All Projects");
        pListTitle.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        pListTitle.setTextFill(Color.web(AppConstants.ROSEWATER));

        VBox projectList = buildProjectList();

        // Add all elements in order: owlHeader, headerBox, nav, calendarGrid, pListTitle, projectList
        content.getChildren().addAll(owlHeader, headerBox, nav, calendarGrid, pListTitle, projectList);
        root.getChildren().add(content);

        return root;
    }
    // ── Calendar grid ─────────────────────────────────────────────────────────

    private void updateCalendar() {
        calendarGrid.getChildren().clear();

        // Set fixed column widths (80px each)
        calendarGrid.getColumnConstraints().clear();
        for (int i = 0; i < 7; i++) {
            ColumnConstraints col = new ColumnConstraints(80);
            calendarGrid.getColumnConstraints().add(col);
        }

        monthYearLabel.setText(
                currentYearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                        + " " + currentYearMonth.getYear());

        String[] days = {"Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
        for (int i = 0; i < 7; i++) {
            Label l = new Label(days[i]);
            l.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            l.setTextFill(Color.web(AppConstants.SUBTEXT1));
            l.setAlignment(Pos.CENTER);
            l.setPrefWidth(80);
            calendarGrid.add(l, i, 0);
        }

        LocalDate first = currentYearMonth.atDay(1);
        int startCol = first.getDayOfWeek().getValue() % 7;
        int total    = currentYearMonth.lengthOfMonth();
        int day = 1;

        for (int row = 1; row <= 6 && day <= total; row++) {
            for (int col = 0; col < 7; col++) {
                if (row == 1 && col < startCol) continue;
                if (day > total) break;

                LocalDate date = currentYearMonth.atDay(day);
                calendarGrid.add(dayCell(date), col, row);
                day++;
            }
        }

        // Prevent the grid from stretching beyond its natural width
        calendarGrid.setMaxWidth(7 * 80 + 6 * 5 + 2 * 10); // columns + hgap*6 + padding*2
    }

    private VBox dayCell(LocalDate date) {
        VBox cell = new VBox(3);
        cell.setPrefSize(80, 80); cell.setPadding(new Insets(5)); cell.setAlignment(Pos.TOP_LEFT);

        boolean isToday = date.equals(LocalDate.now());
        cell.setStyle(isToday
                ? "-fx-background-color: " + AppConstants.SURFACE1 + "; -fx-border-color: " + AppConstants.BLUE +
                "; -fx-border-width: 2; -fx-background-radius: 5; -fx-border-radius: 5;"
                : "-fx-background-color: " + "#bdbbbb"+ "; -fx-border-color: " + AppConstants.SURFACE1 +
                "; -fx-border-width: 1; -fx-background-radius: 5; -fx-border-radius: 5;");

        Label num = new Label(String.valueOf(date.getDayOfMonth()));
        num.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        num.setTextFill(Color.web(isToday ? AppConstants.BLUE : AppConstants.TEXT));
        cell.getChildren().add(num);

        // Project dots
        List<Project> dayProjects = projects.stream()
                .filter(p -> p.getDueDate().equals(date)).collect(Collectors.toList());
        for (Project p : dayProjects) {
            Label dot = new Label("● " + p.getTitle());
            dot.setFont(Font.font("Arial", 9)); dot.setTextFill(Color.web(p.getColor()));
            dot.setMaxWidth(90); dot.setStyle("-fx-text-overrun: ellipsis;");
            cell.getChildren().add(dot);
        }
        return cell;
    }

    // ── Project list ─────────────────────────────────────────────────────────

    private VBox buildProjectList() {
        VBox list = new VBox(10);
        List<Project> sorted = projects.stream()
                .sorted(Comparator.comparing(Project::getDueDate)).collect(Collectors.toList());

        if (sorted.isEmpty()) {
            Label empty = new Label("No projects yet. Click 'Add Project' to schedule one.");
            empty.setTextFill(Color.web(AppConstants.SUBTEXT1));
            empty.setFont(Font.font("Arial", 13)); empty.setPadding(new Insets(20));
            list.getChildren().add(empty);
        } else {
            for (Project p : sorted) list.getChildren().add(projectCard(p));
        }
        return list;
    }

    private VBox projectCard(Project project) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: " + "#fffdfc"+ "; -fx-background-radius: 10;");

        HBox header = new HBox(10); header.setAlignment(Pos.CENTER_LEFT);
        Label t = new Label(project.getTitle());
        t.setFont(Font.font("Arial", FontWeight.BOLD, 16)); t.setTextFill(Color.web(AppConstants.TEXT));
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), project.getDueDate());
        Label dl = new Label(daysLeft + " days left");
        dl.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        dl.setTextFill(Color.web(daysLeft <= 3 ? AppConstants.RED : AppConstants.GREEN));
        header.getChildren().addAll(t, sp, dl);

        Label desc = new Label(project.getDescription());
        desc.setFont(Font.font("Arial", 12)); desc.setTextFill(Color.web(AppConstants.SUBTEXT1)); desc.setWrapText(true);

        ProgressBar pb = new ProgressBar(project.getProgress() / 100.0);
        pb.setPrefWidth(200); pb.setStyle("-fx-accent: " + project.getColor() + ";");
        Label pct = new Label(project.getProgress() + "%");
        pct.setFont(Font.font("Arial", FontWeight.BOLD, 12)); pct.setTextFill(Color.web(AppConstants.TEXT));
        Slider slider = new Slider(0, 100, project.getProgress());
        slider.setPrefWidth(140);
        slider.valueProperty().addListener((obs, o, n) -> {
            project.setProgress(n.intValue()); pb.setProgress(n.intValue() / 100.0);
            pct.setText(n.intValue() + "%"); projectService.updateProject(project);
        });
        HBox progressRow = new HBox(10, new Label("Progress:"), pb, slider, pct);
        progressRow.setAlignment(Pos.CENTER_LEFT);

        Button del = new Button("🗑️ Delete");
        del.setStyle("-fx-background-color: " + AppConstants.RED + "; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 15; -fx-background-radius: 4;");
        del.setOnAction(e -> { if (projectService.deleteProject(project.getId())) { projects.remove(project); updateCalendar(); } });

        card.getChildren().addAll(header, desc, progressRow, del);
        return card;
    }

    // ── Add dialog ────────────────────────────────────────────────────────────

    private void showAddDialog(VBox root) {
        Dialog<Project> dialog = new Dialog<>();
        dialog.setTitle("Add Project"); dialog.setHeaderText("Schedule a new project");
        ButtonType ok = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);

        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));
        TextField titleF = new TextField(); titleF.setPromptText("Project title");
        TextArea  descF  = new TextArea();  descF.setPrefRowCount(3);
        DatePicker dateP = new DatePicker(LocalDate.now().plusDays(7));
        ComboBox<String> colorC = new ComboBox<>();
        colorC.getItems().addAll(AppConstants.BLUE, AppConstants.YELLOW, AppConstants.GREEN,
                AppConstants.RED, AppConstants.MAUVE);
        colorC.setValue(AppConstants.BLUE);

        grid.add(new Label("Title:"),       0, 0); grid.add(titleF, 1, 0);
        grid.add(new Label("Description:"), 0, 1); grid.add(descF,  1, 1);
        grid.add(new Label("Due Date:"),    0, 2); grid.add(dateP,  1, 2);
        grid.add(new Label("Color:"),       0, 3); grid.add(colorC, 1, 3);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> btn == ok
                ? new Project(titleF.getText(), descF.getText(), dateP.getValue(), colorC.getValue())
                : null);
        dialog.showAndWait().ifPresent(p -> {
            if (projectService.addProject(p)) { projects.add(p); updateCalendar(); }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void reload() { projects.clear(); projects.addAll(projectService.getAllProjects()); }

    private Button navBtn(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: " + AppConstants.SURFACE1 +
                "; -fx-text-fill: " + AppConstants.TEXT +
                "; -fx-padding: 8 15; -fx-cursor: hand; -fx-background-radius: 6;");
        return b;
    }
}