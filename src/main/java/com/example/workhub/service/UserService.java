package com.example.workhub.service;
import com.example.workhub.model.User;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.example.workhub.mapper.UserMapper;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder) {

        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    

    public int updatePassword(
            int id,
            String currentPassword,
            String rawPassword) {

        // 1. Find the user
        User user = userMapper.findById(id);

        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "User not found"
            );
        }

        // 2. Verify current password
        if (currentPassword == null ||
            !passwordEncoder.matches(currentPassword, user.getPassword())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Current password is incorrect"
            );
        }

        // 3. Validate new password
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "New password cannot be blank"
            );
        }

        // 4. Encode and save new password
        String encodedPassword = passwordEncoder.encode(rawPassword);

        return userMapper.updatePassword(id, encodedPassword);
    }

}