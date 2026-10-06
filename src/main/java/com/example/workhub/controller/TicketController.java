package com.example.workhub.controller;

import org.springframework.web.bind.annotation.*;

import org.springframework.http.ResponseEntity;

import com.example.workhub.model.Ticket;
import com.example.workhub.service.TicketService;

import java.util.List;

@RestController
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping("/tickets")
    public int add(@RequestBody Ticket ticket) {
        return ticketService.add(ticket);
    }
    @GetMapping("/tickets")
    public List<Ticket> findAll(@RequestParam(required=false) String status){
        
        if (status==null){
            return ticketService.findAll();
        }
        return ticketService.findByStatus(status);
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
    public ResponseEntity<String> update(@PathVariable  int id, @RequestBody Ticket ticket){
        int rows= ticketService.update(id, ticket);
        if (rows==0){
            return ResponseEntity.status(404).body("Ticket not found");
        }
        return ResponseEntity.ok("Ticket updated");
    }

    @PutMapping("/tickets/{id}/status")
    public ResponseEntity<String> updateStatus(
            @PathVariable int id,
            @RequestBody String newStatus) {

        int result= ticketService.updateStatus(id, newStatus);
        if (result==0){
            return ResponseEntity.status(404).body("Ticket not found");
        }
        if (result==-1){
            return ResponseEntity.status(400).body("Invalid status transition");
        }
        return ResponseEntity.ok("Status updated");
    }

    @PutMapping("/tickets/{id}/assignee")
    public ResponseEntity<String> updateAssignee(
            @PathVariable int id,
            @RequestBody Integer assignedTo) {

        int result = ticketService.updateAssignee(id, assignedTo);

        if (result == 0) {
            return ResponseEntity.status(404).body("Ticket not found");
        }

        if (result == -1) {
            return ResponseEntity.status(400).body("User not found");
        }

        return ResponseEntity.ok("Assignee updated");
    }

    @PutMapping("/tickets/{id}/priority")
    public ResponseEntity<String> updatePriority(
            @PathVariable int id,
            @RequestBody String priority) {

        int result = ticketService.updatePriority(id, priority);

        if (result == 0) {
            return ResponseEntity.status(404).body("Ticket not found");
        }

        if (result == -1) {
            return ResponseEntity.status(400).body("Invalid priority");
        }

        return ResponseEntity.ok("Priority updated");
    }

    @DeleteMapping("/tickets/{id}")
    public ResponseEntity<String> delete(@PathVariable int id){
        int rows= ticketService.delete(id);
        if (rows==0){
            return ResponseEntity.status(404).body("Ticket not found");
        }
        return ResponseEntity.ok("Ticket deleted");
    }
}