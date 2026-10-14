package com.example.workhub.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

    

    public int updatePassword(int id, String rawPassword) {

        String encodedPassword =
                passwordEncoder.encode(rawPassword);

        return userMapper.updatePassword(id, encodedPassword);
    }

}