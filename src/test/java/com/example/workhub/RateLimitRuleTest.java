package com.example.workhub;

import com.example.workhub.ratelimit.RateLimitRule;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RateLimitRuleTest {
    @Test void limiterCoversOnlyTheIntendedWriteRoutes() {
        assertEquals(10, RateLimitRule.forRequest("POST", "/tickets").limit());
        assertEquals(30, RateLimitRule.forRequest("POST", "/tickets/16/comments").limit());
        assertEquals(15, RateLimitRule.forRequest("POST", "/api/ai/ask").limit());
        assertNull(RateLimitRule.forRequest("GET", "/tickets"));
        assertNull(RateLimitRule.forRequest("PUT", "/tickets/16/status"));
        assertNull(RateLimitRule.forRequest("POST", "/tickets/not-an-id/comments"));
    }
}
