package com.codeflow.backend.server;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * Simple HMAC-SHA256 JWT implementation (no external library needed).
 * Tokens are: base64(header).base64(payload).base64(signature)
 */
public class TokenUtil {

    private static final String SECRET = System.getenv().getOrDefault(
            "JWT_SECRET", "codeflow-secret-key-change-in-production-" + UUID.randomUUID());
    private static final long EXPIRY_MS = 7L * 24 * 60 * 60 * 1000; // 7 days

    public static String generate(String username) {
        long exp = System.currentTimeMillis() + EXPIRY_MS;
        String header  = b64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = b64("{\"sub\":\"" + username + "\",\"exp\":" + exp + "}");
        String sig     = sign(header + "." + payload);
        return header + "." + payload + "." + sig;
    }

    /** Returns username if valid, null if expired/invalid. */
    public static String validate(String token) {
        if (token == null) return null;
        String[] parts = token.split("\\.");
        if (parts.length != 3) return null;
        String expectedSig = sign(parts[0] + "." + parts[1]);
        if (!expectedSig.equals(parts[2])) return null;
        try {
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            // parse exp
            int expIdx = payload.indexOf("\"exp\":");
            if (expIdx < 0) return null;
            long exp = Long.parseLong(payload.substring(expIdx + 6, payload.indexOf("}", expIdx)));
            if (System.currentTimeMillis() > exp) return null;
            // parse sub
            int subIdx = payload.indexOf("\"sub\":\"");
            if (subIdx < 0) return null;
            int subStart = subIdx + 7;
            int subEnd = payload.indexOf("\"", subStart);
            return payload.substring(subStart, subEnd);
        } catch (Exception e) {
            return null;
        }
    }

    private static String b64(String data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }

    private static String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] sig = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(sig);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
