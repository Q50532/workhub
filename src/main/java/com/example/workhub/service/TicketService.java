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

    public int delete(int id){
        return ticketMapper.delete(id);
    }
}