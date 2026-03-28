package com.codeflow.api.services;

import com.codeflow.api.models.User;
import com.codeflow.shared.ApiClient;
import com.fasterxml.jackson.databind.JsonNode;

import java.net.http.HttpResponse;
import java.util.Map;

public class AuthService {

    private static User currentUser = null;

    public String register(String username, String email, String password) {
        if (username == null || username.trim().isEmpty()) return "Username is required.";
        if (email == null || !email.contains("@"))        return "A valid email is required.";
        if (password == null || password.length() < 6)    return "Password must be at least 6 characters.";
        try {
            Map<String,String> body = Map.of("username", username.trim(),
                                              "email",    email.trim(),
                                              "password", password);
            HttpResponse<String> resp = ApiClient.postNoAuth("/auth/register", body);
            if (resp.statusCode() == 200) {
                JsonNode node = ApiClient.MAPPER.readTree(resp.body());
                ApiClient.setToken(node.get("token").asText(), node.get("username").asText());
                currentUser = new User(node.get("userId").asText(),
                                       node.get("username").asText(),
                                       node.get("email").asText(), "", null, null);
                return null;
            } else {
                JsonNode err = ApiClient.MAPPER.readTree(resp.body());
                return err.has("error") ? err.get("error").asText() : "Registration failed.";
            }
        } catch (Exception e) {
            return "Cannot connect to server: " + e.getMessage();
        }
    }

    public String login(String username, String password) {
        if (username == null || username.trim().isEmpty()) return "Username is required.";
        if (password == null || password.isEmpty())        return "Password is required.";
        try {
            Map<String,String> body = Map.of("username", username.trim(), "password", password);
            HttpResponse<String> resp = ApiClient.postNoAuth("/auth/login", body);
            if (resp.statusCode() == 200) {
                JsonNode node = ApiClient.MAPPER.readTree(resp.body());
                ApiClient.setToken(node.get("token").asText(), node.get("username").asText());
                currentUser = new User(node.get("userId").asText(),
                                       node.get("username").asText(),
                                       node.get("email").asText(), "", null, null);
                return null;
            } else {
                return "Invalid username or password.";
            }
        } catch (Exception e) {
            return "Cannot connect to server: " + e.getMessage();
        }
    }

    public void logout() { currentUser = null; ApiClient.clearToken(); }

    public static User getCurrentUser() { return currentUser; }
    public boolean isLoggedIn()          { return currentUser != null; }
}
