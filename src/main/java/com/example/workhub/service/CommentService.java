package com.example.workhub.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.example.workhub.exception.TicketNotFoundException;
import com.example.workhub.mapper.CommentMapper;
import com.example.workhub.mapper.TicketMapper;
import com.example.workhub.model.Comment;
import com.example.workhub.model.Ticket;




@Service
public class CommentService {

    private final CommentMapper commentMapper;
    private final TicketMapper ticketMapper;

    public CommentService(CommentMapper commentMapper, TicketMapper ticketMapper) {
        this.commentMapper = commentMapper;
        this.ticketMapper = ticketMapper;

    }

    public int add(Comment comment) {
        Ticket ticket= ticketMapper.findById(comment.getTicketId());
        if (ticket ==null){
            throw new TicketNotFoundException("Ticket not found");
        }
        return commentMapper.add(comment);
    }

    public List<Comment> findByTicketId(int ticketId) {
        Ticket ticket = ticketMapper.findById(ticketId);

        if (ticket == null) {
            throw new TicketNotFoundException("Ticket not found");
        }

        return commentMapper.findByTicketId(ticketId);
    }
   

    @Transactional
    public void markResolution(int ticketId, int commentId, int currentUserId) {

        Ticket ticket = ticketMapper.findById(ticketId);

        if (ticket == null) {
            throw new TicketNotFoundException("Ticket not found");
        }

        if (ticket.getCreatedBy() != currentUserId) {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only the ticket creator can change resolution"
        );
    }

        int count = commentMapper.countCommentInTicket(
            ticketId, commentId
        );

        if (count == 0) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Comment not found in this ticket"
            );
        }

        commentMapper.clearResolutionByTicketId(ticketId);

        int updated = commentMapper.setResolution(
            ticketId, commentId
        );

        if (updated != 1) {
            throw new IllegalStateException(
                "Failed to mark resolution"
            );
        }
    }

    @Transactional
    public void unmarkResolution(
            int ticketId,
            int commentId,
            int currentUserId) {

        // 1. Check whether the ticket exists
        Ticket ticket = ticketMapper.findById(ticketId);

        if (ticket == null) {
            throw new TicketNotFoundException("Ticket not found");
        }

        // 2. Only the ticket creator can unmark Resolution
        if (ticket.getCreatedBy() != currentUserId) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the ticket creator can change resolution"
            );
        }

        // 3. Check whether the comment belongs to this ticket
        int count = commentMapper.countCommentInTicket(
                ticketId, commentId
        );

        if (count == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Comment not found in this ticket"
            );
        }

        // 4. Remove the Resolution mark
        commentMapper.unmarkResolution(ticketId, commentId);
    }
}