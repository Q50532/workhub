package com.example.workhub.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.workhub.model.Ticket;
import com.example.workhub.service.TicketService;

@RestController
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping("/tickets")
    public int add(@RequestBody Ticket ticket) {
        return ticketService.add(ticket);
    }
}