package com.codeflow.api.services;

import com.codeflow.api.models.LeetCodeProblem;
import com.codeflow.shared.ApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpResponse;
import java.util.*;

public class LeetCodeService {
    public List<LeetCodeProblem> getAllProblems() {
        try {
            HttpResponse<String> r = ApiClient.get("/leetcode");
            if (r.statusCode() == 200) return parseProblems(ApiClient.MAPPER.readTree(r.body()));
        } catch (Exception e) { e.printStackTrace(); }
        return new ArrayList<>();
    }

    public boolean addProblem(LeetCodeProblem p) {
        try {
            Map<String,Object> body = new HashMap<>();
            body.put("title", p.getTitle());
            body.put("tags", p.getTags() == null ? "" : p.getTags());
            body.put("difficulty", p.getDifficulty());
            body.put("solved", p.isSolved());
            body.put("url", p.getUrl() == null ? "" : p.getUrl());
            HttpResponse<String> r = ApiClient.post("/leetcode", body);
            if (r.statusCode() == 200) {
                p.setId(ApiClient.MAPPER.readTree(r.body()).get("id").asText());
                return true;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    public boolean updateProblem(LeetCodeProblem p) {
        try { return ApiClient.patch("/leetcode/" + p.getId() + "/solve").statusCode() == 200; }
        catch (Exception e) { e.printStackTrace(); return false; }
    }

    public boolean deleteProblem(String id) {
        try { return ApiClient.delete("/leetcode/" + id).statusCode() == 200; }
        catch (Exception e) { e.printStackTrace(); return false; }
    }

    private List<LeetCodeProblem> parseProblems(JsonNode arr) {
        List<LeetCodeProblem> list = new ArrayList<>();
        for (JsonNode n : arr) {
            LeetCodeProblem p = new LeetCodeProblem();
            p.setId(n.get("id").asText());
            p.setTitle(n.get("title").asText());
            p.setTags(n.has("tags") ? n.get("tags").asText() : "");
            p.setDifficulty(n.get("difficulty").asText());
            p.setSolved(n.get("solved").asBoolean());
            p.setUrl(n.has("url") ? n.get("url").asText() : "");
            list.add(p);
        }
        return list;
    }
}
