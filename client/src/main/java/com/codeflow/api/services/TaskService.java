package com.codeflow.api.services;

import com.codeflow.api.models.Task;
import com.codeflow.api.models.Priority;
import com.codeflow.shared.ApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.*;

public class TaskService {
    public List<Task> getAllTasks() {
        try {
            HttpResponse<String> r = ApiClient.get("/tasks");
            if (r.statusCode() == 200) return parseTasks(ApiClient.MAPPER.readTree(r.body()));
        } catch (Exception e) { e.printStackTrace(); }
        return new ArrayList<>();
    }

    public boolean addTask(Task t) {
        try {
            Map<String,Object> body = Map.of(
                "title",       t.getTitle(),
                "description", t.getDescription() == null ? "" : t.getDescription(),
                "priority",    t.getPriority().name(),
                "dueDate",     t.getDueDate() != null ? t.getDueDate().toString() : "",
                "category",    t.getCategory() == null ? "" : t.getCategory()
            );
            HttpResponse<String> r = ApiClient.post("/tasks", body);
            if (r.statusCode() == 200) {
                JsonNode n = ApiClient.MAPPER.readTree(r.body());
                t.setId(n.get("id").asText());
                return true;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    public boolean updateTask(Task t) {
        try {
            Map<String,Object> body = Map.of(
                "title",       t.getTitle(),
                "description", t.getDescription() == null ? "" : t.getDescription(),
                "priority",    t.getPriority().name(),
                "dueDate",     t.getDueDate() != null ? t.getDueDate().toString() : "",
                "category",    t.getCategory() == null ? "" : t.getCategory()
            );
            if (t.isCompleted()) ApiClient.patch("/tasks/" + t.getId() + "/complete");
            else                 ApiClient.put("/tasks/" + t.getId(), body);
            return true;
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    public boolean deleteTask(String id) {
        try { return ApiClient.delete("/tasks/" + id).statusCode() == 200; }
        catch (Exception e) { e.printStackTrace(); return false; }
    }

    private List<Task> parseTasks(JsonNode arr) {
        List<Task> list = new ArrayList<>();
        for (JsonNode n : arr) {
            Task t = new Task();
            t.setId(n.get("id").asText());
            t.setTitle(n.get("title").asText());
            t.setDescription(n.has("description") ? n.get("description").asText() : "");
            t.setPriority(Priority.valueOf(n.get("priority").asText()));
            t.setCompleted(n.get("completed").asBoolean());
            t.setCategory(n.has("category") ? n.get("category").asText() : "");
            if (n.has("dueDate") && !n.get("dueDate").isNull())
                t.setDueDate(LocalDate.parse(n.get("dueDate").asText()));
            list.add(t);
        }
        return list;
    }
}
