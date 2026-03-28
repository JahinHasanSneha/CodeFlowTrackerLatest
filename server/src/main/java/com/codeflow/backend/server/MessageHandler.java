package com.codeflow.backend.server;

import com.codeflow.backend.storage.DataStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * /api/messages - real-time messaging between friends only.
 * All messages are persisted to messages.json.
 *
 * Endpoints:
 *   POST /api/messages/send               { receiverUsername, content }
 *   GET  /api/messages/conversation/{user} → list of messages
 *   GET  /api/messages/partners            → list of conversation partners
 *   GET  /api/messages/unread              → { unread: count }
 *   GET  /api/messages/poll?since=ISO     → new messages since timestamp (long-poll)
 */
public class MessageHandler extends BaseHandler {

    @Override
    protected void handleRequest(HttpExchange ex, String username) throws IOException {
        String method = method(ex);
        String path   = path(ex);

        // POST /api/messages/send
        if ("POST".equals(method) && path.endsWith("/send")) {
            JsonNode req = DataStore.MAPPER.readTree(body(ex));
            String receiver = req.path("receiverUsername").asText("").trim();
            String content  = req.path("content").asText("").trim();

            if (receiver.isEmpty() || content.isEmpty()) {
                CodeFlowServer.sendJson(ex, 400, "{\"error\":\"Missing receiver or content\"}");
                return;
            }
            // Only friends can message each other
            if (!FriendHandler.areFriends(username, receiver)) {
                CodeFlowServer.sendJson(ex, 403, "{\"error\":\"You can only message friends\"}");
                return;
            }

            ObjectNode msg = DataStore.MAPPER.createObjectNode();
            msg.put("id",               UUID.randomUUID().toString());
            msg.put("senderUsername",   username);
            msg.put("receiverUsername", receiver);
            msg.put("content",          content);
            String sentAt = LocalDateTime.now().toString();
            msg.put("sentAt",           sentAt);
            msg.putNull("readAt");
            DataStore.insert("messages", msg);
            CodeFlowServer.sendJson(ex, 200, msg.toString());

        // GET /api/messages/conversation/{peer}
        } else if ("GET".equals(method) && path.contains("/conversation/")) {
            String peer = lastSegment(ex);
            ArrayNode result = DataStore.MAPPER.createArrayNode();
            for (JsonNode m : DataStore.readAll("messages")) {
                String sender   = m.path("senderUsername").asText();
                String receiver = m.path("receiverUsername").asText();
                if ((sender.equals(username) && receiver.equals(peer))
                        || (sender.equals(peer) && receiver.equals(username))) {
                    result.add(m);
                }
            }
            // Sort by sentAt
            List<JsonNode> sorted = new ArrayList<>();
            result.forEach(sorted::add);
            sorted.sort(Comparator.comparing(n -> n.path("sentAt").asText("")));
            ArrayNode sortedArr = DataStore.MAPPER.createArrayNode();
            sorted.forEach(sortedArr::add);
            CodeFlowServer.sendJson(ex, 200, sortedArr.toString());

        // GET /api/messages/partners
        } else if ("GET".equals(method) && path.endsWith("/partners")) {
            Set<String> partners = new LinkedHashSet<>();
            for (JsonNode m : DataStore.readAll("messages")) {
                String sender   = m.path("senderUsername").asText();
                String receiver = m.path("receiverUsername").asText();
                if (sender.equals(username)) partners.add(receiver);
                else if (receiver.equals(username)) partners.add(sender);
            }
            ArrayNode result = DataStore.MAPPER.createArrayNode();
            partners.forEach(result::add);
            CodeFlowServer.sendJson(ex, 200, result.toString());

        // GET /api/messages/unread
        } else if ("GET".equals(method) && path.endsWith("/unread")) {
            long count = 0;
            for (JsonNode m : DataStore.readAll("messages")) {
                if (username.equals(m.path("receiverUsername").asText())
                        && m.path("readAt").isNull()) count++;
            }
            CodeFlowServer.sendJson(ex, 200, "{\"unread\":" + count + "}");

        // GET /api/messages/poll?since=ISO&peer=username
        // Returns messages newer than 'since' from/to the given peer
        } else if ("GET".equals(method) && path.endsWith("/poll")) {
            String since = queryParam(ex, "since");
            String peer  = queryParam(ex, "peer");
            if (since == null || peer == null) {
                CodeFlowServer.sendJson(ex, 400, "{\"error\":\"Missing params\"}");
                return;
            }
            ArrayNode result = DataStore.MAPPER.createArrayNode();
            for (JsonNode m : DataStore.readAll("messages")) {
                String sender   = m.path("senderUsername").asText();
                String receiver = m.path("receiverUsername").asText();
                String sentAt   = m.path("sentAt").asText("");
                if (sentAt.compareTo(since) > 0) {
                    if ((sender.equals(username) && receiver.equals(peer))
                            || (sender.equals(peer) && receiver.equals(username))) {
                        result.add(m);
                    }
                }
            }
            CodeFlowServer.sendJson(ex, 200, result.toString());

        } else {
            CodeFlowServer.sendJson(ex, 404, "{\"error\":\"Not found\"}");
        }
    }
}
