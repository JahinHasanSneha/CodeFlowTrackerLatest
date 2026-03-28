package com.codeflow.frontend.controllers;

import com.codeflow.api.models.Message;
import com.codeflow.api.services.MessageService;
import com.codeflow.shared.ApiClient;
import com.codeflow.shared.AppConstants;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

public class MessagingController {

    private final MessageService messageService = new MessageService();
    private VBox      conversationBox;
    private ScrollPane conversationScroll;
    private TextField newMessageField;
    private String    activePeer = null;
    private VBox      peerListBox;
    private Timeline  pollTimer;
    private String    lastMessageTime = "";

    public Node getView() {
        StackPane root = new StackPane();
        ImageView bgImage = new ImageView();
        try {
            bgImage.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/PinkBg.png"))));
            bgImage.setPreserveRatio(false);
            bgImage.fitWidthProperty().bind(root.widthProperty());
            bgImage.fitHeightProperty().bind(root.heightProperty());
        } catch (Exception e) { root.setStyle("-fx-background-color: " + AppConstants.BASE + ";"); }
        if (bgImage.getImage() != null) root.getChildren().add(bgImage);

        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setAlignment(Pos.TOP_CENTER);

        HBox owlHeader = new HBox(15);
        owlHeader.setAlignment(Pos.CENTER_LEFT);
        owlHeader.setStyle("-fx-background-color: rgba(255,255,255,0.8); -fx-background-radius: 30; -fx-padding: 10 20;");
        ImageView owlIcon = new ImageView();
        try {
            owlIcon.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/owl1.png"))));
            owlIcon.setFitHeight(50); owlIcon.setFitWidth(50);
            owlHeader.getChildren().add(owlIcon);
        } catch (Exception e) { Label fb = new Label("🦉"); fb.setFont(Font.font("Arial", FontWeight.BOLD, 30)); owlHeader.getChildren().add(fb); }

        VBox welcomeText = new VBox(5);
        Label greeting = new Label("Messages");
        greeting.setFont(Font.font("Arial Rounded", FontWeight.BOLD, 18));
        greeting.setTextFill(Color.web(AppConstants.DEEP_TEAL));
        Label sub = new Label("Chat with friends! 💬 (Friends only)");
        sub.setFont(Font.font("Arial", 12));
        sub.setTextFill(Color.web(AppConstants.MEDIUM_TEAL));
        welcomeText.getChildren().addAll(greeting, sub);
        owlHeader.getChildren().add(welcomeText);
        Region hSpacer = new Region(); HBox.setHgrow(hSpacer, Priority.ALWAYS);
        owlHeader.getChildren().add(hSpacer);

        HBox chatLayout = new HBox(16);
        VBox.setVgrow(chatLayout, Priority.ALWAYS);
        chatLayout.setPrefHeight(520);
        VBox leftPanel = buildLeftPanel(); leftPanel.setPrefWidth(220); leftPanel.setMinWidth(200);
        VBox rightPanel = buildRightPanel(); HBox.setHgrow(rightPanel, Priority.ALWAYS);
        chatLayout.getChildren().addAll(leftPanel, rightPanel);
        content.getChildren().addAll(owlHeader, chatLayout);
        root.getChildren().add(content);
        startPolling();
        root.sceneProperty().addListener((obs, o, n) -> { if (n == null) stopPolling(); });
        return root;
    }

    private VBox buildLeftPanel() {
        VBox panel = new VBox(10);
        panel.setStyle("-fx-background-color: white; -fx-background-radius: 30; -fx-padding: 20;" +
                       "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.08),12,0,0,3);" +
                       "-fx-border-color: " + AppConstants.LIGHT_TEAL + "; -fx-border-width: 2; -fx-border-radius: 30;");
        Label title = new Label("💬 Conversations");
        title.setFont(Font.font("Arial Rounded", FontWeight.BOLD, 15));
        title.setTextFill(Color.web(AppConstants.DEEP_TEAL));
        TextField searchField = new TextField();
        searchField.setPromptText("Friend's username…");
        searchField.setStyle("-fx-background-color: " + AppConstants.SURFACE0 + "#74b3d5#5ea2c6#87a9e1; -fx-text-fill: " + AppConstants.TEXT + ";" +
                             "-fx-background-radius: 20; -fx-border-color: " + AppConstants.SURFACE1 + "; -fx-border-radius: 20; -fx-border-width: 1; -fx-padding: 6 12;");
        Button startBtn = new Button("Start Chat");
        startBtn.setStyle("-fx-background-color: " + AppConstants.DEEP_TEAL + "; -fx-text-fill: white;" +
                          "-fx-background-radius: 20; -fx-padding: 6 14; -fx-cursor: hand; -fx-font-weight: bold;");
        startBtn.setOnAction(e -> { String peer = searchField.getText().trim(); if (!peer.isEmpty()) { searchField.clear(); openConversation(peer); } });
        peerListBox = new VBox(6);
        ScrollPane peerScroll = new ScrollPane(peerListBox);
        peerScroll.setFitToWidth(true);
        peerScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(peerScroll, Priority.ALWAYS);
        panel.getChildren().addAll(title, searchField, startBtn, new Separator(), peerScroll);
        loadPeerList();
        return panel;
    }

