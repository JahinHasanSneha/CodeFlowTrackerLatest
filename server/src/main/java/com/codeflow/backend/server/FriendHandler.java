package com.codeflow.backend.server;

import com.codeflow.backend.storage.DataStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * /api/friends - friend request, accept/decline, list, remove
 *
 * Friendship table record:
 *   { id, fromUser, toUser, status: PENDING|ACCEPTED|DECLINED|BLOCKED, createdAt }
 */
public class FriendHandler extends BaseHandler {

    @Override
    protected void handleRequest(HttpExchange ex, String username) throws IOException {
        String method = method(ex);
        String path   = path(ex);

        // POST /api/friends/request   body: {username: "target"}
        if ("POST".equals(method) && path.endsWith("/request")) {
            JsonNode req = DataStore.MAPPER.readTree(body(ex));
            String target = req.path("username").asText("").trim();

            if (target.isEmpty() || target.equals(username)) {
                CodeFlowServer.sendJson(ex, 400, "{\"error\":\"Invalid target\"}");
                return;
            }
            // Check target user exists
            if (DataStore.findOne("users", "username", target) == null) {
                CodeFlowServer.sendJson(ex, 404, "{\"error\":\"User not found\"}");
                return;
            }
            // Check if friendship already exists
            if (friendshipExists(username, target)) {
                CodeFlowServer.sendJson(ex, 400, "{\"error\":\"Already friends or request pending\"}");
                return;
            }
            ObjectNode friendship = DataStore.MAPPER.createObjectNode();
            friendship.put("id",        UUID.randomUUID().toString());
            friendship.put("fromUser",  username);
            friendship.put("toUser",    target);
            friendship.put("status",    "PENDING");
            friendship.put("createdAt", LocalDateTime.now().toString());
            DataStore.insert("friendships", friendship);
            CodeFlowServer.sendJson(ex, 200, "{\"message\":\"Friend request sent to " + target + "\"}");

        // POST /api/friends/respond/{id}   body: {action: "accept"|"decline"}
        } else if ("POST".equals(method) && path.contains("/respond/")) {
            String id = lastSegment(ex);
            JsonNode req = DataStore.MAPPER.readTree(body(ex));
            String action = req.path("action").asText("");

            JsonNode fs = DataStore.findOne("friendships", "id", id);
            if (fs == null || !username.equals(fs.path("toUser").asText())) {
                CodeFlowServer.sendJson(ex, 404, "{\"error\":\"Request not found\"}");
                return;
            }
            ObjectNode updated = (ObjectNode) DataStore.MAPPER.readTree(fs.toString());
            updated.put("status", "accept".equals(action) ? "ACCEPTED" : "DECLINED");
            DataStore.updateById("friendships", id, updated);
            CodeFlowServer.sendJson(ex, 200, "{\"message\":\"Done\"}");

        // GET /api/friends/pending - incoming requests for current user
        } else if ("GET".equals(method) && path.endsWith("/pending")) {
            ArrayNode result = DataStore.MAPPER.createArrayNode();
            for (JsonNode fs : DataStore.readAll("friendships")) {
                if (username.equals(fs.path("toUser").asText())
                        && "PENDING".equals(fs.path("status").asText())) {
                    ObjectNode item = DataStore.MAPPER.createObjectNode();
                    item.put("id",   fs.path("id").asText());
                    item.put("from", fs.path("fromUser").asText());
                    result.add(item);
                }
            }
            CodeFlowServer.sendJson(ex, 200, result.toString());

        // GET /api/friends/list - accepted friends
        } else if ("GET".equals(method) && path.endsWith("/list")) {
            ArrayNode result = DataStore.MAPPER.createArrayNode();
            for (JsonNode fs : DataStore.readAll("friendships")) {
                if (!"ACCEPTED".equals(fs.path("status").asText())) continue;
                String from = fs.path("fromUser").asText();
                String to   = fs.path("toUser").asText();
                String friend = null;
                if (username.equals(from)) friend = to;
                else if (username.equals(to)) friend = from;
                if (friend != null) {
                    ObjectNode item = DataStore.MAPPER.createObjectNode();
                    item.put("username", friend);
                    result.add(item);
                }
            }
            CodeFlowServer.sendJson(ex, 200, result.toString());

        // DELETE /api/friends/remove   body: {username: "target"}
        } else if ("DELETE".equals(method) && path.endsWith("/remove")) {
            JsonNode req = DataStore.MAPPER.readTree(body(ex));
            String target = req.path("username").asText("").trim();
            removeFriendship(username, target);
            CodeFlowServer.sendJson(ex, 200, "{\"message\":\"Removed\"}");

        // POST /api/friends/block   body: {username: "target"}
        } else if ("POST".equals(method) && path.endsWith("/block")) {
            JsonNode req = DataStore.MAPPER.readTree(body(ex));
            String target = req.path("username").asText("").trim();
            blockUser(username, target);
            CodeFlowServer.sendJson(ex, 200, "{\"message\":\"Blocked\"}");

        // GET /api/friends/check?username=x  (check if they're friends - used by messaging)
        } else if ("GET".equals(method) && path.endsWith("/check")) {
            String target = queryParam(ex, "username");
            boolean areFriends = target != null && areFriends(username, target);
            CodeFlowServer.sendJson(ex, 200, "{\"areFriends\":" + areFriends + "}");

        } else {
            CodeFlowServer.sendJson(ex, 404, "{\"error\":\"Not found\"}");
        }
    }

    private boolean friendshipExists(String a, String b) {
        for (JsonNode fs : DataStore.readAll("friendships")) {
            String from = fs.path("fromUser").asText();
            String to   = fs.path("toUser").asText();
            String status = fs.path("status").asText();
            if (!status.equals("DECLINED") && !status.equals("BLOCKED")) {
                if ((from.equals(a) && to.equals(b)) || (from.equals(b) && to.equals(a))) return true;
            }
        }
        return false;
    }

    public static boolean areFriends(String a, String b) {
        for (JsonNode fs : DataStore.readAll("friendships")) {
            if (!"ACCEPTED".equals(fs.path("status").asText())) continue;
            String from = fs.path("fromUser").asText();
            String to   = fs.path("toUser").asText();
            if ((from.equals(a) && to.equals(b)) || (from.equals(b) && to.equals(a))) return true;
        }
        return false;
    }

    private void removeFriendship(String a, String b) {
        ArrayNode all = DataStore.readAll("friendships");
        ArrayNode keep = DataStore.MAPPER.createArrayNode();
        for (JsonNode fs : all) {
            String from = fs.path("fromUser").asText();
            String to   = fs.path("toUser").asText();
            if (!((from.equals(a) && to.equals(b)) || (from.equals(b) && to.equals(a)))) keep.add(fs);
        }
        DataStore.writeAll("friendships", keep);
    }

    private void blockUser(String blocker, String blocked) {
        // Remove existing friendship and create a block record
        removeFriendship(blocker, blocked);
        ObjectNode block = DataStore.MAPPER.createObjectNode();
        block.put("id",        UUID.randomUUID().toString());
        block.put("fromUser",  blocker);
        block.put("toUser",    blocked);
        block.put("status",    "BLOCKED");
        block.put("createdAt", LocalDateTime.now().toString());
        DataStore.insert("friendships", block);
    }
}
