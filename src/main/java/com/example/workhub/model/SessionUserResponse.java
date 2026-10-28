package com.example.workhub.model;

/** Safe user information for the current browser session. */
public record SessionUserResponse(int id, String username, String role) {
}
