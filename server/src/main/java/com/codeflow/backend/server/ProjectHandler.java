package com.codeflow.backend.server;

import com.codeflow.backend.storage.DataStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

/** /api/projects - user-specific project calendar */
public class ProjectHandler extends BaseHandler {

    @Override
    protected void handleRequest(HttpExchange ex, String username) throws IOException {
        String method = method(ex);
        String path   = path(ex);

        // GET /api/projects
        if ("GET".equals(method) && path.endsWith("/projects")) {
            ArrayNode all = DataStore.findByUser("projects", username);
            CodeFlowServer.sendJson(ex, 200, all.toString());

        // GET /api/projects/upcoming?days=7
        } else if ("GET".equals(method) && path.endsWith("/upcoming")) {
            int days = Integer.parseInt(queryParam(ex, "days") != null ? queryParam(ex, "days") : "7");
            LocalDate limit = LocalDate.now().plusDays(days);
            ArrayNode filtered = DataStore.MAPPER.createArrayNode();
            for (JsonNode p : DataStore.findByUser("projects", username)) {
                String dueDateStr = p.path("dueDate").asText("");
                if (!dueDateStr.isEmpty()) {
                    LocalDate due = LocalDate.parse(dueDateStr);
                    if (!due.isBefore(LocalDate.now()) && !due.isAfter(limit)) filtered.add(p);
                }
            }
            CodeFlowServer.sendJson(ex, 200, filtered.toString());

        // POST /api/projects
        } else if ("POST".equals(method) && path.endsWith("/projects")) {
            JsonNode req = DataStore.MAPPER.readTree(body(ex));
            ObjectNode proj = DataStore.MAPPER.createObjectNode();
            proj.put("id",          UUID.randomUUID().toString());
            proj.put("username",    username);
            proj.put("title",       req.path("title").asText());
            proj.put("description", req.path("description").asText(""));
            proj.put("dueDate",     req.path("dueDate").asText());
            proj.put("color",       req.path("color").asText("#89b4fa"));
            proj.put("progress",    req.path("progress").asInt(0));
            DataStore.insert("projects", proj);
            CodeFlowServer.sendJson(ex, 200, proj.toString());

        // PUT /api/projects/{id}
        } else if ("PUT".equals(method) && path.matches(".*/projects/[^/]+")) {
            String id = lastSegment(ex);
            JsonNode existing = findUserProject(username, id);
            if (existing == null) { CodeFlowServer.sendJson(ex, 404, "{\"error\":\"Not found\"}"); return; }
            JsonNode req = DataStore.MAPPER.readTree(body(ex));
            ObjectNode updated = (ObjectNode) DataStore.MAPPER.readTree(existing.toString());
            updated.put("title",       req.path("title").asText(existing.path("title").asText()));
            updated.put("description", req.path("description").asText(existing.path("description").asText()));
            updated.put("dueDate",     req.path("dueDate").asText(existing.path("dueDate").asText()));
            updated.put("color",       req.path("color").asText(existing.path("color").asText()));
            updated.put("progress",    req.path("progress").asInt(existing.path("progress").asInt(0)));
            DataStore.updateById("projects", id, updated);
            CodeFlowServer.sendJson(ex, 200, updated.toString());

        // DELETE /api/projects/{id}
        } else if ("DELETE".equals(method) && path.matches(".*/projects/[^/]+")) {
            String id = lastSegment(ex);
            JsonNode existing = findUserProject(username, id);
            if (existing == null) { CodeFlowServer.sendJson(ex, 404, "{\"error\":\"Not found\"}"); return; }
            DataStore.deleteById("projects", id);
            CodeFlowServer.sendJson(ex, 200, "{\"deleted\":true}");

        } else {
            CodeFlowServer.sendJson(ex, 404, "{\"error\":\"Not found\"}");
        }
    }

    private JsonNode findUserProject(String username, String id) {
        for (JsonNode n : DataStore.readAll("projects")) {
            if (id.equals(n.path("id").asText()) && username.equals(n.path("username").asText()))
                return n;
        }
        return null;
    }
}
