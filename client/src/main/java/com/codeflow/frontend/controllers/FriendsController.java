package com.codeflow.frontend.controllers;

import com.codeflow.shared.ApiClient;
import com.codeflow.shared.AppConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.*;
import javafx.application.Platform;
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
import javafx.util.Duration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class FriendsController {

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();
    private static final String BASE_URL = com.codeflow.shared.ApiClient.BASE_URL + "/friends";

    private VBox friendsList;
    private VBox pendingList;
    private Label notificationBadge;
    private int pendingCount = 0;

    public Node getView() {
        StackPane root = new StackPane();

        // Background
        try {
            ImageView bg = new ImageView(new Image(getClass().getResourceAsStream("/images/BlueBg.png")));
            bg.setPreserveRatio(false);
            bg.fitWidthProperty().bind(root.widthProperty());
            bg.fitHeightProperty().bind(root.heightProperty());
            root.getChildren().add(bg);
        } catch (Exception e) {
            root.setStyle("-fx-background-color: " + AppConstants.BASE + ";");
        }

        VBox content = new VBox(24);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(30, 30, 30, 30));
        root.getChildren().add(content);

        // ── Header ──────────────────────────────────────────────────────────
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 30, 20, 30));
        header.setStyle("-fx-background-color: rgba(255,255,255,0.92); -fx-background-radius: 18;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 3);");
        header.setMaxWidth(1000);

        Label titleIcon = new Label("👥");
        titleIcon.setFont(Font.font("Arial", 34));

        VBox titleText = new VBox(3);
        Label title = new Label("Friends");
        title.setFont(Font.font("Arial Rounded", FontWeight.BOLD, 26));
        title.setTextFill(Color.web(AppConstants.DEEP_TEAL));
        Label sub = new Label("Connect, study, and grow together with Hoot!");
        sub.setFont(Font.font("Arial", 13));
        sub.setTextFill(Color.web(AppConstants.MEDIUM_TEAL));
        titleText.getChildren().addAll(title, sub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Notification bell
        StackPane bellStack = new StackPane();
        Label bell = new Label("🔔");
        bell.setFont(Font.font("Arial", 28));
        notificationBadge = new Label("0");
        notificationBadge.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        notificationBadge.setStyle("-fx-background-color: #E53935; -fx-text-fill: white; " +
                "-fx-background-radius: 10; -fx-padding: 2 6;");
        notificationBadge.setVisible(false);
        StackPane.setAlignment(notificationBadge, Pos.TOP_RIGHT);
        bellStack.getChildren().addAll(bell, notificationBadge);

        header.getChildren().addAll(titleIcon, titleText, spacer, bellStack);

        // ── Add Friend Panel ──────────────────────────────────────────────
        VBox addPanel = buildAddFriendPanel();
        addPanel.setMaxWidth(1000);

        // ── Two-column layout: pending + friends ───────────────────────────
        HBox columns = new HBox(20);
        columns.setMaxWidth(1000);

        VBox pendingCard = buildPendingCard();
        VBox friendsCard = buildFriendsCard();
        HBox.setHgrow(pendingCard, Priority.ALWAYS);
        HBox.setHgrow(friendsCard, Priority.ALWAYS);

        columns.getChildren().addAll(pendingCard, friendsCard);

        content.getChildren().addAll(header, addPanel, columns);

        // Load data
        loadFriends();
        loadPending();

        return root;
    }

    private VBox buildAddFriendPanel() {
        VBox card = new VBox(14);
        card.setPadding(new Insets(24, 30, 24, 30));
        card.setStyle("-fx-background-color: rgba(255,255,255,0.92); -fx-background-radius: 18;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 10, 0, 0, 3);");

        Label panelTitle = new Label("➕ Add Friend");
        panelTitle.setFont(Font.font("Arial Rounded", FontWeight.BOLD, 18));
        panelTitle.setTextFill(Color.web(AppConstants.DEEP_TEAL));

        Label panelSub = new Label("Search by username to send a friend request");
        panelSub.setFont(Font.font("Arial", 12));
        panelSub.setTextFill(Color.web(AppConstants.MEDIUM_TEAL));

        HBox inputRow = new HBox(12);
        inputRow.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("Enter username...");
        searchField.setPrefHeight(42);
        searchField.setPrefWidth(300);
        searchField.setStyle("-fx-background-color: #f0f7fb; -fx-text-fill: #ade3e6;" +
                "-fx-background-radius: 10; -fx-border-color: #5F9598;" +
                "-fx-border-radius: 10; -fx-border-width: 1.5; -fx-padding: 0 12;");

        Button sendBtn = new Button("Send Request 📨");
        sendBtn.setStyle("-fx-background-color: " + AppConstants.DEEP_TEAL + "; -fx-text-fill: white;" +
                "-fx-background-radius: 10; -fx-padding: 10 22; -fx-font-weight: bold;" +
                "-fx-cursor: hand;");

        Label resultLabel = new Label();
        resultLabel.setFont(Font.font("Arial", 12));
        resultLabel.setVisible(false);

        sendBtn.setOnAction(e -> {
            String username = searchField.getText().trim();
            if (username.isEmpty()) { showResult(resultLabel, "Please enter a username.", false); return; }
            sendFriendRequest(username, resultLabel, searchField);
        });
        searchField.setOnAction(e -> sendBtn.fire());

        inputRow.getChildren().addAll(searchField, sendBtn, resultLabel);
        card.getChildren().addAll(panelTitle, panelSub, inputRow);
        return card;
    }

    private VBox buildPendingCard() {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20, 24, 20, 24));
        card.setStyle("-fx-background-color: rgba(255,255,255,0.88); -fx-background-radius: 18;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 8, 0, 0, 3);");

        Label cardTitle = new Label("📬 Friend Requests");
        cardTitle.setFont(Font.font("Arial Rounded", FontWeight.BOLD, 16));
        cardTitle.setTextFill(Color.web(AppConstants.DEEP_TEAL));

        pendingList = new VBox(8);

        Button refresh = new Button("↻ Refresh");
        refresh.setStyle("-fx-background-color: #E3F2FD; -fx-text-fill: #1565C0;" +
                "-fx-background-radius: 8; -fx-padding: 6 16; -fx-cursor: hand;");
        refresh.setOnAction(e -> loadPending());

        card.getChildren().addAll(cardTitle, pendingList, refresh);
        return card;
    }

    private VBox buildFriendsCard() {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20, 24, 20, 24));
        card.setStyle("-fx-background-color: rgba(255,255,255,0.88); -fx-background-radius: 18;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 8, 0, 0, 3);");

        Label cardTitle = new Label("🤝 My Friends");
        cardTitle.setFont(Font.font("Arial Rounded", FontWeight.BOLD, 16));
        cardTitle.setTextFill(Color.web(AppConstants.DEEP_TEAL));

        friendsList = new VBox(8);

        Button refresh = new Button("↻ Refresh");
        refresh.setStyle("-fx-background-color: #E8F5E9; -fx-text-fill: #2E7D32;" +
                "-fx-background-radius: 8; -fx-padding: 6 16; -fx-cursor: hand;");
        refresh.setOnAction(e -> loadFriends());

        card.getChildren().addAll(cardTitle, friendsList, refresh);
        return card;
    }

    // ── API calls ─────────────────────────────────────────────────────────────

    private void sendFriendRequest(String username, Label resultLabel, TextField field) {
        String token = ApiClient.getToken();
        if (token == null) { showResult(resultLabel, "Not logged in.", false); return; }

        new Thread(() -> {
            try {
                String body = "{\"username\":\"" + username + "\"}";
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/request"))
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
                JsonNode node = mapper.readTree(resp.body());
                boolean ok = resp.statusCode() == 200;
                String msg = ok ? node.path("message").asText("Request sent!")
                        : node.path("error").asText("Unknown error");
                Platform.runLater(() -> {
                    showResult(resultLabel, msg, ok);
                    if (ok) field.clear();
                });
            } catch (Exception e) {
                Platform.runLater(() -> showResult(resultLabel, "Connection error.", false));
            }
        }).start();
    }

    private void loadPending() {
        String token = ApiClient.getToken();
        if (token == null) return;

        new Thread(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/pending"))
                        .header("Authorization", "Bearer " + token)
                        .GET().build();
                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
                JsonNode arr = mapper.readTree(resp.body());

                Platform.runLater(() -> {
                    pendingList.getChildren().clear();
                    pendingCount = 0;
                    if (arr.isArray() && arr.size() == 0) {
                        Label empty = new Label("No pending requests.");
                        empty.setFont(Font.font("Arial", 12));
                        empty.setTextFill(Color.web(AppConstants.OVERLAY0));
                        pendingList.getChildren().add(empty);
                    } else if (arr.isArray()) {
                        pendingCount = arr.size();
                        for (JsonNode item : arr) {
                            String id = item.path("id").asText();
                            String from = item.path("from").asText();
                            pendingList.getChildren().add(buildPendingRow(id, from));
                        }
                    }
                    updateBadge();
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void loadFriends() {
        String token = ApiClient.getToken();
        if (token == null) return;

        new Thread(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/list"))
                        .header("Authorization", "Bearer " + token)
                        .GET().build();
                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
                JsonNode arr = mapper.readTree(resp.body());

                Platform.runLater(() -> {
                    friendsList.getChildren().clear();
                    if (arr.isArray() && arr.size() == 0) {
                        Label empty = new Label("No friends yet. Add some! 👆");
                        empty.setFont(Font.font("Arial", 12));
                        empty.setTextFill(Color.web(AppConstants.OVERLAY0));
                        friendsList.getChildren().add(empty);
                    } else if (arr.isArray()) {
                        for (JsonNode item : arr) {
                            String username = item.path("username").asText();
                            friendsList.getChildren().add(buildFriendRow(username));
                        }
                    }
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private HBox buildPendingRow(String requestId, String fromUsername) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 14, 10, 14));
        row.setStyle("-fx-background-color: #FFF8E1; -fx-background-radius: 12;");

        Label avatar = new Label("🧑");
        avatar.setFont(Font.font("Arial", 20));
        Label nameLabel = new Label(fromUsername);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        nameLabel.setTextFill(Color.web(AppConstants.DEEP_TEAL));
        Label reqLabel = new Label("wants to be friends");
        reqLabel.setFont(Font.font("Arial", 11));
        reqLabel.setTextFill(Color.web(AppConstants.MEDIUM_TEAL));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button accept = new Button("✔ Accept");
        accept.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 5 12; -fx-cursor: hand;");
        accept.setOnAction(e -> respondRequest(requestId, "accept"));

        Button decline = new Button("✖ Decline");
        decline.setStyle("-fx-background-color: #E53935; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 5 12; -fx-cursor: hand;");
        decline.setOnAction(e -> respondRequest(requestId, "decline"));

        row.getChildren().addAll(avatar, nameLabel, reqLabel, spacer, accept, decline);
        return row;
    }

    private HBox buildFriendRow(String username) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 14, 10, 14));
        row.setStyle("-fx-background-color: #E8F5E9; -fx-background-radius: 12;");

        Label avatar = new Label("🧑");
        avatar.setFont(Font.font("Arial", 20));
        Label nameLabel = new Label(username);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        nameLabel.setTextFill(Color.web(AppConstants.DEEP_TEAL));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Unfriend / Block options via ContextMenu
        MenuButton options = new MenuButton("⋮");
        options.setStyle("-fx-background-color: transparent; -fx-text-fill: #9E9E9E;" +
                "-fx-font-size: 18px; -fx-cursor: hand; -fx-padding: 2 8;");
        MenuItem unfriend = new MenuItem("🚫 Unfriend");
        MenuItem block = new MenuItem("🛑 Block");
        unfriend.setOnAction(e -> doRemoveFriend(username));
        block.setOnAction(e -> doBlockUser(username));
        options.getItems().addAll(unfriend, block);

        row.getChildren().addAll(avatar, nameLabel, spacer, options);
        return row;
    }

    private void respondRequest(String id, String action) {
        String token = ApiClient.getToken();
        if (token == null) return;

        new Thread(() -> {
            try {
                String body = "{\"action\":\"" + action + "\"}";
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/respond/" + id))
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                http.send(req, HttpResponse.BodyHandlers.ofString());
                Platform.runLater(() -> { loadPending(); loadFriends(); });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void doRemoveFriend(String username) {
        String token = ApiClient.getToken();
        if (token == null) return;
        new Thread(() -> {
            try {
                String body = "{\"username\":\"" + username + "\"}";
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/remove"))
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json")
                        .method("DELETE", HttpRequest.BodyPublishers.ofString(body))
                        .build();
                http.send(req, HttpResponse.BodyHandlers.ofString());
                Platform.runLater(this::loadFriends);
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void doBlockUser(String username) {
        String token = ApiClient.getToken();
        if (token == null) return;
        new Thread(() -> {
            try {
                String body = "{\"username\":\"" + username + "\"}";
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/block"))
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                http.send(req, HttpResponse.BodyHandlers.ofString());
                Platform.runLater(this::loadFriends);
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void updateBadge() {
        if (pendingCount > 0) {
            notificationBadge.setText(String.valueOf(pendingCount));
            notificationBadge.setVisible(true);
        } else {
            notificationBadge.setVisible(false);
        }
    }

    private void showResult(Label label, String msg, boolean success) {
        label.setText(msg);
        label.setTextFill(success ? Color.web(AppConstants.GREEN) : Color.web(AppConstants.RED));
        label.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.seconds(4), label);
        ft.setFromValue(1); ft.setToValue(0);
        ft.setOnFinished(e -> label.setVisible(false));
        ft.play();
    }
}