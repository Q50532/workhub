package com.example.workhub.controller;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.ResponseEntity;

import com.example.workhub.model.Comment;
import com.example.workhub.service.CommentService;

import java.util.List;

@RestController
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService){
        this.commentService=commentService;
    }

    @PostMapping ("/tickets/{id}/comments")
    public ResponseEntity<String> add(
        @PathVariable int id,
        @RequestBody Comment comment) {
        comment.setTicketId(id);
        commentService.add(comment);
        
        return ResponseEntity.ok("comment added");
    }

    @GetMapping ("/tickets/{id}/comments")
    public List<Comment> findByTicketId(@PathVariable int id) {
        return commentService.findByTicketId(id);
    }
}
    

