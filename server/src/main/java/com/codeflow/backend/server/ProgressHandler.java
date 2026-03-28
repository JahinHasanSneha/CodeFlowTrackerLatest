package com.codeflow.backend.server;

import com.codeflow.backend.storage.DataStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

/** /api/progress - daily problem-solving progress tracking */
public class ProgressHandler extends BaseHandler {

    @Override
    protected void handleRequest(HttpExchange ex, String username) throws IOException {
        String method = method(ex);
        String path   = path(ex);

        // GET /api/progress?days=30
        if ("GET".equals(method) && path.endsWith("/progress")) {
            int days = Integer.parseInt(queryParam(ex, "days") != null ? queryParam(ex, "days") : "30");
            LocalDate since = LocalDate.now().minusDays(days);
            ArrayNode filtered = DataStore.MAPPER.createArrayNode();
            for (JsonNode n : DataStore.findByUser("progress", username)) {
                String dateStr = n.path("progressDate").asText("");
                if (!dateStr.isEmpty() && !LocalDate.parse(dateStr).isBefore(since))
                    filtered.add(n);
            }
            CodeFlowServer.sendJson(ex, 200, filtered.toString());

        // GET /api/progress/stats
        } else if ("GET".equals(method) && path.endsWith("/stats")) {
            ArrayNode all = DataStore.findByUser("progress", username);
            int total = 0, week = 0;
            LocalDate weekAgo = LocalDate.now().minusDays(7);
            for (JsonNode n : all) {
                int solved = n.path("problemsSolved").asInt(0);
                total += solved;
                String dateStr = n.path("progressDate").asText("");
                if (!dateStr.isEmpty() && !LocalDate.parse(dateStr).isBefore(weekAgo)) week += solved;
            }
            String resp = "{\"totalSolved\":" + total + ",\"weekSolved\":" + week + "}";
            CodeFlowServer.sendJson(ex, 200, resp);

        // POST /api/progress/update
        } else if ("POST".equals(method) && path.endsWith("/update")) {
            JsonNode req = DataStore.MAPPER.readTree(body(ex));
            int count = req.path("count").asInt(0);
            String today = LocalDate.now().toString();

            // Find existing entry for today
            JsonNode existing = null;
            for (JsonNode n : DataStore.readAll("progress")) {
                if (username.equals(n.path("username").asText()) && today.equals(n.path("progressDate").asText()))
                    existing = n;
            }

            if (existing != null) {
                ObjectNode updated = (ObjectNode) DataStore.MAPPER.readTree(existing.toString());
                updated.put("problemsSolved", count);
                DataStore.updateById("progress", existing.path("id").asText(), updated);
                CodeFlowServer.sendJson(ex, 200, updated.toString());
            } else {
                ObjectNode entry = DataStore.MAPPER.createObjectNode();
                entry.put("id",             UUID.randomUUID().toString());
                entry.put("username",       username);
                entry.put("progressDate",   today);
                entry.put("problemsSolved", count);
                DataStore.insert("progress", entry);
                CodeFlowServer.sendJson(ex, 200, entry.toString());
            }

        } else {
            CodeFlowServer.sendJson(ex, 404, "{\"error\":\"Not found\"}");
        }
    }
}
