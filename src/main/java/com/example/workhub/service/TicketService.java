package com.example.workhub.service;

import org.springframework.stereotype.Service;

import com.example.workhub.mapper.TicketLogMapper;
import com.example.workhub.mapper.TicketMapper;
import com.example.workhub.model.Ticket;
import com.example.workhub.model.TicketLog;

import java.util.List;

@Service
public class TicketService {

    private final TicketMapper ticketMapper;
    private final TicketLogMapper ticketLogMapper;

    public TicketService(TicketMapper ticketMapper, TicketLogMapper ticketLogMapper) {
        this.ticketMapper = ticketMapper;
        this.ticketLogMapper = ticketLogMapper;
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
            int result= ticketMapper.updateStatus(id, newStatus);
            if (result==1){
                TicketLog ticketLog=new TicketLog();

                ticketLog.setTicketId(id);
                ticketLog.setOldStatus(currentStatus);
                ticketLog.setNewStatus(newStatus);

                ticketLogMapper.add(ticketLog);

            }
            return result;
        }
        return -1;
        


    }

    public int delete(int id){
        return ticketMapper.delete(id);
    }
}