package com.codeflow.backend.server;

import com.codeflow.backend.storage.DataStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

/** /api/leetcode - LeetCode problem tracking (user-specific) */
public class LeetHandler extends BaseHandler {

    @Override
    protected void handleRequest(HttpExchange ex, String username) throws IOException {
        String method = method(ex);
        String path   = path(ex);

        // GET /api/leetcode
        if ("GET".equals(method) && path.endsWith("/leetcode")) {
            ArrayNode all = DataStore.findByUser("leetcode", username);
            CodeFlowServer.sendJson(ex, 200, all.toString());

        // POST /api/leetcode
        } else if ("POST".equals(method) && path.endsWith("/leetcode")) {
            JsonNode req = DataStore.MAPPER.readTree(body(ex));
            boolean solved = req.path("solved").asBoolean(false);
            ObjectNode prob = DataStore.MAPPER.createObjectNode();
            prob.put("id",         UUID.randomUUID().toString());
            prob.put("username",   username);
            prob.put("title",      req.path("title").asText());
            prob.put("tags",       req.path("tags").asText(""));
            prob.put("difficulty", req.path("difficulty").asText("Easy"));
            prob.put("solved",     solved);
            prob.put("url",        req.path("url").asText(""));
            prob.put("solvedAt",   solved ? LocalDateTime.now().toString() : "");
            DataStore.insert("leetcode", prob);
            CodeFlowServer.sendJson(ex, 200, prob.toString());

        // PATCH /api/leetcode/{id}/solve
        } else if ("PATCH".equals(method) && path.contains("/solve")) {
            String id = segmentFromEnd(ex, 1);
            JsonNode existing = findUserProblem(username, id);
            if (existing == null) { CodeFlowServer.sendJson(ex, 404, "{\"error\":\"Not found\"}"); return; }
            ObjectNode updated = (ObjectNode) DataStore.MAPPER.readTree(existing.toString());
            boolean nowSolved = !existing.path("solved").asBoolean(false);
            updated.put("solved",   nowSolved);
            updated.put("solvedAt", nowSolved ? LocalDateTime.now().toString() : "");
            DataStore.updateById("leetcode", id, updated);
            CodeFlowServer.sendJson(ex, 200, updated.toString());

        // DELETE /api/leetcode/{id}
        } else if ("DELETE".equals(method) && path.matches(".*/leetcode/[^/]+")) {
            String id = lastSegment(ex);
            JsonNode existing = findUserProblem(username, id);
            if (existing == null) { CodeFlowServer.sendJson(ex, 404, "{\"error\":\"Not found\"}"); return; }
            DataStore.deleteById("leetcode", id);
            CodeFlowServer.sendJson(ex, 200, "{\"deleted\":true}");

        } else {
            CodeFlowServer.sendJson(ex, 404, "{\"error\":\"Not found\"}");
        }
    }

    private JsonNode findUserProblem(String username, String id) {
        for (JsonNode n : DataStore.readAll("leetcode")) {
            if (id.equals(n.path("id").asText()) && username.equals(n.path("username").asText()))
                return n;
        }
        return null;
    }
}
