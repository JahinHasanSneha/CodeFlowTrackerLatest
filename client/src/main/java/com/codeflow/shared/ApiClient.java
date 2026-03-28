package com.codeflow.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Thin HTTP wrapper. SERVER_URL priority:
 *   1. System property  -Dserver.url=https://myapp.railway.app
 *   2. Env var          CODEFLOW_SERVER_URL
 *   3. Default          http://localhost:8080
 */
public class ApiClient {

    public static final String BASE_URL;
    static {
        String p = System.getProperty("server.url");
        String e = System.getenv("CODEFLOW_SERVER_URL");
        if (p != null && !p.isEmpty()) BASE_URL = p.trim().replaceAll("/+$", "") + "/api";
        else if (e != null && !e.isEmpty()) BASE_URL = e.trim().replaceAll("/+$", "") + "/api";
        else BASE_URL = "http://localhost:8080/api";
    }

    private static String jwtToken = null;
    private static String currentUsername = null;

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    public static void setToken(String token, String username) { jwtToken = token; currentUsername = username; }
    public static void clearToken() { jwtToken = null; currentUsername = null; }
    public static String getCurrentUsername() { return currentUsername; }
    public static boolean isLoggedIn()         { return jwtToken != null; }
    public static String getToken()            { return jwtToken; }

    public static HttpResponse<String> get(String path) throws Exception {
        return HTTP.send(HttpRequest.newBuilder().uri(URI.create(BASE_URL + path))
                .header("Authorization", "Bearer " + jwtToken).timeout(Duration.ofSeconds(15)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    public static HttpResponse<String> post(String path, Object body) throws Exception {
        String json = MAPPER.writeValueAsString(body);
        return HTTP.send(HttpRequest.newBuilder().uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json").header("Authorization", "Bearer " + jwtToken)
                .timeout(Duration.ofSeconds(15)).POST(HttpRequest.BodyPublishers.ofString(json)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    public static HttpResponse<String> postNoAuth(String path, Object body) throws Exception {
        String json = MAPPER.writeValueAsString(body);
        return HTTP.send(HttpRequest.newBuilder().uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json").timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(json)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    public static HttpResponse<String> put(String path, Object body) throws Exception {
        String json = MAPPER.writeValueAsString(body);
        return HTTP.send(HttpRequest.newBuilder().uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json").header("Authorization", "Bearer " + jwtToken)
                .timeout(Duration.ofSeconds(15)).PUT(HttpRequest.BodyPublishers.ofString(json)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    public static HttpResponse<String> patch(String path) throws Exception {
        return HTTP.send(HttpRequest.newBuilder().uri(URI.create(BASE_URL + path))
                .header("Authorization", "Bearer " + jwtToken).timeout(Duration.ofSeconds(15))
                .method("PATCH", HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    public static HttpResponse<String> delete(String path) throws Exception {
        return HTTP.send(HttpRequest.newBuilder().uri(URI.create(BASE_URL + path))
                .header("Authorization", "Bearer " + jwtToken).timeout(Duration.ofSeconds(15))
                .DELETE().build(), HttpResponse.BodyHandlers.ofString());
    }

    public static HttpResponse<String> deleteWithBody(String path, Object body) throws Exception {
        String json = MAPPER.writeValueAsString(body);
        return HTTP.send(HttpRequest.newBuilder().uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json").header("Authorization", "Bearer " + jwtToken)
                .timeout(Duration.ofSeconds(15))
                .method("DELETE", HttpRequest.BodyPublishers.ofString(json)).build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
