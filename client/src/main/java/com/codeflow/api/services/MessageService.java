package com.codeflow.api.services;

import com.codeflow.api.models.Message;
import com.codeflow.shared.ApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.*;

public class MessageService {

    public List<Message> getConversation(String otherUsername) {
        try {
            HttpResponse<String> r = ApiClient.get("/messages/conversation/" + otherUsername);
            if (r.statusCode() == 200) return parseMessages(ApiClient.MAPPER.readTree(r.body()));
        } catch (Exception e) { e.printStackTrace(); }
        return new ArrayList<>();
    }

    public List<String> getConversationPartners() {
        try {
            HttpResponse<String> r = ApiClient.get("/messages/partners");
            if (r.statusCode() == 200) {
                List<String> partners = new ArrayList<>();
                for (JsonNode n : ApiClient.MAPPER.readTree(r.body())) partners.add(n.asText());
                return partners;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return new ArrayList<>();
    }

    public int getUnreadCount() {
        try {
            HttpResponse<String> r = ApiClient.get("/messages/unread");
            if (r.statusCode() == 200) return ApiClient.MAPPER.readTree(r.body()).get("unread").asInt();
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    public Message sendMessage(String receiverUsername, String content) {
        try {
            Map<String,String> body = Map.of("receiverUsername", receiverUsername, "content", content);
            HttpResponse<String> r = ApiClient.post("/messages/send", body);
            if (r.statusCode() == 200) {
                JsonNode n = ApiClient.MAPPER.readTree(r.body());
                Message msg = new Message();
                msg.setId(n.get("id").asText());
                msg.setSenderUsername(n.get("senderUsername").asText());
                msg.setReceiverUsername(n.get("receiverUsername").asText());
                msg.setContent(n.get("content").asText());
                if (n.has("sentAt") && !n.get("sentAt").isNull())
                    msg.setSentAt(LocalDateTime.parse(n.get("sentAt").asText()));
                return msg;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    /** Poll for new messages since a given timestamp (for real-time updates) */
    public List<Message> pollMessages(String peer, String since) {
        try {
            String encoded = java.net.URLEncoder.encode(since, "UTF-8");
            HttpResponse<String> r = ApiClient.get("/messages/poll?peer=" + peer + "&since=" + encoded);
            if (r.statusCode() == 200) return parseMessages(ApiClient.MAPPER.readTree(r.body()));
        } catch (Exception e) { /* silent - poll failure is normal */ }
        return new ArrayList<>();
    }

    private List<Message> parseMessages(JsonNode arr) {
        List<Message> list = new ArrayList<>();
        for (JsonNode n : arr) {
            Message m = new Message();
            m.setId(n.get("id").asText());
            m.setSenderUsername(n.get("senderUsername").asText());
            m.setReceiverUsername(n.get("receiverUsername").asText());
            m.setContent(n.get("content").asText());
            if (n.has("sentAt") && !n.get("sentAt").isNull() && !n.get("sentAt").asText().isEmpty()) {
                try { m.setSentAt(LocalDateTime.parse(n.get("sentAt").asText())); } catch (Exception ignore) {}
            }
            list.add(m);
        }
        return list;
    }
}