    private void loadPeerList() {
        new Thread(() -> {
            List<String> partners = messageService.getConversationPartners();
            Platform.runLater(() -> {
                peerListBox.getChildren().clear();
                if (partners.isEmpty()) {
                    Label empty = new Label("No conversations yet.\nAdd friends first!");
                    empty.setTextFill(Color.web(AppConstants.SUBTEXT1)); empty.setFont(Font.font("Arial", 12)); empty.setWrapText(true);
                    peerListBox.getChildren().add(empty);
                } else { for (String p : partners) peerListBox.getChildren().add(peerRow(p)); }
            });
        }).start();
    }

    private HBox peerRow(String username) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT); row.setPadding(new Insets(8, 12, 8, 12));
        row.setStyle("-fx-background-color: " + AppConstants.LIGHT_TEAL + "; -fx-background-radius: 20; -fx-cursor: hand;");
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: " + AppConstants.SURFACE2 + "; -fx-background-radius: 20; -fx-cursor: hand;"));
        row.setOnMouseExited(e  -> row.setStyle("-fx-background-color: " + AppConstants.LIGHT_TEAL + "; -fx-background-radius: 20; -fx-cursor: hand;"));
        Label avatar = new Label("👤"); avatar.setFont(Font.font("Arial", 18));
        Label name = new Label(username); name.setFont(Font.font("Arial Rounded", FontWeight.BOLD, 13)); name.setTextFill(Color.web(AppConstants.DEEP_TEAL));
        row.getChildren().addAll(avatar, name);
        row.setOnMouseClicked(e -> openConversation(username));
        return row;
    }

    private VBox buildRightPanel() {
        VBox panel = new VBox(10);
        panel.setStyle("-fx-background-color: white; -fx-background-radius: 30; -fx-padding: 20;" +
                       "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.08),12,0,0,3);" +
                       "-fx-border-color: " + AppConstants.LIGHT_TEAL + "; -fx-border-width: 2; -fx-border-radius: 30;");
        Label placeholder = new Label("👈 Select or start a conversation");
        placeholder.setTextFill(Color.web(AppConstants.SUBTEXT1)); placeholder.setFont(Font.font("Arial Rounded", 15)); placeholder.setAlignment(Pos.CENTER);
        conversationBox = new VBox(10); conversationBox.setPadding(new Insets(8)); conversationBox.getChildren().add(placeholder);
        conversationScroll = new ScrollPane(conversationBox);
        conversationScroll.setFitToWidth(true); conversationScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(conversationScroll, Priority.ALWAYS);
        newMessageField = new TextField(); newMessageField.setPromptText("Type a message…");
        newMessageField.setStyle("-fx-background-color: " + AppConstants.SURFACE0 + "; -fx-text-fill: " + AppConstants.TEXT + ";" +
                                 "-fx-background-radius: 25; -fx-border-color: " + AppConstants.SURFACE1 + "; -fx-border-radius: 25; -fx-border-width: 1; -fx-padding: 10 16;");
        HBox.setHgrow(newMessageField, Priority.ALWAYS); newMessageField.setOnAction(e -> sendMessage());
        Button sendBtn = new Button("Send ➤");
        sendBtn.setStyle("-fx-background-color: " + AppConstants.DEEP_TEAL + "; -fx-text-fill: white; -fx-background-radius: 25; -fx-padding: 10 20; -fx-cursor: hand; -fx-font-weight: bold;");
        sendBtn.setOnAction(e -> sendMessage());
        HBox inputRow = new HBox(10, newMessageField, sendBtn); inputRow.setAlignment(Pos.CENTER); inputRow.setPadding(new Insets(8, 0, 0, 0));
        panel.getChildren().addAll(conversationScroll, new Separator(), inputRow);
        return panel;
    }

    private void openConversation(String peer) {
        activePeer = peer; lastMessageTime = "";
        conversationBox.getChildren().clear();
        Label header = new Label("💬 Chat with " + peer);
        header.setFont(Font.font("Arial Rounded", FontWeight.BOLD, 15)); header.setTextFill(Color.web(AppConstants.DEEP_TEAL));
        conversationBox.getChildren().add(header);
        boolean found = peerListBox.getChildren().stream().filter(n -> n instanceof HBox)
                .flatMap(n -> ((HBox)n).getChildren().stream()).filter(n -> n instanceof Label)
                .map(n -> ((Label)n).getText()).anyMatch(t -> t.equals(peer));
        if (!found) peerListBox.getChildren().add(0, peerRow(peer));
        new Thread(() -> {
            List<Message> msgs = messageService.getConversation(peer);
            Platform.runLater(() -> {
                conversationBox.getChildren().clear(); conversationBox.getChildren().add(header);
                if (msgs.isEmpty()) {
                    Label empty = new Label("No messages yet. Say hi! 👋");
                    empty.setTextFill(Color.web(AppConstants.SUBTEXT1)); empty.setFont(Font.font("Arial", 13));
                    conversationBox.getChildren().add(empty);
                } else {
                    for (Message m : msgs) {
                        conversationBox.getChildren().add(messageBubble(m));
                        if (m.getSentAt() != null) { String t = m.getSentAt().toString(); if (t.compareTo(lastMessageTime) > 0) lastMessageTime = t; }
                    }
                }
                scrollToBottom();
            });
        }).start();
    }

    private void sendMessage() {
        if (activePeer == null) { new Alert(Alert.AlertType.WARNING, "Select a conversation first.\n(You can only message friends)").showAndWait(); return; }
        String text = newMessageField.getText().trim(); if (text.isEmpty()) return;
        newMessageField.clear();
        new Thread(() -> {
            Message sent = messageService.sendMessage(activePeer, text);
            Platform.runLater(() -> {
                if (sent != null) {
                    conversationBox.getChildren().add(messageBubble(sent));
                    if (sent.getSentAt() != null) { String t = sent.getSentAt().toString(); if (t.compareTo(lastMessageTime) > 0) lastMessageTime = t; }
                    scrollToBottom(); loadPeerList();
                } else {
                    new Alert(Alert.AlertType.ERROR, "Failed to send.\nAre you friends with " + activePeer + "?").showAndWait();
                }
            });
        }).start();
    }

    private void startPolling() {
        pollTimer = new Timeline(new KeyFrame(Duration.seconds(2), e -> pollNewMessages()));
        pollTimer.setCycleCount(Timeline.INDEFINITE); pollTimer.play();
    }

    private void stopPolling() { if (pollTimer != null) { pollTimer.stop(); pollTimer = null; } }

    private void pollNewMessages() {
        if (activePeer == null || lastMessageTime.isEmpty()) return;
        String peer = activePeer; String since = lastMessageTime;
        new Thread(() -> {
            List<Message> newMsgs = messageService.pollMessages(peer, since);
            if (!newMsgs.isEmpty()) {
                Platform.runLater(() -> {
                    for (Message m : newMsgs) {
                        if (m.getSenderUsername().equals(ApiClient.getCurrentUsername())) continue;
                        conversationBox.getChildren().add(messageBubble(m));
                        if (m.getSentAt() != null) { String t = m.getSentAt().toString(); if (t.compareTo(lastMessageTime) > 0) lastMessageTime = t; }
                    }
                    scrollToBottom();
                });
            }
        }).start();
    }

    private void scrollToBottom() { conversationScroll.setVvalue(1.0); }

    private HBox messageBubble(Message m) {
        String me = ApiClient.getCurrentUsername(); boolean isMine = m.getSenderUsername().equals(me);
        Label bubble = new Label(m.getContent()); bubble.setWrapText(true); bubble.setMaxWidth(380);
        bubble.setFont(Font.font("Arial", 13)); bubble.setPadding(new Insets(10, 16, 10, 16));
        if (isMine) bubble.setStyle("-fx-background-color: " + AppConstants.DEEP_TEAL + "; -fx-text-fill: white; -fx-background-radius: 20 20 4 20;");
        else         bubble.setStyle("-fx-background-color: " + AppConstants.LIGHT_TEAL + "; -fx-text-fill: " + AppConstants.DEEP_TEAL + "; -fx-background-radius: 20 20 20 4;");
        VBox wrapper = new VBox(3, bubble);
        String timeStr = m.getSentAt() != null ? m.getSentAt().format(DateTimeFormatter.ofPattern("HH:mm")) : "";
        Label meta = new Label((isMine ? "You" : m.getSenderUsername()) + (timeStr.isEmpty() ? "" : "  " + timeStr));
        meta.setFont(Font.font("Arial", 10)); meta.setTextFill(Color.web(AppConstants.SUBTEXT0));
        wrapper.getChildren().add(meta); wrapper.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        HBox row = new HBox(wrapper); row.setPadding(new Insets(3, 6, 3, 6)); row.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        return row;
    }
}
