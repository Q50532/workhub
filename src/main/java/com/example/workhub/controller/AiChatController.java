package com.example.workhub.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

/**
 * Authenticated, CSRF-protected browser -> Java -> local Python Agent gateway.
 * WorkHub remains the authority for Ticket ACL; the Agent's read-only tools
 * call back into WorkHub with THIS user's existing authenticated session.
 */
@RestController
@RequestMapping("/api/ai")
public class AiChatController {
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private static final Set<Integer> SAFE_ERROR_CODES = Set.of(400, 401, 403, 404, 429, 503, 504);
    private final JsonMapper json;
    private final URI agentUri;
    private final String sharedKey;

    public AiChatController(JsonMapper json,
            @Value("${WORKHUB_AGENT_URL:http://127.0.0.1:8765/chat}") String agentUrl,
            @Value("${WORKHUB_AGENT_SHARED_KEY:}") String sharedKey) {
        this.json = json;
        this.agentUri = URI.create(agentUrl);
        if (!"http".equals(agentUri.getScheme()) ||
                !("127.0.0.1".equals(agentUri.getHost()) || "localhost".equals(agentUri.getHost())) ||
                agentUri.getPort() < 1 || agentUri.getPort() > 65535 ||
                !"/chat".equals(agentUri.getPath()) ||
                agentUri.getUserInfo() != null || agentUri.getQuery() != null ||
                agentUri.getFragment() != null) {
            throw new IllegalArgumentException("AI Agent URL must be a loopback http://host:port/chat");
        }
        this.sharedKey = sharedKey;
    }

    public record ChatRequest(String message) { }

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody ChatRequest input,
                                  Principal principal,
                                  HttpServletRequest servletRequest) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Login required"));
        }
        if (input == null || input.message() == null || input.message().isBlank()
                || input.message().length() > 2000) {
            return ResponseEntity.badRequest().body(Map.of("error", "Question must contain 1..2000 characters"));
        }
        // Fail closed; do not silently bypass service authentication.
        if (sharedKey.length() < 32) {
            return ResponseEntity.status(503).body(Map.of("error", "AI Agent is not configured"));
        }
        String basic = servletRequest.getHeader("Authorization");
        String session = null;
        if (servletRequest.getCookies() != null) {
            for (Cookie cookie : servletRequest.getCookies()) {
                if ("JSESSIONID".equals(cookie.getName())) {
                    session = cookie.getValue();
                    break;
                }
            }
        }
        HttpRequest.Builder builder = HttpRequest.newBuilder(agentUri)
                .timeout(Duration.ofSeconds(55))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json")
                .header("X-Agent-Internal-Key", sharedKey);
        if (basic != null && basic.matches("Basic [A-Za-z0-9+/=]{4,2048}")) {
            builder.header("X-Workhub-Authorization", basic);
        } else if (session != null && session.matches("[A-Za-z0-9._-]{8,256}")) {
            builder.header("X-Workhub-Session", session);
        } else {
            return ResponseEntity.status(401).body(Map.of("error", "Authenticated session required"));
        }
        try {
            String payload = json.writeValueAsString(
                    Map.of("message", input.message().strip(), "username", principal.getName()));
            HttpResponse<String> response = HTTP.send(
                    builder.POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8)).build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();
            if (status == 200) {
                // Return an allowlisted structure, not arbitrary fields from Python.
                @SuppressWarnings("unchecked")
                Map<String, Object> body = json.readValue(response.body(), Map.class);
                Object answer = body.get("answer");
                if (!(answer instanceof String text) || text.isBlank()) {
                    return ResponseEntity.status(502).body(Map.of("error", "AI response was invalid"));
                }
                return ResponseEntity.ok(Map.of("answer", text,
                        "tools", body.get("tools") instanceof java.util.List<?> list ? list : java.util.List.of(),
                        "citations", body.get("citations") instanceof java.util.List<?> list ? list : java.util.List.of()));
            }
            if (SAFE_ERROR_CODES.contains(status)) {
                return ResponseEntity.status(status).body(Map.of("error", switch (status) {
                    case 400 -> "Invalid AI request";
                    case 401 -> "WorkHub session expired; please sign in again";
                    case 403 -> "Ticket access denied";
                    case 404 -> "Ticket not found";
                    case 429 -> "AI request limit reached; please retry later";
                    case 504 -> "AI request timed out";
                    default -> "AI Agent temporarily unavailable";
                }));
            }
            return ResponseEntity.status(502).body(Map.of("error", "AI Agent request failed"));
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(503).body(Map.of("error", "AI Agent request interrupted"));
        } catch (java.net.http.HttpTimeoutException error) {
            return ResponseEntity.status(504).body(Map.of("error", "AI Agent request timed out"));
        } catch (Exception error) {
            // Never return raw exceptions / forwarded credentials to a browser.
            return ResponseEntity.status(503).body(Map.of("error", "AI Agent temporarily unavailable"));
        }
    }
}
