
package com.example.workhub.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.example.workhub.model.Comment;
import com.example.workhub.model.User;
import com.example.workhub.service.CommentService;
import com.example.workhub.service.TicketAccessService;
import com.example.workhub.mapper.UserMapper;

import java.security.Principal;
import java.util.List;

@RestController
public class CommentController {

    private final CommentService commentService;
    private final UserMapper userMapper;
    private final TicketAccessService access;

    public CommentController(
            CommentService commentService,
            UserMapper userMapper, TicketAccessService access) {

        this.commentService = commentService;
        this.userMapper = userMapper;
        this.access = access;
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

        access.visibleTicket(id, principal);
        comment.setTicketId(id);
        comment.setAuthorId(currentUser.getId());

        commentService.add(comment);

        return ResponseEntity.ok("comment added");
    }

    @GetMapping("/tickets/{id}/comments")
    public List<Comment> findByTicketId(@PathVariable int id, Principal principal) {
        access.visibleTicket(id, principal);
        return commentService.findByTicketId(id);
    }

    @PutMapping("/tickets/{id}/comments/{commentId}/resolution")
    public ResponseEntity<String> markResolution(
            @PathVariable("id") int id,
            @PathVariable("commentId") int commentId,
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

        access.visibleTicket(id, principal);
        commentService.markResolution(
                id,
                commentId,
                currentUser.getId()
        );

        return ResponseEntity.ok("Resolution marked");
    }
    @DeleteMapping("/tickets/{id}/comments/{commentId}/resolution")
    public ResponseEntity<String> unmarkResolution(
            @PathVariable("id") int id,
            @PathVariable("commentId") int commentId,
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

        access.visibleTicket(id, principal);
        commentService.unmarkResolution(
                id,
                commentId,
                currentUser.getId()
        );

        return ResponseEntity.ok("Resolution unmarked");
    }
}
