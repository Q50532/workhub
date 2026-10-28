package com.example.workhub.ratelimit;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class RateLimitWebConfig implements WebMvcConfigurer {
    private final RateLimitInterceptor limiter;
    public RateLimitWebConfig(RateLimitInterceptor limiter) { this.limiter = limiter; }
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(limiter).addPathPatterns("/tickets", "/tickets/*/comments", "/api/ai/**");
    }
}
