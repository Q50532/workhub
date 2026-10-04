package com.example.workhub.controller;

import com.example.workhub.service.TicketLogService;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.example.workhub.model.TicketLog;

@RestController 
public class TicketLogController {
    private final TicketLogService ticketLogService;

    public TicketLogController(TicketLogService ticketLogService){
        this.ticketLogService=ticketLogService;
    }
    
    @GetMapping("/tickets/{id}/logs")
    public List<TicketLog> findByTicketId(@PathVariable int id){
        return ticketLogService.findByTicketId(id);
    }
}
