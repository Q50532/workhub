package com.example.workhub.ratelimit;

/** Explicit endpoint policy; other paths are not rate limited. */
public record RateLimitRule(String bucket, int limit, int windowSeconds) {
    public static RateLimitRule forRequest(String method, String path) {
        if (!"POST".equalsIgnoreCase(method)) return null;
        if ("/tickets".equals(path)) return new RateLimitRule("ticket-create", 10, 60);
        if (path.matches("/tickets/[0-9]+/comments")) return new RateLimitRule("comment-create", 30, 60);
        if (path.startsWith("/api/ai/")) return new RateLimitRule("ai", 15, 60);
        return null;
    }
}
