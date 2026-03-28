package com.codeflow.api.services;

import com.codeflow.api.models.Project;
import com.codeflow.shared.ApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.*;

public class ProjectService {
    public List<Project> getAllProjects() {
        try {
            HttpResponse<String> r = ApiClient.get("/projects");
            if (r.statusCode() == 200) return parseProjects(ApiClient.MAPPER.readTree(r.body()));
        } catch (Exception e) { e.printStackTrace(); }
        return new ArrayList<>();
    }

    public List<Project> getUpcomingProjects(int days) {
        try {
            HttpResponse<String> r = ApiClient.get("/projects/upcoming?days=" + days);
            if (r.statusCode() == 200) return parseProjects(ApiClient.MAPPER.readTree(r.body()));
        } catch (Exception e) { e.printStackTrace(); }
        return new ArrayList<>();
    }

    public boolean addProject(Project p) {
        try {
            Map<String,Object> body = Map.of(
                "title", p.getTitle(), "description", p.getDescription() == null ? "" : p.getDescription(),
                "dueDate", p.getDueDate().toString(), "color", p.getColor(), "progress", p.getProgress()
            );
            HttpResponse<String> r = ApiClient.post("/projects", body);
            if (r.statusCode() == 200) {
                JsonNode n = ApiClient.MAPPER.readTree(r.body());
                p.setId(n.get("id").asText());
                return true;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    public boolean updateProject(Project p) {
        try {
            Map<String,Object> body = Map.of(
                "title", p.getTitle(), "description", p.getDescription() == null ? "" : p.getDescription(),
                "dueDate", p.getDueDate().toString(), "color", p.getColor(), "progress", p.getProgress()
            );
            return ApiClient.put("/projects/" + p.getId(), body).statusCode() == 200;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public boolean deleteProject(String id) {
        try { return ApiClient.delete("/projects/" + id).statusCode() == 200; }
        catch (Exception e) { e.printStackTrace(); return false; }
    }

    private List<Project> parseProjects(JsonNode arr) {
        List<Project> list = new ArrayList<>();
        for (JsonNode n : arr) {
            Project p = new Project();
            p.setId(n.get("id").asText());
            p.setTitle(n.get("title").asText());
            p.setDescription(n.has("description") ? n.get("description").asText() : "");
            p.setDueDate(LocalDate.parse(n.get("dueDate").asText()));
            p.setColor(n.has("color") ? n.get("color").asText() : "#89b4fa");
            p.setProgress(n.has("progress") ? n.get("progress").asInt() : 0);
            list.add(p);
        }
        return list;
    }
}
