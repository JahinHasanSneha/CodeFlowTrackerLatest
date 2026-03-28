package com.codeflow.api.models;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a registered user of CodeFlow Tracker.
 */
public class User {

    private String id;

    private String username;

    private String email;

    private String passwordHash;   // BCrypt hash – never store plain text

    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;

    public User() {
        this.id        = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
    }

    public User(String username, String email, String passwordHash) {
        this();
        this.username     = username;
        this.email        = email;
        this.passwordHash = passwordHash;
    }

    public User(String id, String username, String email,
                String passwordHash, LocalDateTime createdAt, LocalDateTime lastLogin) {
        this.id           = id;
        this.username     = username;
        this.email        = email;
        this.passwordHash = passwordHash;
        this.createdAt    = createdAt;
        this.lastLogin    = lastLogin;
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public String        getId()           { return id; }
    public String        getUsername()     { return username; }
    public String        getEmail()        { return email; }
    public String        getPasswordHash() { return passwordHash; }
    public LocalDateTime getCreatedAt()    { return createdAt; }
    public LocalDateTime getLastLogin()    { return lastLogin; }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setId(String id)                       { this.id = id; }
    public void setUsername(String username)           { this.username = username; }
    public void setEmail(String email)                 { this.email = email; }
    public void setPasswordHash(String passwordHash)   { this.passwordHash = passwordHash; }
    public void setCreatedAt(LocalDateTime createdAt)  { this.createdAt = createdAt; }
    public void setLastLogin(LocalDateTime lastLogin)  { this.lastLogin = lastLogin; }

    @Override
    public String toString() {
        return "User{id='" + id + "', username='" + username + "', email='" + email + "'}";
    }
}
