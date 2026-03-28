package com.codeflow.backend.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

/**
 * Base class for all protected handlers.
 * Provides authentication and routing helpers.
 */
public abstract class BaseHandler implements HttpHandler {

    @Override
    public final void handle(HttpExchange ex) throws IOException {
        if ("OPTIONS".equals(ex.getRequestMethod())) {
            CodeFlowServer.handleOptions(ex);
            return;
        }
        try {
            String token    = CodeFlowServer.extractToken(ex);
            String username = TokenUtil.validate(token);
            if (username == null) {
                CodeFlowServer.sendJson(ex, 401, "{\"error\":\"Unauthorized\"}");
                return;
            }
            handleRequest(ex, username);
        } catch (Exception e) {
            e.printStackTrace();
            CodeFlowServer.sendJson(ex, 500, "{\"error\":\"Server error: " + e.getMessage() + "\"}");
        }
    }

    protected abstract void handleRequest(HttpExchange ex, String username) throws IOException;

    protected String path(HttpExchange ex) {
        return ex.getRequestURI().getPath();
    }

    protected String method(HttpExchange ex) {
        return ex.getRequestMethod();
    }

    protected String body(HttpExchange ex) throws IOException {
        return CodeFlowServer.readBody(ex);
    }

    /** Extract the last path segment, e.g. /api/tasks/abc123 → abc123 */
    protected String lastSegment(HttpExchange ex) {
        String p = ex.getRequestURI().getPath();
        String[] parts = p.split("/");
        return parts[parts.length - 1];
    }

    /** Extract path segment at position from the end (0=last, 1=second-to-last) */
    protected String segmentFromEnd(HttpExchange ex, int fromEnd) {
        String p = ex.getRequestURI().getPath();
        String[] parts = p.split("/");
        int idx = parts.length - 1 - fromEnd;
        return idx >= 0 ? parts[idx] : "";
    }

    protected String queryParam(HttpExchange ex, String name) {
        String query = ex.getRequestURI().getQuery();
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && kv[0].equals(name)) return kv[1];
        }
        return null;
    }
}
