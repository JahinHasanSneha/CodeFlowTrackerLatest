package com.codeflow.api.services;

import com.codeflow.shared.ApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.*;

/**
 * Derives all progress data automatically from the LeetCode tracker.
 * Counts solved problems grouped by their solvedAt date.
 * No manual logging needed.
 */
public class ProgressService {

    /** Fetch all solved problems from /leetcode and group by solvedAt date. */
    private Map<LocalDate, Integer> buildFromLeetCode() {
        Map<LocalDate, Integer> map = new LinkedHashMap<>();
        try {
            HttpResponse<String> r = ApiClient.get("/leetcode");
            if (r.statusCode() == 200) {
                for (JsonNode n : ApiClient.MAPPER.readTree(r.body())) {
                    boolean solved = n.path("solved").asBoolean(false);
                    String solvedAt = n.path("solvedAt").asText("");
                    if (solved && !solvedAt.isEmpty()) {
                        try {
                            // solvedAt is stored as LocalDateTime string e.g. "2024-02-23T14:30:00"
                            LocalDate date = LocalDate.parse(solvedAt.substring(0, 10));
                            map.merge(date, 1, Integer::sum);
                        } catch (Exception ignore) {}
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return map;
    }

    /** Returns daily solved counts for the last `days` days. */
    public Map<LocalDate, Integer> getDailyProgress(int days) {
        Map<LocalDate, Integer> all = buildFromLeetCode();
        Map<LocalDate, Integer> filtered = new LinkedHashMap<>();
        LocalDate since = LocalDate.now().minusDays(days);
        for (Map.Entry<LocalDate, Integer> entry : all.entrySet()) {
            if (!entry.getKey().isBefore(since)) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return filtered;
    }

    /** Total number of solved problems ever. */
    public int getTotalSolved() {
        return buildFromLeetCode().values().stream().mapToInt(Integer::intValue).sum();
    }

    /** Current consecutive daily streak. */
    public int getCurrentStreak() {
        Map<LocalDate, Integer> progress = buildFromLeetCode();
        int streak = 0;
        LocalDate d = LocalDate.now();
        while (progress.getOrDefault(d, 0) > 0) {
            streak++;
            d = d.minusDays(1);
        }
        return streak;
    }

    /** Total solved in the last 7 days. */
    public int getWeekProgress() {
        Map<LocalDate, Integer> all = buildFromLeetCode();
        LocalDate weekAgo = LocalDate.now().minusDays(7);
        return all.entrySet().stream()
                .filter(e -> !e.getKey().isBefore(weekAgo))
                .mapToInt(Map.Entry::getValue)
                .sum();
    }

    /** Average problems solved per active day (last 30 days). */
    public double getAveragePerDay() {
        Map<LocalDate, Integer> m = getDailyProgress(30);
        if (m.isEmpty()) return 0;
        return m.values().stream().mapToInt(Integer::intValue).average().orElse(0);
    }
}