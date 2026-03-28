package com.codeflow.backend.server;

import com.codeflow.backend.storage.DataStore;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Lightweight HTTP server (no Spring Boot / SQL).
 * Handles all REST API endpoints used by the JavaFX frontend.
 * Deploy this jar on Railway; the frontend connects to it over HTTP.
 */
public class CodeFlowServer {

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        DataStore.init(); // ensure data directories exist

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newCachedThreadPool());

        // Register all route handlers
        AuthHandler    auth    = new AuthHandler();
        TaskHandler    task    = new TaskHandler();
        ProjectHandler project = new ProjectHandler();
        LeetHandler    leet    = new LeetHandler();
        ProgressHandler prog   = new ProgressHandler();
        FriendHandler  friend  = new FriendHandler();
        MessageHandler msg     = new MessageHandler();

        server.createContext("/api/auth",     auth);
        server.createContext("/api/tasks",    task);
        server.createContext("/api/projects", project);
        server.createContext("/api/leetcode", leet);
        server.createContext("/api/progress", prog);
        server.createContext("/api/friends",  friend);
        server.createContext("/api/messages", msg);
        CourseHandler  course  = new CourseHandler();
        server.createContext("/api/courses",  course);

               // Health check
        //  server.createContext("/health", ex -> sendJson(ex, 200, "{\"status\":\"ok\"}"));
// Health check - Accepts requests from any hostname
        server.createContext("/health", ex -> {
            System.out.println("✅ Health check received");
            ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            ex.getResponseHeaders().set("Content-Type", "application/json");
            String response = "{\"status\":\"ok\"}";
            byte[] bytes = response.getBytes("UTF-8");
            ex.sendResponseHeaders(200, bytes.length);
            try (var os = ex.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
        System.out.println("CodeFlow Server started on port " + port);
    }

    // ── Shared HTTP helpers ──────────────────────────────────────────────────

    public static void sendJson(HttpExchange ex, int code, String json) throws IOException {
        byte[] bytes = json.getBytes("UTF-8");
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        addCors(ex);
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    public static void addCors(HttpExchange ex) {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,PATCH,OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type,Authorization");
    }

    public static void handleOptions(HttpExchange ex) throws IOException {
        addCors(ex);
        ex.sendResponseHeaders(204, -1);
    }

    public static String readBody(HttpExchange ex) throws IOException {
        return new String(ex.getRequestBody().readAllBytes(), "UTF-8");
    }

    public static String extractToken(HttpExchange ex) {
        String auth = ex.getRequestHeaders().getFirst("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) return auth.substring(7);
        return null;
    }
}
