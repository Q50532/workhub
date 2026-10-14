package com.example.workhub.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

import com.example.workhub.service.UserService;
import com.example.workhub.mapper.UserMapper;
import com.example.workhub.model.User;

@RestController
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @PutMapping("/users/{id}/password")
    public int updatePassword(
            @PathVariable int id,
            @RequestBody String rawPassword,
            Principal principal) {

        User targetUser = userMapper.findById(id);

        if (targetUser == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "User not found"
            );
        }

        if (!targetUser.getUsername().equals(principal.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot change another user's password"
            );
        }

        return userService.updatePassword(id, rawPassword);
    }
}