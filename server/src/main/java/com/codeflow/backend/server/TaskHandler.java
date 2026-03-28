package com.codeflow.backend.server;

import com.codeflow.backend.storage.DataStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

/** /api/tasks - user-specific task management */
public class TaskHandler extends BaseHandler {

    @Override
    protected void handleRequest(HttpExchange ex, String username) throws IOException {
        String method = method(ex);
        String path   = path(ex);

        // GET /api/tasks
        if ("GET".equals(method) && path.endsWith("/tasks")) {
            ArrayNode all  = DataStore.findByUser("tasks", username);
            CodeFlowServer.sendJson(ex, 200, all.toString());

        // POST /api/tasks
        } else if ("POST".equals(method) && path.endsWith("/tasks")) {
            JsonNode req = DataStore.MAPPER.readTree(body(ex));
            ObjectNode task = DataStore.MAPPER.createObjectNode();
            task.put("id",          UUID.randomUUID().toString());
            task.put("username",    username);
            task.put("title",       req.path("title").asText());
            task.put("description", req.path("description").asText(""));
            task.put("priority",    req.path("priority").asText("MEDIUM"));
            task.put("dueDate",     req.path("dueDate").asText(""));
            task.put("category",    req.path("category").asText(""));
            task.put("completed",   false);
            task.put("createdAt",   LocalDateTime.now().toString());
            task.putNull("completedAt");
            DataStore.insert("tasks", task);
            CodeFlowServer.sendJson(ex, 200, task.toString());

        // PUT /api/tasks/{id}
        } else if ("PUT".equals(method) && path.matches(".*/tasks/[^/]+")) {
            String id = lastSegment(ex);
            JsonNode existing = findUserTask(username, id);
            if (existing == null) { CodeFlowServer.sendJson(ex, 404, "{\"error\":\"Not found\"}"); return; }
            JsonNode req = DataStore.MAPPER.readTree(body(ex));
            ObjectNode updated = (ObjectNode) DataStore.MAPPER.readTree(existing.toString());
            updated.put("title",       req.path("title").asText(existing.path("title").asText()));
            updated.put("description", req.path("description").asText(existing.path("description").asText()));
            updated.put("priority",    req.path("priority").asText(existing.path("priority").asText()));
            updated.put("dueDate",     req.path("dueDate").asText(existing.path("dueDate").asText()));
            updated.put("category",    req.path("category").asText(existing.path("category").asText()));
            DataStore.updateById("tasks", id, updated);
            CodeFlowServer.sendJson(ex, 200, updated.toString());

        // PATCH /api/tasks/{id}/complete
        } else if ("PATCH".equals(method) && path.contains("/complete")) {
            String id = segmentFromEnd(ex, 1);
            JsonNode existing = findUserTask(username, id);
            if (existing == null) { CodeFlowServer.sendJson(ex, 404, "{\"error\":\"Not found\"}"); return; }
            ObjectNode updated = (ObjectNode) DataStore.MAPPER.readTree(existing.toString());
            updated.put("completed",   true);
            updated.put("completedAt", LocalDateTime.now().toString());
            DataStore.updateById("tasks", id, updated);
            CodeFlowServer.sendJson(ex, 200, updated.toString());

        // DELETE /api/tasks/{id}
        } else if ("DELETE".equals(method) && path.matches(".*/tasks/[^/]+")) {
            String id = lastSegment(ex);
            JsonNode existing = findUserTask(username, id);
            if (existing == null) { CodeFlowServer.sendJson(ex, 404, "{\"error\":\"Not found\"}"); return; }
            DataStore.deleteById("tasks", id);
            CodeFlowServer.sendJson(ex, 200, "{\"deleted\":true}");

        } else {
            CodeFlowServer.sendJson(ex, 404, "{\"error\":\"Not found\"}");
        }
    }

    private JsonNode findUserTask(String username, String id) {
        for (JsonNode n : DataStore.readAll("tasks")) {
            if (id.equals(n.path("id").asText()) && username.equals(n.path("username").asText()))
                return n;
        }
        return null;
    }
}
