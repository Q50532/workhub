package com.example.workhub.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.example.workhub.model.PageResponse;
import com.example.workhub.model.Ticket;
import com.example.workhub.service.TicketService;
import com.example.workhub.model.TicketUpdateRequest;
import com.example.workhub.mapper.UserMapper;
import com.example.workhub.model.User;


import java.security.Principal;

import jakarta.validation.Valid;

@RestController
public class TicketController {

    private final TicketService ticketService;
    private final UserMapper userMapper;

    public TicketController(TicketService ticketService, UserMapper userMapper) {
        this.ticketService = ticketService;
        this.userMapper= userMapper;
    }

    @PostMapping("/tickets")
    public int add(@Valid @RequestBody Ticket ticket, Principal principal) {

        User currentUser = userMapper.findByUsername(principal.getName());

        if (currentUser == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Current user not found"
            );
        }

        ticket.setCreatedBy(currentUser.getId());

        return ticketService.add(ticket);
    }

    @GetMapping("/tickets")
    public PageResponse findAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Integer assignedTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String order
            ) {
        if (page < 1 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid page or size"
            );
        }
        
        if (((long) page - 1) * size > Integer.MAX_VALUE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page offset is too large"
            );
        }

        // Validate sorting field
        if (!sortBy.equals("id") && !sortBy.equals("createdAt")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid sortBy"
            );
        }

        // Validate sorting direction
        if (!order.equals("asc") && !order.equals("desc")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid order"
            );
        }

        return ticketService.findTickets(
                status,
                priority,
                assignedTo,
                page,
                size,
                sortBy,
                order
        );
    }

    @GetMapping("/tickets/{id}")
    public ResponseEntity<?> findById(@PathVariable int id){
        Ticket ticket= ticketService.findById(id);
        if (ticket==null){
            return ResponseEntity.status(404).body("Ticket not found");

        }
        return ResponseEntity.ok(ticket);
    }

   @PutMapping("/tickets/{id}")
    public ResponseEntity<String> update(
            @PathVariable int id,
            @Valid @RequestBody TicketUpdateRequest request) {

        Ticket ticket = new Ticket();

        ticket.setTitle(request.getTitle());
        ticket.setDescription(request.getDescription());
        ticket.setCategory(request.getCategory());
        ticket.setResolution(request.getResolution());

        ticketService.update(id,ticket);

        return ResponseEntity.ok("Ticket updated");
    }

    @PutMapping("/tickets/{id}/status")
    public ResponseEntity<String> updateStatus(
            @PathVariable int id,
            @RequestBody String newStatus) {

        ticketService.updateStatus(id, newStatus);
        
        return ResponseEntity.ok("Status updated");
    }

    @PutMapping("/tickets/{id}/assignee")
    public ResponseEntity<String> updateAssignee(
            @PathVariable int id,
            @RequestBody Integer assignedTo) {

        ticketService.updateAssignee(id, assignedTo);

        return ResponseEntity.ok("Assignee updated");
    }

    @PutMapping("/tickets/{id}/priority")
    public ResponseEntity<String> updatePriority(
            @PathVariable int id,
            @RequestBody String priority) {

        ticketService.updatePriority(id, priority);

        

        return ResponseEntity.ok("Priority updated");
    }

    @DeleteMapping("/tickets/{id}")
    public ResponseEntity<String> delete(@PathVariable int id){
        ticketService.delete(id);
        
        return ResponseEntity.ok("Ticket deleted");
    }
}