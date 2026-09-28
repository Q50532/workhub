package com.example.workhub.service;

import org.springframework.stereotype.Service;

import com.example.workhub.mapper.TicketMapper;
import com.example.workhub.model.Ticket;

@Service
public class TicketService {

    private final TicketMapper ticketMapper;

    public TicketService(TicketMapper ticketMapper) {
        this.ticketMapper = ticketMapper;
    }

    public int add(Ticket ticket) {
        return ticketMapper.add(ticket);
    }
}