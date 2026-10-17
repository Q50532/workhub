package com.example.workhub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
            // Existing project setting, retained for compatibility with earlier API tests.
            // Enable CSRF protection before any non-local deployment.
            
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login.html", "/assets/**", "/favicon.svg","/csrf").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(basic -> {})
            .formLogin(form -> form
                .loginPage("/login.html")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login.html?logout")
                .permitAll()
            );

        return http.build();
    }
}
