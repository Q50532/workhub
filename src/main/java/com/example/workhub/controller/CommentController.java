
package com.example.workhub.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.example.workhub.model.Comment;
import com.example.workhub.model.User;
import com.example.workhub.service.CommentService;
import com.example.workhub.mapper.UserMapper;

import java.security.Principal;
import java.util.List;

@RestController
public class CommentController {

    private final CommentService commentService;
    private final UserMapper userMapper;

    public CommentController(
            CommentService commentService,
            UserMapper userMapper) {

        this.commentService = commentService;
        this.userMapper = userMapper;
    }

    @PostMapping("/tickets/{id}/comments")
    public ResponseEntity<String> add(
            @PathVariable int id,
            @RequestBody Comment comment,
            Principal principal) {

        User currentUser = userMapper.findByUsername(
                principal.getName()
        );

        if (currentUser == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Current user not found"
            );
        }

        comment.setTicketId(id);
        comment.setAuthorId(currentUser.getId());

        commentService.add(comment);

        return ResponseEntity.ok("comment added");
    }

    @GetMapping("/tickets/{id}/comments")
    public List<Comment> findByTicketId(@PathVariable int id) {
        return commentService.findByTicketId(id);
    }
}
