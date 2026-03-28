package com.codeflow.backend.server;

import com.codeflow.backend.storage.DataStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;


public class AuthHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if ("OPTIONS".equals(ex.getRequestMethod())) { CodeFlowServer.handleOptions(ex); return; }
        String path = ex.getRequestURI().getPath();
        String body = CodeFlowServer.readBody(ex);

        try {
            if (path.endsWith("/register")) handleRegister(ex, body);
            else if (path.endsWith("/login"))    handleLogin(ex, body);
            else CodeFlowServer.sendJson(ex, 404, "{\"error\":\"Not found\"}");
        } catch (Exception e) {
            e.printStackTrace();
            CodeFlowServer.sendJson(ex, 500, "{\"error\":\"Server error\"}");
        }
    }

    private void handleRegister(HttpExchange ex, String body) throws IOException {
        JsonNode req = DataStore.MAPPER.readTree(body);
        String username = req.path("username").asText("").trim();
        String email    = req.path("email").asText("").trim();
        String password = req.path("password").asText("");

        if (username.isEmpty() || email.isEmpty() || password.length() < 6) {
            CodeFlowServer.sendJson(ex, 400, "{\"error\":\"Invalid input\"}");
            return;
        }

        // Check uniqueness
        if (DataStore.findOne("users", "username", username) != null) {
            CodeFlowServer.sendJson(ex, 400, "{\"error\":\"Username already taken\"}");
            return;
        }
        if (DataStore.findOne("users", "email", email) != null) {
            CodeFlowServer.sendJson(ex, 400, "{\"error\":\"Email already registered\"}");
            return;
        }

        String userId = UUID.randomUUID().toString();
        String hash   = BCrypt.hashpw(password, BCrypt.gensalt());

        ObjectNode user = DataStore.MAPPER.createObjectNode();
        user.put("id",           userId);
        user.put("username",     username);
        user.put("email",        email);
        user.put("passwordHash", hash);
        user.put("createdAt",    LocalDateTime.now().toString());
        DataStore.insert("users", user);

        String token = TokenUtil.generate(username);
        String resp = "{\"token\":\"" + token + "\",\"username\":\"" + username + "\",\"userId\":\"" + userId + "\",\"email\":\"" + email + "\"}";
        CodeFlowServer.sendJson(ex, 200, resp);
    }

    private void handleLogin(HttpExchange ex, String body) throws IOException {
        JsonNode req = DataStore.MAPPER.readTree(body);
        String username = req.path("username").asText("").trim();
        String password = req.path("password").asText("");

        JsonNode user = DataStore.findOne("users", "username", username);
        if (user == null) {
            CodeFlowServer.sendJson(ex, 401, "{\"error\":\"Invalid credentials\"}");
            return;
        }

        if (!BCrypt.checkpw(password, user.path("passwordHash").asText())) {
            CodeFlowServer.sendJson(ex, 401, "{\"error\":\"Invalid credentials\"}");
            return;
        }

        String token = TokenUtil.generate(username);
        String resp = "{\"token\":\"" + token + "\",\"username\":\"" + username
                + "\",\"userId\":\"" + user.path("id").asText()
                + "\",\"email\":\"" + user.path("email").asText() + "\"}";
        CodeFlowServer.sendJson(ex, 200, resp);
    }
}
