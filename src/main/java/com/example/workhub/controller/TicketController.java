package com.example.workhub.controller;

import org.springframework.web.bind.annotation.*;

import org.springframework.http.ResponseEntity;

import com.example.workhub.model.Ticket;
import com.example.workhub.service.TicketService;
import com.example.workhub.model.TicketUpdateRequest;

import java.util.List;

import jakarta.validation.Valid;

@RestController
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping("/tickets")
    public int add(@Valid @RequestBody Ticket ticket) {
        return ticketService.add(ticket);
    }

    @GetMapping("/tickets")
    public List<Ticket> findAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Integer assignedTo) {

        return ticketService.findByFilters(
                status,
                priority,
                assignedTo
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