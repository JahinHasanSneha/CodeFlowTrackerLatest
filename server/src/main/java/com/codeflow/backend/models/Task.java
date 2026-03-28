package com.codeflow.backend.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class Task {

    private String        id;
    private String        title;

    private String        description;

    private Priority      priority;

    private LocalDate     dueDate;
    private boolean       completed;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String        category;

    public Task() {
        this.id        = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
    }

    public Task(String title, String description, Priority priority,
                LocalDate dueDate, String category) {
        this();
        this.title       = title;
        this.description = description;
        this.priority    = priority;
        this.dueDate     = dueDate;
        this.completed   = false;
        this.category    = category;
    }

    public Task(String id, String title, String description, Priority priority,
                LocalDate dueDate, boolean completed,
                LocalDateTime createdAt, LocalDateTime completedAt, String category) {
        this.id          = id;
        this.title       = title;
        this.description = description;
        this.priority    = priority;
        this.dueDate     = dueDate;
        this.completed   = completed;
        this.createdAt   = createdAt;
        this.completedAt = completedAt;
        this.category    = category;
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public String        getId()          { return id; }
    public String        getTitle()       { return title; }
    public String        getDescription() { return description; }
    public Priority      getPriority()    { return priority; }
    public LocalDate     getDueDate()     { return dueDate; }
    public boolean       isCompleted()    { return completed; }
    public LocalDateTime getCreatedAt()   { return createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public String        getCategory()    { return category; }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setId(String id)               { this.id = id; }
    public void setTitle(String title)         { this.title = title; }
    public void setDescription(String desc)    { this.description = desc; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public void setDueDate(LocalDate dueDate)  { this.dueDate = dueDate; }
    public void setCategory(String category)   { this.category = category; }

    public void setCompleted(boolean completed) {
        this.completed   = completed;
        this.completedAt = completed ? LocalDateTime.now() : null;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    public boolean isDueToday() {
        return dueDate != null && dueDate.equals(LocalDate.now());
    }

    public boolean isOverdue() {
        return dueDate != null && dueDate.isBefore(LocalDate.now()) && !completed;
    }

    @Override
    public String toString() { return title + " (" + priority + ")"; }
}
