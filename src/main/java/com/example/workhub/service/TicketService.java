package com.example.workhub.service;

import org.springframework.stereotype.Service;

import com.example.workhub.mapper.TicketMapper;
import com.example.workhub.model.Ticket;

import java.util.List;

@Service
public class TicketService {

    private final TicketMapper ticketMapper;

    public TicketService(TicketMapper ticketMapper) {
        this.ticketMapper = ticketMapper;
    }

    public int add(Ticket ticket) {
        return ticketMapper.add(ticket);
    }

    public List<Ticket> findAll(){
        return ticketMapper.findAll();
    }

    public Ticket findById(int id){
        return ticketMapper.findById(id);
    }

    public int update(int id, Ticket ticket){
        return ticketMapper.update(id, ticket);
    }

    public int updateStatus(int id, String newStatus){
        Ticket ticket= ticketMapper.findById(id);
        if (ticket ==null){
            return 0;
        }
        
        String currentStatus=ticket.getStatus();
        
        if ((currentStatus.equals("OPEN")  && newStatus.equals("IN_PROGRESS"))
        ||  (currentStatus.equals("IN_PROGRESS")
        && newStatus.equals("RESOLVED"))
        || (currentStatus.equals("RESOLVED")
        && newStatus.equals("CLOSED"))){
            return ticketMapper.updateStatus(id, newStatus);
        }
        return -1;
        


    }

    public int delete(int id){
        return ticketMapper.delete(id);
    }
}