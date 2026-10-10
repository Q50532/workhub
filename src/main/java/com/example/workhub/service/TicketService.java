package com.example.workhub.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.workhub.exception.InvalidPriorityException;
import com.example.workhub.exception.InvalidStatusTransitionException;
import com.example.workhub.exception.TicketNotFoundException;
import com.example.workhub.exception.UserNotFoundException;
import com.example.workhub.mapper.TicketLogMapper;
import com.example.workhub.mapper.TicketMapper;
import com.example.workhub.mapper.UserMapper;
import com.example.workhub.model.Ticket;
import com.example.workhub.model.TicketLog;
import com.example.workhub.model.User;

import java.util.List;

@Service
public class TicketService {

    private final TicketMapper ticketMapper;
    private final TicketLogMapper ticketLogMapper;
    private final UserMapper userMapper;

    public TicketService(TicketMapper ticketMapper, TicketLogMapper ticketLogMapper,UserMapper userMapper) {
        this.ticketMapper = ticketMapper;
        this.ticketLogMapper = ticketLogMapper;
        this.userMapper=userMapper;
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

    public List<Ticket> findByStatus(String status) {
        return ticketMapper.findByStatus(status);
    }

    public List<Ticket> findByPriority(String priority) {
        return ticketMapper.findByPriority(priority);
    }

    public List<Ticket> findByAssignedTo(Integer assignedTo) {
        return ticketMapper.findByAssignedTo(assignedTo);
    }

    public int update(int id, Ticket ticket){
        Ticket existTicket= ticketMapper.findById(id);
        if (existTicket==null){
            throw new TicketNotFoundException("Ticket not found");
        }
        return ticketMapper.update(id, ticket);
    }


    @Transactional 
    public int updateStatus(int id, String newStatus) {
    Ticket ticket = ticketMapper.findById(id);

    if (ticket == null) {
        throw new TicketNotFoundException("Ticket not found");
    }

    String currentStatus = ticket.getStatus();

    if ((currentStatus.equals("OPEN")&& "IN_PROGRESS".equals(newStatus))
        || (currentStatus.equals("IN_PROGRESS") && "RESOLVED".equals(newStatus))
        || (currentStatus.equals("RESOLVED") && "CLOSED".equals(newStatus))) {

        int result = ticketMapper.updateStatus(id, newStatus);

        if (result == 1) {
            TicketLog ticketLog = new TicketLog();

            ticketLog.setTicketId(id);
            ticketLog.setOldStatus(currentStatus);
            ticketLog.setNewStatus(newStatus);

            ticketLogMapper.add(ticketLog);
        }

        return result;
    }

    throw new InvalidStatusTransitionException("Invalid status transition");
}

    public int updateAssignee(int id, Integer  assignedTo   ){
        Ticket ticket= ticketMapper.findById(id);
        if (ticket ==null){
            throw new TicketNotFoundException("Ticket not found");
        }
        if (assignedTo != null) {//really assigend to someone.There exists a case that the ticket are not assigned.
            User user = userMapper.findById(assignedTo);

            if (user == null) {
                throw new UserNotFoundException("User not found");//when it is assigend to someone, we need to check the validity.
            }
        }
        return ticketMapper.updateAssignee(id, assignedTo);
    }

    public int updatePriority(int id, String priority){
        Ticket ticket= ticketMapper.findById(id);
        if(ticket==null){
            throw new TicketNotFoundException("Ticket not found");
            
        }
        if ("LOW".equals(priority)|| "MEDIUM".equals(priority)
        || "HIGH".equals(priority)|| "URGENT".equals(priority)) {
        return ticketMapper.updatePriority(id, priority);}
        
        throw new InvalidPriorityException("Invalid priority");
    
    }

    public int delete(int id){
        Ticket ticket = ticketMapper.findById(id);

        if (ticket == null) {
            throw new TicketNotFoundException("Ticket not found");
        }
        return ticketMapper.delete(id);
    }
}