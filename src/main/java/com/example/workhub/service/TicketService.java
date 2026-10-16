package com.example.workhub.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.example.workhub.exception.InvalidPriorityException;
import com.example.workhub.exception.InvalidStatusTransitionException;
import com.example.workhub.exception.TicketNotFoundException;
import com.example.workhub.exception.UserNotFoundException;
import com.example.workhub.mapper.TicketLogMapper;
import com.example.workhub.mapper.TicketMapper;
import com.example.workhub.mapper.UserMapper;
import com.example.workhub.model.PageResponse;
import com.example.workhub.model.Ticket;
import com.example.workhub.model.TicketLog;
import com.example.workhub.model.User;

import java.util.List;
import java.time.Duration;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Service
public class TicketService {

    private final TicketMapper ticketMapper;
    private final TicketLogMapper ticketLogMapper;
    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final JsonMapper jsonMapper;    

    private void deleteTicketCache(int id){
        String key="ticket:"+id;
        stringRedisTemplate.delete(key);
    }

    public TicketService(TicketMapper ticketMapper, TicketLogMapper ticketLogMapper,UserMapper userMapper
        ,StringRedisTemplate stringRedisTemplate,JsonMapper jsonMapper) {
        this.ticketMapper = ticketMapper;
        this.ticketLogMapper = ticketLogMapper;
        this.userMapper=userMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.jsonMapper = jsonMapper;
    }

    public int add(Ticket ticket) {
        return ticketMapper.add(ticket);
    }

    public List<Ticket> findAll(){
        return ticketMapper.findAll();
    }

    public Ticket findById(int id) {

        String key = "ticket:" + id;

        String cachedTicket = stringRedisTemplate.opsForValue().get(key);

        if (cachedTicket != null) {
            System.out.println("CACHE HIT: " + key);

            try {
                return jsonMapper.readValue(cachedTicket, Ticket.class);
            } catch (JacksonException e) {
                throw new RuntimeException("Failed to read Ticket from Redis", e);
            }
        }

        System.out.println("CACHE MISS: " + key);

        Ticket ticket = ticketMapper.findById(id);

        if (ticket != null) {
            try {
                String json = jsonMapper.writeValueAsString(ticket);

                stringRedisTemplate.opsForValue().set(
                        key,
                        json,
                        Duration.ofSeconds(60)
                );
            } catch (JacksonException e) {
                throw new RuntimeException("Failed to write Ticket to Redis", e);
            }
        }

        return ticket;
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

    public List<Ticket> findByFilters(String status,String priority,Integer assignedTo
        ,int page, int size,String sortBy,String order
    ) {
        int offset=(page-1)*size;
        return ticketMapper.findByFilters(
                status,
                priority,
                assignedTo,
                size,
                offset,
                sortBy,
                order
        );
    }

    public int countByFilters(
            String status,
            String priority,
            Integer assignedTo) {

        return ticketMapper.countByFilters(
                status,
                priority,
                assignedTo
        );
    }

    public PageResponse findTickets(
            String status,
            String priority,
            Integer assignedTo,
            int page,
            int size,
            String sortBy,
            String order) {

        // 1. Get tickets for the current page
        List<Ticket> tickets = findByFilters(
                status,
                priority,
                assignedTo,
                page,
                size,
                sortBy,
                order
        );

        // 2. Count all matching tickets
        int total = countByFilters(
                status,
                priority,
                assignedTo
        );

        // 3. Build the pagination response
        return new PageResponse(tickets, total, page, size);
    }

    public int update(int id, Ticket ticket){
        Ticket existingTicket= ticketMapper.findById(id);
        if (existingTicket==null){
            throw new TicketNotFoundException("Ticket not found");
        }

        int result=ticketMapper.update(id, ticket);
        if (result==1){
            deleteTicketCache(id);
        }
        return result;
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
        || (currentStatus.equals("RESOLVED") && "CLOSED".equals(newStatus))
        || (currentStatus.equals("RESOLVED") && "IN_PROGRESS".equals(newStatus))) {

        int result = ticketMapper.updateStatus(id, newStatus);

        if (result == 1) {
            TicketLog ticketLog = new TicketLog();

            ticketLog.setTicketId(id);
            ticketLog.setOldStatus(currentStatus);
            ticketLog.setNewStatus(newStatus);

            ticketLogMapper.add(ticketLog);
            deleteTicketCache(id);
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
        int result = ticketMapper.updateAssignee(id, assignedTo);

        if (result == 1) {
            deleteTicketCache(id);
        }

        return result;
    }

    public int updatePriority(int id, String priority){
        Ticket ticket= ticketMapper.findById(id);
        if(ticket==null){
            throw new TicketNotFoundException("Ticket not found");
            
        }
        if ("LOW".equals(priority)|| "MEDIUM".equals(priority)
        || "HIGH".equals(priority)|| "URGENT".equals(priority)) {
        int result = ticketMapper.updatePriority(id, priority);

        if (result == 1) {
            deleteTicketCache(id);
        }
        return result;
        }
        
        throw new InvalidPriorityException("Invalid priority");
    
    }

    public int delete(int id){
        Ticket ticket = ticketMapper.findById(id);

        if (ticket == null) {
            throw new TicketNotFoundException("Ticket not found");
        }
        int result = ticketMapper.delete(id);

        if (result == 1) {
            deleteTicketCache(id);
        }

        return result;
    }

    
}