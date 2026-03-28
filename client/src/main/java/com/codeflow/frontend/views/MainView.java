package com.codeflow.frontend.views;

import com.codeflow.api.services.AuthService;
import com.codeflow.frontend.controllers.*;
import com.codeflow.shared.AppConstants;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class MainView {

    private final Stage stage;

    private BorderPane root;

    // Controllers
    private DashboardController dashboard;
    private HubController hubCtrl;
    private CoursePageController coursePageCtrl;

    private TaskController taskCtrl;
    private ProgressController progressCtrl;
    private CalendarController calendarCtrl;
    private LeetCodeController leetCtrl;
    private MessagingController msgCtrl;
    private PomodoroController pomodoroCtrl;
    private FriendsController friendsCtrl;

    // Notification badges
    private Label friendBadge;
    private Label msgBadge;

    public MainView(Stage stage) {
        this.stage = stage;
    }

    public Parent build() {
        // init controllers
        dashboard = new DashboardController();
        hubCtrl = new HubController();
        coursePageCtrl = new CoursePageController();

        taskCtrl = new TaskController();
        progressCtrl = new ProgressController();
        calendarCtrl = new CalendarController();
        leetCtrl = new LeetCodeController();
        msgCtrl = new MessagingController();
        pomodoroCtrl = new PomodoroController();
        friendsCtrl = new FriendsController();

        root = new BorderPane();
        root.setStyle("-fx-background-color: " + AppConstants.BASE + ";");
        root.setTop(buildNavBar());

        showDashboard(); // default page
        return root;
    }

    private HBox buildNavBar() {
        HBox nav = new HBox(10);
        nav.setPadding(new Insets(12, 20, 12, 20));
        nav.setStyle(
                "-fx-background-color: #8eb8ea;" +
                        "-fx-border-color: " + AppConstants.MAUVE + ";" +
                        "-fx-border-width: 0 0 2 0;"
        );
        nav.setAlignment(Pos.CENTER_LEFT);

        Label brand = new Label("⚡ CodeFlow");
        brand.setFont(Font.font("Arial", FontWeight.BOLD, 23));
        brand.setTextFill(Color.web(AppConstants.MAUVE));

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button dashBtn = navButton("", "Dashboard");
        Button taskBtn = navButton("", "Tasks");
        Button pomodoroBtn = navButton("", "Pomodoro");
        Button progBtn = navButton("", "Progress");
        Button calBtn = navButton("", "Calendar");
        Button leetBtn = navButton("", "LeetCode");

        // Friends button with notification badge
        StackPane friendBtnStack = new StackPane();
        Button friendBtn = navButton("", "Friends");
        friendBadge = new Label("");
        friendBadge.setStyle("-fx-background-color: #E53935; -fx-text-fill: white;" +
                "-fx-background-radius: 8; -fx-padding: 1 5; -fx-font-size: 10px; -fx-font-weight: bold;");
        friendBadge.setVisible(false);
        StackPane.setAlignment(friendBadge, Pos.TOP_RIGHT);
        friendBtnStack.getChildren().addAll(friendBtn, friendBadge);

        // Messages button with notification badge
        StackPane msgBtnStack = new StackPane();
        Button msgBtn = navButton("", "Messages");
        msgBadge = new Label("●");
        msgBadge.setStyle("-fx-background-color: #E53935; -fx-text-fill: white;" +
                "-fx-background-radius: 8; -fx-padding: 1 5; -fx-font-size: 10px;");
        msgBadge.setVisible(false);
        StackPane.setAlignment(msgBadge, Pos.TOP_RIGHT);
        msgBtnStack.getChildren().addAll(msgBtn, msgBadge);

        dashBtn.setOnAction(e -> showDashboard());
        taskBtn.setOnAction(e -> showTasks());
        pomodoroBtn.setOnAction(e -> showPomodoro());
        progBtn.setOnAction(e -> showProgress());
        calBtn.setOnAction(e -> showCalendar());
        leetBtn.setOnAction(e -> showLeetCode());
        friendBtn.setOnAction(e -> showFriends());
        msgBtn.setOnAction(e -> showMessages());

        String username = AuthService.getCurrentUser() != null
                ? AuthService.getCurrentUser().getUsername()
                : "User";
        Label userLabel = new Label("👤 " + username);
        userLabel.setTextFill(Color.web(AppConstants.SUBTEXT1));
        userLabel.setFont(Font.font("Arial", 13));

        Button logoutBtn = new Button("Log out");
        logoutBtn.setStyle(logoutStyleNormal());
        logoutBtn.setOnMouseEntered(e -> logoutBtn.setStyle(logoutStyleHover()));
        logoutBtn.setOnMouseExited(e -> logoutBtn.setStyle(logoutStyleNormal()));
        logoutBtn.setOnAction(e -> {
            new AuthService().logout();
            stage.getScene().setRoot(new SplashView(stage).build());
        });

        nav.getChildren().addAll(
                brand, spacer,
                dashBtn, taskBtn, pomodoroBtn, progBtn, calBtn, leetBtn,
                friendBtnStack, msgBtnStack,
                userLabel, logoutBtn
        );
        return nav;
    }

    private Button navButton(String icon, String text) {
        Button btn = new Button(icon + " " + text);
        String base = "-fx-background-color: #437ae3;" +
                "-fx-text-fill: " + AppConstants.TEXT + ";" +
                "-fx-padding: 7 13; -fx-cursor: hand;" +
                "-fx-font-size: 12px; -fx-background-radius: 7;";
        String hover = "-fx-background-color: " + AppConstants.SURFACE1 + ";" +
                "-fx-text-fill: " + AppConstants.TEXT + ";" +
                "-fx-padding: 7 13; -fx-cursor: hand;" +
                "-fx-font-size: 12px; -fx-background-radius: 7;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    private String logoutStyleNormal() {
        return "-fx-background-color: transparent; -fx-text-fill: white;" +
                "-fx-border-color: " + AppConstants.SURFACE1 + ";" +
                "-fx-border-radius: 6; -fx-border-width: 1;" +
                "-fx-background-radius: 6; -fx-padding: 5 12;" +
                "-fx-cursor: hand; -fx-font-size: 12px;";
    }

    private String logoutStyleHover() {
        return "-fx-background-color: #edf7fc; -fx-text-fill: " + AppConstants.RED + ";" +
                "-fx-border-color: " + AppConstants.RED + ";" +
                "-fx-border-radius: 6; -fx-border-width: 1;" +
                "-fx-background-radius: 6; -fx-padding: 5 12;" +
                "-fx-cursor: hand; -fx-font-size: 12px;";
    }

    private void showDashboard() {
        root.setCenter(
                wrapScroll(
                        dashboard.getView(taskCtrl, progressCtrl, calendarCtrl, this::showHub)
                )
        );
    }

    private void showHub() {
        root.setCenter(
                wrapScroll(
                        hubCtrl.getView(
                                this::showDashboard,
                                (course) -> root.setCenter(
                                        wrapScroll(
                                                coursePageCtrl.getView(
                                                        new CoursePageController.Course(
                                                                course.code,
                                                                course.name,
                                                                course.instructor, course.imagePath
                                                        ),
                                                        this::showHub
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private void showTasks() {
        root.setCenter(wrapScroll(taskCtrl.getView()));
    }

    private void showPomodoro() {
        root.setCenter(wrapScroll(pomodoroCtrl.getView()));
    }

    private void showProgress() {
        root.setCenter(wrapScroll(progressCtrl.getView()));
    }

    private void showCalendar() {
        root.setCenter(wrapScroll(calendarCtrl.getView()));
    }

    private void showLeetCode() {
        root.setCenter(wrapScroll(leetCtrl.getView()));
    }

    private void showFriends() {
        root.setCenter(wrapScroll(friendsCtrl.getView()));
    }

    private void showMessages() {
        root.setCenter(msgCtrl.getView());
    }

    private ScrollPane wrapScroll(javafx.scene.Node node) {
        ScrollPane sp = new ScrollPane(node);
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);
        sp.setStyle("-fx-background: " + AppConstants.BASE + "; -fx-background-color: " + AppConstants.BASE + ";");
        return sp;
    }
}