package com.example.workhub.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Principal;
import java.util.HexFormat;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Per-authenticated-principal fixed window limiter; all app replicas share Redis state. */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    // INCR + EXPIRE inside a single atomic Redis script (unlike separate commands).
    private static final String LUA = """
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1])) end
            return count
            """;
    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> script = buildScript();
    private static DefaultRedisScript<Long> buildScript() {
        DefaultRedisScript<Long> s = new DefaultRedisScript<>();
        s.setScriptText(LUA);
        s.setResultType(Long.class);
        return s;
    }

    public RateLimitInterceptor(StringRedisTemplate redis) { this.redis = redis; }

    static String sha256(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) { throw new IllegalStateException("SHA-256 unavailable", e); }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        RateLimitRule rule = RateLimitRule.forRequest(request.getMethod(), request.getRequestURI());
        if (rule == null) return true;
        Principal principal = request.getUserPrincipal();
        if (principal == null) return true; // SecurityFilterChain enforces authentication.
        String key = "workhub:rate:" + rule.bucket() + ":" + sha256(principal.getName());
        Long count;
        try { count = redis.execute(script, java.util.List.of(key), String.valueOf(rule.windowSeconds())); }
        catch (RuntimeException redisFailure) {
            // Protected writes fail closed: silent bypass would defeat rate limiting.
            response.sendError(HttpStatus.SERVICE_UNAVAILABLE.value(), "Rate limiter unavailable");
            return false;
        }
        if (count == null) {
            response.sendError(HttpStatus.SERVICE_UNAVAILABLE.value(), "Rate limiter unavailable");
            return false;
        }
        if (count > rule.limit()) {
            response.setHeader("Retry-After", String.valueOf(rule.windowSeconds()));
            response.sendError(429, "Rate limit exceeded");
            return false;
        }
        return true;
    }
}
