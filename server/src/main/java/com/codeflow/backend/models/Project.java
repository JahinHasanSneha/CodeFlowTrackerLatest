package com.codeflow.backend.models;

import java.time.LocalDate;
import java.util.UUID;

public class Project {

    private String    id;
    private String    title;

    private String    description;

    private LocalDate dueDate;
    private String    color;
    private int       progress; // 0–100

    public Project() {
        this.id       = UUID.randomUUID().toString();
        this.progress = 0;
    }

    public Project(String title, String description, LocalDate dueDate, String color) {
        this();
        this.title       = title;
        this.description = description;
        this.dueDate     = dueDate;
        this.color       = color;
    }

    public Project(String id, String title, String description,
                   LocalDate dueDate, String color, int progress) {
        this.id          = id;
        this.title       = title;
        this.description = description;
        this.dueDate     = dueDate;
        this.color       = color;
        this.progress    = progress;
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public String    getId()          { return id; }
    public String    getTitle()       { return title; }
    public String    getDescription() { return description; }
    public LocalDate getDueDate()     { return dueDate; }
    public String    getColor()       { return color; }
    public int       getProgress()    { return progress; }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setId(String id)               { this.id = id; }
    public void setTitle(String title)         { this.title = title; }
    public void setDescription(String desc)    { this.description = desc; }
    public void setDueDate(LocalDate dueDate)  { this.dueDate = dueDate; }
    public void setColor(String color)         { this.color = color; }
    public void setProgress(int progress)      { this.progress = Math.max(0, Math.min(100, progress)); }

    public boolean isDueThisWeek() {
        if (dueDate == null) return false;
        LocalDate weekFromNow = LocalDate.now().plusDays(7);
        return dueDate.isAfter(LocalDate.now()) && dueDate.isBefore(weekFromNow);
    }

    @Override
    public String toString() { return title + " (Due: " + dueDate + ")"; }
}
