package com.example.workhub.service;

import com.example.workhub.model.TicketLog;
import com.example.workhub.model.Ticket;
import com.example.workhub.mapper.TicketLogMapper;
import com.example.workhub.mapper.TicketMapper;
import org.springframework.stereotype.Service;

import com.example.workhub.exception.TicketNotFoundException;

import java.util.List;

@Service
public class TicketLogService {
    private final TicketLogMapper ticketLogMapper;
    private final TicketMapper ticketMapper;


    public TicketLogService(TicketLogMapper ticketLogMapper, TicketMapper ticketMapper){
        this.ticketLogMapper=ticketLogMapper;
        this.ticketMapper=ticketMapper;
    }

    public int add(TicketLog ticketLog){
        return ticketLogMapper.add(ticketLog);
    }

    public List<TicketLog> findByTicketId(int ticketId){
        Ticket ticket = ticketMapper.findById(ticketId);

        if (ticket == null) {
            throw new TicketNotFoundException("Ticket not found");
        }

        return ticketLogMapper.findByTicketId(ticketId);
    }
    
    
}
