package com.example.workhub.controller;

import com.example.workhub.service.TicketLogService;
import com.example.workhub.service.TicketAccessService;
import java.security.Principal;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.example.workhub.model.TicketLog;

@RestController 
public class TicketLogController {
    private final TicketLogService ticketLogService;
    private final TicketAccessService access;

    public TicketLogController(TicketLogService ticketLogService, TicketAccessService access){
        this.ticketLogService=ticketLogService;
        this.access=access;
    }
    
    @GetMapping("/tickets/{id}/logs")
    public List<TicketLog> findByTicketId(@PathVariable int id, Principal principal){
        access.visibleTicket(id, principal);
        return ticketLogService.findByTicketId(id);
    }
}
