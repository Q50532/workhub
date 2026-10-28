package com.example.workhub.controller;

import com.example.workhub.mapper.UserMapper;
import com.example.workhub.model.SessionUserResponse;
import com.example.workhub.model.User;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class SessionController {
    private final UserMapper userMapper;

    public SessionController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @GetMapping("/api/me")
    public SessionUserResponse me(Principal principal) {
        User user = userMapper.findByUsername(principal.getName());
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
        }
        // Never return User directly: it contains a password hash.
        return new SessionUserResponse(user.getId(), user.getUsername(), user.getRole());
    }
}
