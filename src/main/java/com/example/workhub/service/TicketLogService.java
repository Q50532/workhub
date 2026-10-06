package com.example.workhub.service;

import com.example.workhub.model.TicketLog;
import com.example.workhub.mapper.TicketLogMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TicketLogService {
    private final TicketLogMapper ticketLogMapper;

    public TicketLogService(TicketLogMapper ticketLogMapper){
        this.ticketLogMapper=ticketLogMapper;
    }

    public int add(TicketLog ticketLog){
        return ticketLogMapper.add(ticketLog);
    }

    public List<TicketLog> findByTicketId(int ticketId){
        return ticketLogMapper.findByTicketId(ticketId);
    }
    
    
}
