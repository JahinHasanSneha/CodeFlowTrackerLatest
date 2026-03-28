package com.codeflow.backend.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * File-based JSON data store.
 * Each "table" is a JSON array stored in a file under DATA_DIR.
 * Thread-safe with per-table read-write locks.
 */
public class DataStore {

    public static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    // On Railway: use /data (persistent volume) if available, else /tmp
    private static final String DATA_DIR;
    static {
        String railwayDir = System.getenv("DATA_DIR");
        if (railwayDir != null && !railwayDir.isEmpty()) {
            DATA_DIR = railwayDir;
        } else {
            // Try /data first (Railway persistent volume), else local
            String candidate = "/data/codeflow";
            File dir = new File(candidate);
            if (dir.exists() || dir.mkdirs()) {
                DATA_DIR = candidate;
            } else {
                DATA_DIR = System.getProperty("user.home") + "/.codeflow/data";
            }
        }
    }

    private static final ConcurrentHashMap<String, ReentrantReadWriteLock> LOCKS = new ConcurrentHashMap<>();

    public static void init() {
        new File(DATA_DIR).mkdirs();
        System.out.println("DataStore initialized at: " + DATA_DIR);
    }

    private static ReentrantReadWriteLock lockFor(String table) {
        return LOCKS.computeIfAbsent(table, k -> new ReentrantReadWriteLock());
    }

    public static ArrayNode readAll(String table) {
        ReentrantReadWriteLock.ReadLock lock = lockFor(table).readLock();
        lock.lock();
        try {
            File file = tableFile(table);
            if (!file.exists()) return MAPPER.createArrayNode();
            return (ArrayNode) MAPPER.readTree(file);
        } catch (Exception e) {
            return MAPPER.createArrayNode();
        } finally {
            lock.unlock();
        }
    }

    public static void writeAll(String table, ArrayNode data) {
        ReentrantReadWriteLock.WriteLock lock = lockFor(table).writeLock();
        lock.lock();
        try {
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(tableFile(table), data);
        } catch (Exception e) {
            System.err.println("Failed to write table " + table + ": " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    /** Find first element where field==value */
    public static JsonNode findOne(String table, String field, String value) {
        for (JsonNode n : readAll(table)) {
            if (value.equals(n.path(field).asText())) return n;
        }
        return null;
    }

    /** Find all elements for a user */
    public static ArrayNode findByUser(String table, String username) {
        ArrayNode result = MAPPER.createArrayNode();
        for (JsonNode n : readAll(table)) {
            if (username.equals(n.path("username").asText())) result.add(n);
        }
        return result;
    }

    /** Insert a new record */
    public static void insert(String table, ObjectNode record) {
        ReentrantReadWriteLock.WriteLock lock = lockFor(table).writeLock();
        lock.lock();
        try {
            File file = tableFile(table);
            ArrayNode arr = file.exists() ? (ArrayNode) MAPPER.readTree(file) : MAPPER.createArrayNode();
            arr.add(record);
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(file, arr);
        } catch (Exception e) {
            System.err.println("Insert failed for " + table + ": " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    /** Update record by id field */
    public static boolean updateById(String table, String id, ObjectNode updated) {
        ReentrantReadWriteLock.WriteLock lock = lockFor(table).writeLock();
        lock.lock();
        try {
            File file = tableFile(table);
            if (!file.exists()) return false;
            ArrayNode arr = (ArrayNode) MAPPER.readTree(file);
            ArrayNode newArr = MAPPER.createArrayNode();
            boolean found = false;
            for (JsonNode n : arr) {
                if (id.equals(n.path("id").asText())) {
                    newArr.add(updated);
                    found = true;
                } else {
                    newArr.add(n);
                }
            }
            if (found) MAPPER.writerWithDefaultPrettyPrinter().writeValue(file, newArr);
            return found;
        } catch (Exception e) {
            return false;
        } finally {
            lock.unlock();
        }
    }

    /** Delete record by id */
    public static boolean deleteById(String table, String id) {
        ReentrantReadWriteLock.WriteLock lock = lockFor(table).writeLock();
        lock.lock();
        try {
            File file = tableFile(table);
            if (!file.exists()) return false;
            ArrayNode arr = (ArrayNode) MAPPER.readTree(file);
            ArrayNode newArr = MAPPER.createArrayNode();
            boolean found = false;
            for (JsonNode n : arr) {
                if (id.equals(n.path("id").asText())) found = true;
                else newArr.add(n);
            }
            if (found) MAPPER.writerWithDefaultPrettyPrinter().writeValue(file, newArr);
            return found;
        } catch (Exception e) {
            return false;
        } finally {
            lock.unlock();
        }
    }

    private static File tableFile(String table) {
        return new File(DATA_DIR + "/" + table + ".json");
    }
}
