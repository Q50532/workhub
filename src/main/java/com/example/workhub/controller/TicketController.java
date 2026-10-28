package com.example.workhub.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.example.workhub.model.PageResponse;
import com.example.workhub.model.Ticket;
import com.example.workhub.service.TicketService;
import com.example.workhub.service.TicketAccessService;
import com.example.workhub.model.TicketUpdateRequest;
import com.example.workhub.mapper.UserMapper;
import com.example.workhub.idempotency.TicketCreationService;
import com.example.workhub.model.User;


import java.security.Principal;

import jakarta.validation.Valid;

@RestController
public class TicketController {

    private final TicketService ticketService;
    private final UserMapper userMapper;
    private final TicketAccessService access;
    private final TicketCreationService creator;

    public TicketController(TicketService ticketService, UserMapper userMapper, TicketAccessService access,
                            TicketCreationService creator) {
        this.ticketService = ticketService;
        this.userMapper= userMapper;
        this.access = access;
        this.creator = creator;
    }

    @PostMapping("/tickets")
    public int add(@Valid @RequestBody Ticket ticket,
                   @RequestHeader(name = "Idempotency-Key", required = false) String requestKey,
                   Principal principal) {

        User currentUser = userMapper.findByUsername(principal.getName());

        if (currentUser == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Current user not found"
            );
        }

        ticket.setCreatedBy(currentUser.getId());
        if (!access.isManager(currentUser)) {
            // Assignment is a manager-only operation, including on creation.
            ticket.setAssignedTo(null);
        }
        return creator.create(ticket, requestKey);
    }

    @GetMapping("/tickets")
    public PageResponse findAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Integer assignedTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String order,
            Principal principal
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

        User viewer = access.currentUser(principal);
        return ticketService.findTickets(
                status, priority, assignedTo, page, size, sortBy, order,
                access.isManager(viewer) ? null : viewer.getId()
        );
    }

    @GetMapping("/tickets/{id}")
    public ResponseEntity<?> findById(@PathVariable int id, Principal principal){
        access.visibleTicket(id, principal);
        Ticket ticket = ticketService.findById(id);
        if (ticket==null){
            return ResponseEntity.status(404).body("Ticket not found");

        }
        return ResponseEntity.ok(ticket);
    }

    /** HTTP writes must use the version the client previously read. */
    private int expectedVersion(String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED,
                    "If-Match version required");
        }
        String value = ifMatch.trim();
        if (value.length() > 1 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        try {
            int version = Integer.parseInt(value);
            if (version < 0) throw new NumberFormatException("negative");
            return version;
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid If-Match version");
        }
    }

    @PutMapping("/tickets/{id}")
    public ResponseEntity<String> update(
            @PathVariable int id,
            @Valid @RequestBody TicketUpdateRequest request,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            Principal principal) {
        access.requireCreatorOrManager(id, principal);

        Ticket ticket = new Ticket();

        ticket.setTitle(request.getTitle());
        ticket.setDescription(request.getDescription());
        ticket.setCategory(request.getCategory());
        ticket.setResolution(request.getResolution());

        ticketService.update(id, ticket, expectedVersion(ifMatch),
                access.currentUser(principal).getId());

        return ResponseEntity.ok("Ticket updated");
    }

    @PutMapping("/tickets/{id}/status")
    public ResponseEntity<String> updateStatus(
            @PathVariable int id,
            @RequestBody String newStatus,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            Principal principal) {
        access.requireAssigneeOrManager(id, principal);

        ticketService.updateStatus(id, newStatus, expectedVersion(ifMatch),
                access.currentUser(principal).getId());
        
        return ResponseEntity.ok("Status updated");
    }

    @PutMapping("/tickets/{id}/assignee")
    public ResponseEntity<String> updateAssignee(
            @PathVariable int id,
            @RequestBody Integer assignedTo,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            Principal principal) {
        access.requireManager(principal);

        ticketService.updateAssignee(id, assignedTo, expectedVersion(ifMatch),
                access.currentUser(principal).getId());

        return ResponseEntity.ok("Assignee updated");
    }

    @DeleteMapping("/tickets/{id}/assignee")
    public ResponseEntity<String> unassign(
            @PathVariable int id,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            Principal principal) {
        access.requireManager(principal);
        ticketService.updateAssignee(id, null, expectedVersion(ifMatch),
                access.currentUser(principal).getId());
        return ResponseEntity.ok("Ticket unassigned");
    }

    @PutMapping("/tickets/{id}/priority")
    public ResponseEntity<String> updatePriority(
            @PathVariable int id,
            @RequestBody String priority,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            Principal principal) {
        access.requireManager(principal);

        ticketService.updatePriority(id, priority, expectedVersion(ifMatch),
                access.currentUser(principal).getId());

        

        return ResponseEntity.ok("Priority updated");
    }

    @DeleteMapping("/tickets/{id}")
    public ResponseEntity<String> delete(@PathVariable int id,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            Principal principal){
        access.requireCreatorOrManager(id, principal);
        ticketService.delete(id, expectedVersion(ifMatch),
                access.currentUser(principal).getId());
        
        return ResponseEntity.ok("Ticket deleted");
    }
}