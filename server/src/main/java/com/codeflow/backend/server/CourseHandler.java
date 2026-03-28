package com.codeflow.backend.server;

import com.codeflow.backend.storage.DataStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.URLDecoder;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * /api/courses
 *
 * GET    /api/courses                                       → list all courses for user
 * POST   /api/courses                                       → add course {code,name,instructor,imagePath}
 * DELETE /api/courses/{courseCode}                          → delete course + all its data
 * GET    /api/courses/{courseCode}                          → get course content
 * POST   /api/courses/{courseCode}/{type}                   → add item
 * PATCH  /api/courses/{courseCode}/{type}/{id}/complete     → mark complete
 * PATCH  /api/courses/{courseCode}/{type}/{id}/uncomplete   → undo complete
 * DELETE /api/courses/{courseCode}/{type}/{id}              → delete item
 */
public class CourseHandler extends BaseHandler {

    private static final String TABLE     = "course_data";
    private static final String HUB_TABLE = "courses";

    @Override
    protected void handleRequest(HttpExchange ex, String username) throws IOException {
        String method  = method(ex);
        String rawPath = path(ex);
        String path;
        try { path = URLDecoder.decode(rawPath, "UTF-8"); }
        catch (Exception e) { path = rawPath; }

        // ── GET /api/courses  (list)
        if ("GET".equals(method) && path.endsWith("/courses")) {
            ArrayNode result = DataStore.MAPPER.createArrayNode();
            for (JsonNode n : DataStore.readAll(HUB_TABLE)) {
                if (username.equals(n.path("username").asText())) result.add(n);
            }
            CodeFlowServer.sendJson(ex, 200, result.toString());
            return;
        }

        // ── POST /api/courses  (create)
        if ("POST".equals(method) && path.endsWith("/courses")) {
            JsonNode req = DataStore.MAPPER.readTree(body(ex));
            ObjectNode course = DataStore.MAPPER.createObjectNode();
            course.put("id",         UUID.randomUUID().toString());
            course.put("username",   username);
            course.put("code",       req.path("code").asText());
            course.put("name",       req.path("name").asText());
            course.put("instructor", req.path("instructor").asText(""));
            course.put("imagePath",  req.path("imagePath").asText(""));
            course.put("createdAt",  LocalDateTime.now().toString());
            DataStore.insert(HUB_TABLE, course);
            CodeFlowServer.sendJson(ex, 200, course.toString());
            return;
        }

        // All remaining routes need at least one segment after /courses/
        // Extract segments after /api/courses/
        String afterCourses = path.replaceFirst(".*/courses/", "");
        String[] seg = afterCourses.split("/");
        // seg[0]=courseCode, seg[1]=type, seg[2]=itemId, seg[3]=action

        if (seg.length == 0 || seg[0].isEmpty()) {
            CodeFlowServer.sendJson(ex, 404, "{\"error\":\"Not found\"}");
            return;
        }

        String courseCode = seg[0];
        String dataKey    = username + ":" + courseCode;

        // ── DELETE /api/courses/{courseCode}
        if ("DELETE".equals(method) && seg.length == 1) {
            // Remove from courses list
            ArrayNode allCourses = DataStore.readAll(HUB_TABLE);
            ArrayNode newCourses = DataStore.MAPPER.createArrayNode();
            for (JsonNode n : allCourses) {
                boolean mine     = username.equals(n.path("username").asText());
                boolean sameCode = courseCode.equals(n.path("code").asText());
                if (!(mine && sameCode)) newCourses.add(n);
            }
            DataStore.writeAll(HUB_TABLE, newCourses);
            // Remove course content
            ArrayNode allData = DataStore.readAll(TABLE);
            ArrayNode newData = DataStore.MAPPER.createArrayNode();
            for (JsonNode n : allData) {
                if (!dataKey.equals(n.path("key").asText())) newData.add(n);
            }
            DataStore.writeAll(TABLE, newData);
            CodeFlowServer.sendJson(ex, 200, "{\"deleted\":true}");
            return;
        }

        // ── GET /api/courses/{courseCode}
        if ("GET".equals(method) && seg.length == 1) {
            JsonNode doc = DataStore.findOne(TABLE, "key", dataKey);
            if (doc == null) {
                ObjectNode empty = DataStore.MAPPER.createObjectNode();
                empty.put("key", dataKey);
                empty.putArray("notes");
                empty.putArray("resources");
                empty.putArray("slides");
                empty.putArray("tasks");
                CodeFlowServer.sendJson(ex, 200, empty.toString());
            } else {
                CodeFlowServer.sendJson(ex, 200, doc.toString());
            }
            return;
        }

        // ── POST /api/courses/{courseCode}/{type}
        if ("POST".equals(method) && seg.length == 2) {
            String type = seg[1];
            if (!isValidType(type)) {
                CodeFlowServer.sendJson(ex, 400, "{\"error\":\"Invalid type\"}");
                return;
            }
            JsonNode req  = DataStore.MAPPER.readTree(body(ex));
            ObjectNode item = DataStore.MAPPER.createObjectNode();
            item.put("id",        UUID.randomUUID().toString());
            item.put("completed", false);
            item.put("createdAt", LocalDateTime.now().toString());
            switch (type) {
                case "notes":
                    item.put("text", req.path("text").asText());
                    break;
                case "resources": case "slides":
                    item.put("name", req.path("name").asText(""));
                    item.put("link", req.path("link").asText(""));
                    break;
                case "tasks":
                    item.put("title", req.path("title").asText());
                    break;
            }
            synchronized (TABLE.intern()) {
                ObjectNode doc = getOrCreateDoc(dataKey);
                ((ArrayNode) doc.get(type)).add(item);
                saveDoc(dataKey, doc);
            }
            CodeFlowServer.sendJson(ex, 200, item.toString());
            return;
        }

        // ── PATCH /api/courses/{courseCode}/{type}/{id}/complete|uncomplete
        if ("PATCH".equals(method) && seg.length == 4) {
            String type   = seg[1];
            String itemId = seg[2];
            String action = seg[3];
            if (!isValidType(type) || (!action.equals("complete") && !action.equals("uncomplete"))) {
                CodeFlowServer.sendJson(ex, 400, "{\"error\":\"Invalid request\"}");
                return;
            }
            boolean complete = "complete".equals(action);
            synchronized (TABLE.intern()) {
                ObjectNode doc = getOrCreateDoc(dataKey);
                ArrayNode arr  = (ArrayNode) doc.get(type);
                if (arr == null) { CodeFlowServer.sendJson(ex, 404, "{\"error\":\"Not found\"}"); return; }
                ArrayNode newArr = DataStore.MAPPER.createArrayNode();
                boolean found = false;
                for (JsonNode n : arr) {
                    if (itemId.equals(n.path("id").asText())) {
                        ObjectNode u = (ObjectNode) n.deepCopy();
                        u.put("completed", complete);
                        newArr.add(u);
                        found = true;
                    } else newArr.add(n);
                }
                if (!found) { CodeFlowServer.sendJson(ex, 404, "{\"error\":\"Item not found\"}"); return; }
                doc.set(type, newArr);
                saveDoc(dataKey, doc);
            }
            CodeFlowServer.sendJson(ex, 200, "{\"ok\":true}");
            return;
        }

        // ── DELETE /api/courses/{courseCode}/{type}/{id}
        if ("DELETE".equals(method) && seg.length == 3) {
            String type   = seg[1];
            String itemId = seg[2];
            if (!isValidType(type)) { CodeFlowServer.sendJson(ex, 400, "{\"error\":\"Invalid type\"}"); return; }
            synchronized (TABLE.intern()) {
                ObjectNode doc = getOrCreateDoc(dataKey);
                ArrayNode arr  = (ArrayNode) doc.get(type);
                if (arr == null) { CodeFlowServer.sendJson(ex, 404, "{\"error\":\"Not found\"}"); return; }
                ArrayNode newArr = DataStore.MAPPER.createArrayNode();
                boolean found = false;
                for (JsonNode n : arr) {
                    if (itemId.equals(n.path("id").asText())) found = true;
                    else newArr.add(n);
                }
                if (!found) { CodeFlowServer.sendJson(ex, 404, "{\"error\":\"Item not found\"}"); return; }
                doc.set(type, newArr);
                saveDoc(dataKey, doc);
            }
            CodeFlowServer.sendJson(ex, 200, "{\"deleted\":true}");
            return;
        }

        CodeFlowServer.sendJson(ex, 404, "{\"error\":\"Not found\"}");
    }

    private boolean isValidType(String t) {
        return "notes".equals(t) || "resources".equals(t) || "slides".equals(t) || "tasks".equals(t);
    }

    private ObjectNode getOrCreateDoc(String key) {
        JsonNode existing = DataStore.findOne(TABLE, "key", key);
        if (existing != null) return (ObjectNode) existing.deepCopy();
        ObjectNode doc = DataStore.MAPPER.createObjectNode();
        doc.put("key", key);
        doc.putArray("notes");
        doc.putArray("resources");
        doc.putArray("slides");
        doc.putArray("tasks");
        return doc;
    }

    private void saveDoc(String key, ObjectNode doc) {
        ArrayNode all    = DataStore.readAll(TABLE);
        ArrayNode newAll = DataStore.MAPPER.createArrayNode();
        boolean found    = false;
        for (JsonNode n : all) {
            if (key.equals(n.path("key").asText())) { newAll.add(doc); found = true; }
            else newAll.add(n);
        }
        if (!found) newAll.add(doc);
        DataStore.writeAll(TABLE, newAll);
    }
}
