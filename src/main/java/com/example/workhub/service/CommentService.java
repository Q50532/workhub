package com.example.workhub.service;

import java.util.List;

import org.springframework.stereotype.Service;

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
   



}